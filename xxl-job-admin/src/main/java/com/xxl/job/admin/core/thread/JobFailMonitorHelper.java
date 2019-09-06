package com.xxl.job.admin.core.thread;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.core.util.MailUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;

/**
 * job monitor instance 失败日志监控线程
 * 
 * @author xuxueli 2015-9-1 18:05:56
 */
public class JobFailMonitorHelper {

	private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);

	private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

	public static JobFailMonitorHelper getInstance() {
		return instance;
	}

	// ---------------------- monitor ----------------------
	// job的日志id队列(只会放进来,在线程中拿出去做判断)
	private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>(0xfff8);
	// 日志监控线程
	private Thread monitorThread;

	//
	private volatile boolean toStop = false;

	// producer=触发器触发的时候将日志id放进来,或者还没结束的任务就继续监听
	public static void monitor(int jobLogId) {
		// 将指定元素插入此队列中。
		getInstance().queue.offer(jobLogId);
	}

	/**
	 * 日志id的监控进程
	 */
	public void start() {
		monitorThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// monitor
				while (!toStop) {
					try {
						// 从队列中拿出所有可用的 jobLogIds
						List<Integer> jobLogIdList = new ArrayList<Integer>();
						// int drainToNum =
						// 移除此队列中所有可用的元素，并将它们添加到给定 jobLogIdList 中。
						JobFailMonitorHelper.instance.queue.drainTo(jobLogIdList);

						if (CollectionUtils.isNotEmpty(jobLogIdList)) {
							for (Integer jobLogId : jobLogIdList) {
								if (jobLogId == null || jobLogId == 0) {
									continue;
								}

								// 从数据库跟以前有日志信息
								XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(jobLogId);

								if (log == null) {
									continue;
								}

								// 任务触发成功， 但是JobHandle 还没有返回结果
								if (IJobHandler.SUCCESS.getCode() == log.getTriggerCode() && log.getHandleCode() == 0) {
									// 将 JobLogId 放入队列 ， 继续监控
									JobFailMonitorHelper.monitor(jobLogId);
									logger.debug(">>>>>>>>>>> job monitor, job running, JobLogId:{}", jobLogId);
								} else if (IJobHandler.SUCCESS.getCode() == log.getHandleCode()) {// 成功的
									// job success, pass
									logger.info(">>>>>>>>>>> job monitor, job success, JobLogId:{}", jobLogId);
								} else {

									// 失败重试次数大于零
									if (log.getExecutorFailRetryCount() > 0) {
										// 放到job触发器线程池再试试(重试)
										JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY,
												(log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(),
												null);
										String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>"
												+ I18nUtil.getString("jobconf_trigger_type_retry")
												+ "<<<<<<<<<<< </span><br>";
										log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
										// 修改日志触发信息
										XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(log);
									}

									// 1、fail retry
									XxlJobInfo info = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao()
											.loadById(log.getJobId());
									// 任务执行失败， 执行发送邮件等预警措施
									failAlarm(info, log);

									logger.info(">>>>>>>>>>> job monitor, job fail, JobLogId:{}", jobLogId);
								}
							}
						}

						TimeUnit.SECONDS.sleep(10);
					} catch (Exception e) {
						logger.error("job monitor error:{}", e);
					}
				}

				/**
				 * 停止前要做的事
				 */
				// monitor all clear
				List<Integer> jobLogIdList = new ArrayList<Integer>();
				// int drainToNum =
				getInstance().queue.drainTo(jobLogIdList);

				if (jobLogIdList != null && jobLogIdList.size() > 0) {
					for (Integer jobLogId : jobLogIdList) {
						// 主键查询日志
						XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(jobLogId);
						// 两个只要有一个失败
						if (ReturnT.FAIL_CODE == log.getTriggerCode() || ReturnT.FAIL_CODE == log.getHandleCode()) {
							// 主键查询调度任务信息
							XxlJobInfo info = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao()
									.loadById(log.getJobId());
							// fail alarm 任务执行失败， 执行发送邮件等预警措施
							failAlarm(info, log);
							logger.info(">>>>>>>>>>> job monitor last, job fail, JobLogId:{}", jobLogId);
						}
					}
				}

			}
		});
		monitorThread.setDaemon(true);

		// 日志监控
		monitorThread.start();
	}

	/**
	 * 停止
	 */
	public void toStop() {
		toStop = true;
		// interrupt and wait
		monitorThread.interrupt();
		try {
			monitorThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	// ---------------------- alarm ----- TODO 失败的信息-----------------

	// email alarm template
	private static final String mailBodyTemplate = "<h5>" + I18nUtil.getString("jobconf_monitor_detail") + "：</span>"
			+ "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n"
			+ "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" + "      <tr>\n"
			+ "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobgroup") + "</td>\n"
			+ "         <td width=\"10%\" >" + I18nUtil.getString("jobinfo_field_id") + "</td>\n"
			+ "         <td width=\"20%\" >" + I18nUtil.getString("jobinfo_field_jobdesc") + "</td>\n"
			+ "         <td width=\"10%\" >" + I18nUtil.getString("jobconf_monitor_alarm_title") + "</td>\n"
			+ "         <td width=\"40%\" >" + I18nUtil.getString("jobconf_monitor_alarm_content") + "</td>\n"
			+ "      </tr>\n" + "   </thead>\n" + "   <tbody>\n" + "      <tr>\n" + "         <td>{0}</td>\n"
			+ "         <td>{1}</td>\n" + "         <td>{2}</td>\n" + "         <td>"
			+ I18nUtil.getString("jobconf_monitor_alarm_type") + "</td>\n" + "         <td>{3}</td>\n" + "      </tr>\n"
			+ "   </tbody>\n" + "</table>";

	/**
	 * fail alarm 任务执行失败， 执行发送邮件等预警措施
	 * 
	 * @param jobLog
	 */
	private void failAlarm(XxlJobInfo info, XxlJobLog jobLog) {

		// send monitor email
		if (info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0) {

			String alarmContent = "Alarm Job LogId=" + jobLog.getId();
			if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE) {
				alarmContent += "<br>TriggerMsg=" + jobLog.getTriggerMsg();
			}
			if (jobLog.getHandleCode() > 0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
				alarmContent += "<br>HandleCode=" + jobLog.getHandleMsg();
			}

			Set<String> emailSet = new HashSet<String>(Arrays.asList(info.getAlarmEmail().split(",")));
			for (String email : emailSet) {

				// 主键查找执行器信息
				XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao()
						.load(Integer.valueOf(info.getJobGroup()));
				// 标题
				String title = I18nUtil.getString("jobconf_monitor");
				// 内容
				String content = MessageFormat.format(mailBodyTemplate, group != null ? group.getTitle() : "null",
						info.getId(), info.getJobDesc(), alarmContent);

				// 发送邮件
				MailUtil.sendMail(email, title, content);
			}
		}

	}

}
