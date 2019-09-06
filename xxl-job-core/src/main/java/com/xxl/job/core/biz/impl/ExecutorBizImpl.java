package com.xxl.job.core.biz.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.impl.GlueJobHandler;
import com.xxl.job.core.handler.impl.ScriptJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.JobThread;

/**
 * 执行器被调度实现方法 Created by xuxueli on 17/3/1.
 */
public class ExecutorBizImpl implements ExecutorBiz {
	private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.ExecutorBiz#beat()
	 */
	@Override
	public ReturnT<String> beat() {
		return ReturnT.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.ExecutorBiz#idleBeat(int)
	 */
	@Override
	public ReturnT<String> idleBeat(int jobId) {

		// isRunningOrHasQueue
		boolean isRunningOrHasQueue = false;
		// 从线程池里面获取当前任务对应的线程
		JobThread jobThread = XxlJobExecutor.loadJobThread(jobId);
		if (jobThread != null && jobThread.isRunningOrHasQueue()) {
			// 线程处于运行中
			isRunningOrHasQueue = true;
		}

		if (isRunningOrHasQueue) {
			// 线程运行中，则返回fasle
			return new ReturnT<String>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
		}
		// 线程空闲，返回success
		return ReturnT.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.ExecutorBiz#kill(int)
	 */
	@Override
	public ReturnT<String> kill(int jobId) {
		// kill handlerThread, and create new one
		JobThread jobThread = XxlJobExecutor.loadJobThread(jobId);
		if (jobThread != null) {
			XxlJobExecutor.removeJobThread(jobId, "scheduling center kill job.");
			return ReturnT.SUCCESS;
		}

		return new ReturnT<String>(ReturnT.SUCCESS_CODE, "job thread aleady killed.");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.ExecutorBiz#log(long, int, int)
	 */
	@Override
	public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum) {
		// log filename: logPath/yyyy-MM-dd/9999.log
		String logFileName = XxlJobFileAppender.makeLogFileName(new Date(logDateTim), logId);

		LogResult logResult = XxlJobFileAppender.readLog(logFileName, fromLineNum);
		return new ReturnT<LogResult>(logResult);
	}

	/**
	 * 运行某一个触发器: 1.不动/创建/替换一个任务执行线程
	 * 
	 * @see com.xxl.job.core.biz.ExecutorBiz#run(com.xxl.job.core.biz.model.TriggerParam)
	 */
	@Override
	public ReturnT<String> run(TriggerParam triggerParam) {
		// load old：jobHandler + jobThread
		// 获取是否有这个JobId的线程
		JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());
		// handler
		IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;
		// 删除旧的线程的原因
		String removeOldReason = null;

		/*
		 * valid：jobHandler + jobThread 验证jobHandler 和 jobThread
		 */
		GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
		if (GlueTypeEnum.BEAN == glueTypeEnum) {// bean模式

			// 查询jobHandlerRepository里面是否有该名字的JobHandler
			IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

			// valid old jobThread
			if (jobThread != null && jobHandler != newJobHandler) {// 两个不一样
				// change handler, need kill old thread
				removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";

				jobThread = null;
				jobHandler = null;
			}

			// valid handler
			if (jobHandler == null) {
				jobHandler = newJobHandler;
				if (jobHandler == null) {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							"job handler [" + triggerParam.getExecutorHandler() + "] not found.");
				}
			}

		} else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {

			// valid old jobThread
			if (jobThread != null
					&& !(jobThread.getHandler() instanceof GlueJobHandler && ((GlueJobHandler) jobThread.getHandler())
							.getGlueUpdatetime() == triggerParam.getGlueUpdatetime())) {
				// change handler or gluesource updated, need kill old thread
				removeOldReason = "change job source or glue type, and terminate the old job thread.";

				jobThread = null;
				jobHandler = null;
			}

			// valid handler
			if (jobHandler == null) {
				try {
					IJobHandler originJobHandler = GlueFactory.getInstance()
							.loadNewInstance(triggerParam.getGlueSource());
					jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return new ReturnT<String>(ReturnT.FAIL_CODE, e.getMessage());
				}
			}
		} else if (glueTypeEnum != null && glueTypeEnum.isScript()) {

			// valid old jobThread
			if (jobThread != null && !(jobThread.getHandler() instanceof ScriptJobHandler
					&& ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdatetime() == triggerParam
							.getGlueUpdatetime())) {
				// change script or gluesource updated, need kill old thread
				removeOldReason = "change job source or glue type, and terminate the old job thread.";

				jobThread = null;
				jobHandler = null;
			}

			// valid handler
			if (jobHandler == null) {
				jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(),
						triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
			}
		} else {
			return new ReturnT<String>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
		}

		// executor block strategy
		if (jobThread != null) {

			// 阻塞处理策略
			ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum
					.match(triggerParam.getExecutorBlockStrategy(), null);

			if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {// 并行
				// discard when running
				if (jobThread.isRunningOrHasQueue()) {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							"block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
				}
			} else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {// 覆盖
				// kill running jobThread
				if (jobThread.isRunningOrHasQueue()) {
					removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
					jobThread = null;
				}
			} else {
				// just queue trigger
			}
		}

		// replace thread (new or exists invalid)
		if (jobThread == null) {// 新建一个jobThread代替老的
			jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
		}

		// push data to queue:触发器增加一次要触发执行的任务
		ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
		return pushResult;
	}

}
