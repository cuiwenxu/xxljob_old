package com.xxl.job.admin.core.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.trigger.XxlJobTrigger;

/**
 * job trigger thread pool helper <br/>
 * job触发器线程池
 * 
 * @author xuxueli 2018-07-03 21:08:07
 */
public class JobTriggerPoolHelper {
	private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

	// ---------------------- trigger pool ----------------------

	//
	private ThreadPoolExecutor triggerPool = new ThreadPoolExecutor(32, 256, 60L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(1000));

	/**
	 * 将任务提交给一个线程池(任务调度中心默认开启50个线程),在线程池中调用XxlJobTrigger.trigger
	 * 
	 * @param jobId
	 *            调度任务信息XxlJobInfo表主键
	 * @param triggerType
	 *            触发类型
	 * @param failRetryCount
	 *            >=0: use this param <0: use param from job info config<br/>
	 *            >=0: 用这个值 <0: 用XxlJobInfo中配置好的重试次数
	 * @param executorShardingParam
	 * 			  执行分片的参数
	 * @param executorParam
	 *            执行参数:<br/>
	 *            null: use job param ;not null: cover job param
	 *            null: 用XxlJobInfo中配置好的参数 ;not null: 使用这个参数(覆盖XxlJobInfo中配置好的参数)
	 */
	public void addTrigger(final int jobId, final TriggerTypeEnum triggerType, final int failRetryCount,
			final String executorShardingParam, final String executorParam) {
		triggerPool.execute(new Runnable() {
			@Override
			public void run() {
				XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam);
			}
		});
	}

	/**
	 * 线程池停止
	 */
	public void stop() {
		// triggerPool.shutdown();
		triggerPool.shutdownNow();
		logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
	}

	// ---------- helper ----下面的就是实例化一个helper对象来进行执行任务---- TODO ------------

	private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

	/**
	 * 调用上面的执行任务方法
	 * 
	 * @param jobId
	 *            调度任务信息XxlJobInfo表主键
	 * @param triggerType
	 *            触发类型
	 * @param failRetryCount
	 *            >=0: use this param <0: use param from job info config<br/>
	 *            >=0: 用这个值 <0: 用XxlJobInfo中配置好的重试次数
	 * @param executorShardingParam
	 * 			  执行分片的参数
	 * @param executorParam
	 *            执行参数:<br/>
	 *            null: use job param ;not null: cover job param
	 *            null: 用XxlJobInfo中配置好的参数 ;not null: 使用这个参数(覆盖XxlJobInfo中配置好的参数)
	 */
	public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam,
			String executorParam) {
		helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam);
	}

	/**
	 * 调用上面的停止方法
	 */
	public static void toStop() {
		helper.stop();
	}

}
