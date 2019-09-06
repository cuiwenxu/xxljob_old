package com.xxl.job.admin.core.jobbean;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;

/**
 * http job bean<br/>
 * 触发器执行调用的类,然后由这个类中方法中将触发任务加入到触发器的线程池中<br/>
 * 
 * @author xuxueli 2015-12-17 18:20:34
 */
// diable concurrent, thread size can not be only one, better given more
// 不允许并发，线程大小不能只有一个，最好给出更多
// @DisallowConcurrentExecution
public class RemoteHttpJobBean extends QuartzJobBean {
	private static Logger logger = LoggerFactory.getLogger(RemoteHttpJobBean.class);

	/**
	 * 触发器任务执行的时候，调用这个方法
	 */
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

		logger.info(">>>>>>>>> RemoteHttpJobBean >>>> put in JobTriggerPool...start......");

		// load jobId
		JobKey jobKey = context.getTrigger().getJobKey();
		Integer jobId = Integer.valueOf(jobKey.getName());

		// trigger 加入到触发器的线程池中
		JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null);

		logger.info(">>>>>>>>> RemoteHttpJobBean >>>> put in JobTriggerPool...end......");

	}

}