package com.xxl.job.admin.core.schedule;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.jobbean.RemoteHttpJobBean;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.thread.JobFailMonitorHelper;
import com.xxl.job.admin.core.thread.JobRegistryMonitorHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.call.CallType;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.net.NetEnum;
import com.xxl.rpc.remoting.net.impl.jetty.server.JettyServerHandler;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.serialize.Serializer;

/**
 * base quartz scheduler util <br/>
 * 定时任务工具类，在启动中做很多事
 * 
 * @author xuxueli 2015-12-19 16:13:53
 */
public final class XxlJobDynamicScheduler {

	private static final Logger logger = LoggerFactory.getLogger(XxlJobDynamicScheduler.class);

	// ---------------------- param ----------------------

	// scheduler
	private static Scheduler scheduler;

	public void setScheduler(Scheduler scheduler) {
		XxlJobDynamicScheduler.scheduler = scheduler;
	}

	// ---------------------- init + destroy ----TODO------------------
	/**
	 * 启动
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		// valid
		Assert.notNull(scheduler, "quartz scheduler is null");

		// init i18n
		initI18n();

		// admin registry monitor run
		// 启动自动注册线程， 获取类型为自动注册的执行器信息，完成机器的自动注册与发现
		JobRegistryMonitorHelper.getInstance().start();

		// admin monitor run
		// 启动失败日志监控线程
		JobFailMonitorHelper.getInstance().start();

		// admin-server
		// admin的服务启动:让执行器可以调用调度中心的接口
		initRpcProvider();

		logger.info(">>>>>>>>> init xxl-job admin success.");
	}

	/**
	 * 停止
	 * 
	 * @throws Exception
	 */
	public void destroy() throws Exception {
		// admin trigger pool stop
		JobTriggerPoolHelper.toStop();

		// admin registry stop
		JobRegistryMonitorHelper.getInstance().toStop();

		// admin monitor stop
		JobFailMonitorHelper.getInstance().toStop();

		// admin-server
		stopRpcProvider();
	}

	// ---------------------- I18n ---TODO-------------------

