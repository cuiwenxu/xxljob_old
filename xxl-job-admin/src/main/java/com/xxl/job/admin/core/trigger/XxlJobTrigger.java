package com.xxl.job.admin.core.trigger;

import java.util.Date;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.thread.JobFailMonitorHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.rpc.util.IpUtil;
import com.xxl.rpc.util.ThrowableUtil;

/**
 * xxl-job trigger 任务触发执行类<br/>
 * Created by xuxueli on 17/7/13.
 */
public class XxlJobTrigger {

	private static Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);

	/**
	 * 触发任务
	 * 
	 * @param jobId
	 *            调度任务信息XxlJobInfo表主键
	 * @param triggerType
	 *            触发类型
	 * @param failRetryCount
	 *            >=0: use this param <0: use param from job info config<br/>
	 *            >=0: 用这个值 <0: 用XxlJobInfo中配置好的重试次数
	 * @param executorShardingParam
	 *            执行分片的参数
	 * @param executorParam
	 *            执行参数:<br/>
	 *            null: use job param ;not null: cover job param <br/>
	 *            null: 用XxlJobInfo中配置好的参数 ;not null: 使用这个参数(覆盖XxlJobInfo中配置好的参数)
	 */
	public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam,
			String executorParam) {
		// 查询job信息
		XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
		if (jobInfo == null) {
			logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
			return;
		}
		if (executorParam != null) {// 执行参数不为空，则覆盖设置执行参数
			jobInfo.setExecutorParam(executorParam);
		}

		// 设置失败重试次数失败重试次数
		int finalFailRetryCount = failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();

		// 获取执行器信息
		XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(jobInfo.getJobGroup());

		// sharding param
		int[] shardingParam = null;
		if (executorShardingParam != null) {
			String[] shardingArr = executorShardingParam.split("/");
			if (shardingArr.length == 2 && StringUtils.isNumeric(shardingArr[0])
					&& StringUtils.isNumeric(shardingArr[1])) {
				shardingParam = new int[2];
				shardingParam[0] = Integer.valueOf(shardingArr[0]);
				shardingParam[1] = Integer.valueOf(shardingArr[1]);
			}
		}

		// 路由:分片广播
		if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum
				.match(jobInfo.getExecutorRouteStrategy(), null) && CollectionUtils.isNotEmpty(group.getRegistryList())
				&& shardingParam == null) {
			for (int i = 0; i < group.getRegistryList().size(); i++) {
				processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryList().size());
			}
		} else {// 路由:非分片广播
			if (shardingParam == null) {
				shardingParam = new int[] { 0, 1 };
			}
			// 执行任务
			processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0], shardingParam[1]);
		}

	}

	/**
	 * 执行任务
	 * 
	 * @param group
	 *            执行器信息
	 * @param jobInfo
	 *            任务信息
	 * @param finalFailRetryCount
	 *            失败重试次数失败重试次数
	 * @param triggerType
	 *            触发类型
	 * @param index
	 *            sharding index
	 * @param total
	 *            sharding index
	 */
	private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount,
			TriggerTypeEnum triggerType, int index, int total) {

		// 阻塞处理策略
		ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(),
				ExecutorBlockStrategyEnum.SERIAL_EXECUTION); // block strategy
		// 路由策略
		ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum
				.match(jobInfo.getExecutorRouteStrategy(), null); // route strategy

		// 路由策略是否是分片广播
		String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum)
				? String.valueOf(index).concat("/").concat(String.valueOf(total)) : null;

		// ----------------执行前进行的操作-----------------------TODO--------

		// 1、save log-id
		XxlJobLog jobLog = new XxlJobLog();
		jobLog.setJobGroup(jobInfo.getJobGroup());
		jobLog.setJobId(jobInfo.getId());
		jobLog.setTriggerTime(new Date());
		// 保存日志
		XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().save(jobLog);
		logger.debug(">>>>>>>>>>> xxl-job trigger start, jobId:{}", jobLog.getId());

		// 2、init trigger-param===初始化设置触发器参数
		TriggerParam triggerParam = new TriggerParam();
		triggerParam.setJobId(jobInfo.getId());
		triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
		triggerParam.setExecutorParams(jobInfo.getExecutorParam());
		triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
		triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
		triggerParam.setLogId(jobLog.getId());
		triggerParam.setLogDateTim(jobLog.getTriggerTime().getTime());
		triggerParam.setGlueType(jobInfo.getGlueType());
		triggerParam.setGlueSource(jobInfo.getGlueSource());
		triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
		triggerParam.setBroadcastIndex(index);
		triggerParam.setBroadcastTotal(total);

		// 3、init address(根据路由策略执行器地址)
		String address = null;
		ReturnT<String> routeAddressResult = null;

		// 执行器地址列表不为空
		if (CollectionUtils.isNotEmpty(group.getRegistryList())) {

			// 分片广播
			if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
				if (index < group.getRegistryList().size()) {
					address = group.getRegistryList().get(index);
				} else {
					address = group.getRegistryList().get(0);
				}
			} else {// 其他方式
				// 此处使用了策略模式， 根据不同的策略 使用不同的实现类 TODO
				routeAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryList());

				if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
					address = routeAddressResult.getContent();
					logger.debug(">>>>>>>>>>> xxl-job trigger excute address, address:{}", address);

				}
			}
		} else {

			routeAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE,
					I18nUtil.getString("jobconf_trigger_address_empty"));
		}

		// 4、trigger remote executor 调用执行器远程执行
		ReturnT<String> triggerResult = null;
		if (address != null) {
			// ---- 调用执行器远程执行 ----- TODO --
			triggerResult = runExecutor(triggerParam, address);
		} else {
			triggerResult = new ReturnT<String>(ReturnT.FAIL_CODE, null);
		}

		// 5、collection trigger info 触发执行后的消息
		StringBuffer triggerMsgSb = new StringBuffer();
		triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.getTitle());
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：")
				.append(IpUtil.getIp());
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
				.append((group.getAddressType() == 0) ? I18nUtil.getString("jobgroup_field_addressType_0")
						: I18nUtil.getString("jobgroup_field_addressType_1"));
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：")
				.append(group.getRegistryList());
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：")
				.append(executorRouteStrategyEnum.getTitle());
		if (shardingParam != null) {
			triggerMsgSb.append("(" + shardingParam + ")");
		}
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：")
				.append(blockStrategy.getTitle());
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：")
				.append(jobInfo.getExecutorTimeout());
		triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：")
				.append(finalFailRetryCount);

		triggerMsgSb
				.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"
						+ I18nUtil.getString("jobconf_trigger_run") + "<<<<<<<<<<< </span><br>")
				.append((routeAddressResult != null && routeAddressResult.getMsg() != null)
						? routeAddressResult.getMsg() + "<br><br>" : "")
				.append(triggerResult.getMsg() != null ? triggerResult.getMsg() : "");

		// 6、save log trigger-info 修改触发器日志信息
		jobLog.setExecutorAddress(address);
		jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
		jobLog.setExecutorParam(jobInfo.getExecutorParam());
		jobLog.setExecutorShardingParam(shardingParam);
		jobLog.setExecutorFailRetryCount(finalFailRetryCount);
		// jobLog.setTriggerTime();
		jobLog.setTriggerCode(triggerResult.getCode());
		jobLog.setTriggerMsg(triggerMsgSb.toString());// 修改日志触发信息
		// 修改日志触发信息
		XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(jobLog);

		// 7、monitor trigger
		// 最后：这个触发器任务正在执行,放到队列里面
		// 要么执行器回调后被操作，要么超时被失败日志监控线程操作
		JobFailMonitorHelper.monitor(jobLog.getId());

		logger.debug(">>>>>>>>>>> xxl-job trigger end, jobId:{}", jobLog.getId());
	}

	/**
	 * 调用执行器执行该触发器任务 run executor
	 * 
	 * @param triggerParam
	 *            触发器参数(调用执行器进行执行)
	 * @param address
	 *            执行器地址
	 * @return
	 */
	public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
		ReturnT<String> runResult = null;
		try {

			// 创建一个ExcutorBiz 的对象，重点在这个方法里面
			ExecutorBiz executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
			// 这个run 方法不会最终执行，仅仅只是为了触发 proxy object 的 invoke方法，
			// 同时将目标的类型传送给执行器端， 因为在代理对象的invoke的方法里面没有执行目标对象的方法
			runResult = executorBiz.run(triggerParam);

		} catch (Exception e) {
			logger.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.", address, e);
			runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
		}

		StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
		runResultSB.append("<br>address：").append(address);
		runResultSB.append("<br>code：").append(runResult.getCode());
		runResultSB.append("<br>msg：").append(runResult.getMsg());

		runResult.setMsg(runResultSB.toString());
		return runResult;
	}

}