	private void initI18n() {
		for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
			item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
		}
	}

	// ------ admin rpc provider (no server version) ----调度中心服务(RPC的服务提供者,被执行器调用)---TODO------------------
	private static JettyServerHandler jettyServerHandler;

	/**
	 * ----- TODO ----(与调度中心不同,直接设置jettyServerHandler,不像执行器那样单独启动的jetty server,共用一个web端口server.port)
	 */
	private void initRpcProvider() {
		// init
		XxlRpcProviderFactory xxlRpcProviderFactory = new XxlRpcProviderFactory();
		xxlRpcProviderFactory.initConfig(NetEnum.JETTY, Serializer.SerializeEnum.HESSIAN.getSerializer(), null, 0,
				XxlJobAdminConfig.getAdminConfig().getAccessToken(), null, null);

		// add services:增加调度中心服务到PRC
		xxlRpcProviderFactory.addService(AdminBiz.class.getName(), null,
				XxlJobAdminConfig.getAdminConfig().getAdminBiz());

		// jetty handler:直接设置jettyServerHandler,不像执行器那样单独启动的jetty server
		jettyServerHandler = new JettyServerHandler(xxlRpcProviderFactory);
	}

	/**
	 * 停止
	 * 
	 * @throws Exception
	 */
	private void stopRpcProvider() throws Exception {
		new XxlRpcInvokerFactory().stop();
	}

	/**
	 * RPC远程执行的请求时候要调用的
	 * 
	 * @param request
	 *            内涵了request请求参数信息的byte数组
	 * @param response
	 *            内涵了response结果参数信息的byte数组
	 * @throws IOException
	 * @throws ServletException
	 */
	public static void invokeAdminService(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		jettyServerHandler.handle(null, new Request(null, null), request, response);
	}

	// -------------- executor-client -----执行器client(用这个去调用执行器的接口)---TODO-------------
	// 执行器client的map
	private static ConcurrentHashMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();

	/**
	 * 获取执行器client
	 * 
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public static ExecutorBiz getExecutorBiz(String address) throws Exception {
		// valid
		if (address == null || address.trim().length() == 0) {
			return null;
		}

		// 执行器client的map 中获取
		address = address.trim();
		// 查看缓存里面是否存在，如果存在则不需要再去创建executorBiz了
		ExecutorBiz executorBiz = executorBizRepository.get(address);
		if (executorBiz != null) {
			return executorBiz;
		}

		// 创建ExecutorBiz的代理对象
		executorBiz = (ExecutorBiz) new XxlRpcReferenceBean(NetEnum.JETTY,
				Serializer.SerializeEnum.HESSIAN.getSerializer(), CallType.SYNC, ExecutorBiz.class, null, 10000,
				address, XxlJobAdminConfig.getAdminConfig().getAccessToken(), null).getObject();

		// 放入执行器client的map
		executorBizRepository.put(address, executorBiz);
		return executorBiz;
	}

	// ---------------------- schedule util ---TODO 定时任务操作类-------------------

	/**
	 * fill job info 填充任务信息:cron表达式和JobStatus
	 * 
	 * @param jobInfo
	 */
	public static void fillJobInfo(XxlJobInfo jobInfo) {

		// 执行器id
		String group = String.valueOf(jobInfo.getJobGroup());
		// 任务id
		String name = String.valueOf(jobInfo.getId());

		// trigger key
		TriggerKey triggerKey = TriggerKey.triggerKey(name, group);
		try {

			// trigger cron
			Trigger trigger = scheduler.getTrigger(triggerKey);
			if (trigger != null && trigger instanceof CronTriggerImpl) {
				// 获取cron表达式
				String cronExpression = ((CronTriggerImpl) trigger).getCronExpression();
				jobInfo.setJobCron(cronExpression);
			}

			// trigger state
			TriggerState triggerState = scheduler.getTriggerState(triggerKey);
			if (triggerState != null) {
				// 任务状态
				jobInfo.setJobStatus(triggerState.name());
			}

			// JobKey jobKey = new JobKey(jobInfo.getJobName(), String.valueOf(jobInfo.getJobGroup()));
			// JobDetail jobDetail = scheduler.getJobDetail(jobKey);
			// String jobClass = jobDetail.getJobClass().getName();

		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * add trigger + job 增加并开始执行任务
	 * 
	 * @param jobName
	 *            任务id
	 * @param jobGroup
	 *            执行器id
	 * @param cronExpression
	 *            cron表达式
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean addJob(String jobName, String jobGroup, String cronExpression) throws SchedulerException {
		// 1、job key
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
		JobKey jobKey = new JobKey(jobName, jobGroup);

		// 2、valid 验证是否存在这个触发器任务
		if (scheduler.checkExists(triggerKey)) {
			return true; // PASS
		}

		// 3、corn trigger
		CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
				.withMisfireHandlingInstructionDoNothing(); // withMisfireHandlingInstructionDoNothing 忽略掉调度终止过程中忽略的调度
		CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder)
				.build();

		// 4、job detail RemoteHttpJobBean的方式执行
		Class<? extends Job> jobClass_ = RemoteHttpJobBean.class; // Class.forName(jobInfo.getJobClass());
		JobDetail jobDetail = JobBuilder.newJob(jobClass_).withIdentity(jobKey).build();

		/*
		 * if (jobInfo.getJobData()!=null) { JobDataMap jobDataMap = jobDetail.getJobDataMap();
		 * jobDataMap.putAll(JacksonUtil.readValue(jobInfo.getJobData(), Map.class)); // JobExecutionContext
		 * context.getMergedJobDataMap().get("mailGuid"); }
		 */

		// 5、schedule job 放入到定时任务中
		Date date = scheduler.scheduleJob(jobDetail, cronTrigger);

		logger.info(">>>>>>>>>>> addJob success, jobDetail:{}, cronTrigger:{}, date:{}", jobDetail, cronTrigger, date);
		return true;
	}

	/**
	 * remove trigger + job<br/>
	 * 删除触发器和任务
	 * 
	 * @param jobName
	 *            名字(任务id)
	 * @param jobGroup
	 *            分组(执行器id)
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean removeJob(String jobName, String jobGroup) throws SchedulerException {

		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);

		if (scheduler.checkExists(triggerKey)) {
			// 关闭触发器
			scheduler.unscheduleJob(triggerKey); // trigger + job
		}

		logger.info(">>>>>>>>>>> removeJob success, triggerKey:{}", triggerKey);
		return true;
	}

	/**
	 * updateJobCron 修改任务的cron表达式
	 * 
	 * @param jobGroup
	 * @param jobName
	 * @param cronExpression
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean updateJobCron(String jobGroup, String jobName, String cronExpression)
			throws SchedulerException {

		// 1、job key
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);

		// 2、valid
		if (!scheduler.checkExists(triggerKey)) {
			return true; // PASS
		}

		CronTrigger oldTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);

		// 3、avoid repeat cron
		String oldCron = oldTrigger.getCronExpression();
		// 新的cron表达式和老的cron表达式一样就直接退出
		if (oldCron.equals(cronExpression)) {
			return true; // PASS
		}

		// 4、new cron trigger
		CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
				// 所有的misfire不管，执行下一个周期的任务
				.withMisfireHandlingInstructionDoNothing();
		//
		Trigger newTrigger = oldTrigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder)
				.build();

		// 5、rescheduleJob
		// 替换触发器，通过triggerkey移除旧的触发器，同时添加一个新的进去。
		// 这个方法返回一个Date.如果返回 null 说明替换失败，原因就是旧触发器没有找到，所以新的触发器也不会设置进去
		scheduler.rescheduleJob(triggerKey, newTrigger);

		logger.info(">>>>>>>>>>> resumeJob success, JobGroup:{}, JobName:{}", jobGroup, jobName);
		return true;
	}

	/**
	 * pause
	 *
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	/*
	 * public static boolean pauseJob(String jobName, String jobGroup) throws SchedulerException {
	 * 
	 * TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
	 * 
	 * boolean result = false; if (scheduler.checkExists(triggerKey)) { scheduler.pauseTrigger(triggerKey); result =
	 * true; }
	 * 
	 * logger.info(">>>>>>>>>>> pauseJob {}, triggerKey:{}", (result?"success":"fail"),triggerKey); return result; }
	 */

	/**
	 * resume
	 *
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	/*
	 * public static boolean resumeJob(String jobName, String jobGroup) throws SchedulerException {
	 * 
	 * TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
	 * 
	 * boolean result = false; if (scheduler.checkExists(triggerKey)) { scheduler.resumeTrigger(triggerKey); result =
	 * true; }
	 * 
	 * logger.info(">>>>>>>>>>> resumeJob {}, triggerKey:{}", (result?"success":"fail"), triggerKey); return result; }
	 */

	/**
	 * run
	 *
	 * @param jobName
	 * @param jobGroup
	 * @return
	 * @throws SchedulerException
	 */
	/*
	 * public static boolean triggerJob(String jobName, String jobGroup) throws SchedulerException { // TriggerKey :
	 * name + group JobKey jobKey = new JobKey(jobName, jobGroup); TriggerKey triggerKey =
	 * TriggerKey.triggerKey(jobName, jobGroup);
	 * 
	 * boolean result = false; if (scheduler.checkExists(triggerKey)) { scheduler.triggerJob(jobKey); result = true;
	 * logger.info(">>>>>>>>>>> runJob success, jobKey:{}", jobKey); } else {
	 * logger.info(">>>>>>>>>>> runJob fail, jobKey:{}", jobKey); } return result; }
	 */

	/**
	 * finaAllJobList
	 *
	 * @return
	 *//*
		 * @Deprecated public static List<Map<String, Object>> finaAllJobList(){ List<Map<String, Object>> jobList = new
		 * ArrayList<Map<String,Object>>();
		 * 
		 * try { if (scheduler.getJobGroupNames()==null || scheduler.getJobGroupNames().size()==0) { return null; }
		 * String groupName = scheduler.getJobGroupNames().get(0); Set<JobKey> jobKeys =
		 * scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)); if (jobKeys!=null && jobKeys.size()>0) { for
		 * (JobKey jobKey : jobKeys) { TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(),
		 * Scheduler.DEFAULT_GROUP); Trigger trigger = scheduler.getTrigger(triggerKey); JobDetail jobDetail =
		 * scheduler.getJobDetail(jobKey); TriggerState triggerState = scheduler.getTriggerState(triggerKey);
		 * Map<String, Object> jobMap = new HashMap<String, Object>(); jobMap.put("TriggerKey", triggerKey);
		 * jobMap.put("Trigger", trigger); jobMap.put("JobDetail", jobDetail); jobMap.put("TriggerState", triggerState);
		 * jobList.add(jobMap); } }
		 * 
		 * } catch (SchedulerException e) { logger.error(e.getMessage(), e); return null; } return jobList; }
		 */

}