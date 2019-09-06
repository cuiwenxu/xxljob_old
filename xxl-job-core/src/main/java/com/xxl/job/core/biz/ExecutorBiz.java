package com.xxl.job.core.biz;

import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * Created by xuxueli on 17/3/1.
 */
public interface ExecutorBiz {

	/**
	 * beat 心跳检测：调度中心使用
	 * 
	 * @return
	 */
	public ReturnT<String> beat();

	/**
	 * idle beat忙碌检测：调度中心使用
	 *
	 * @param jobId
	 * @return
	 */
	public ReturnT<String> idleBeat(int jobId);

	/**
	 * kill终止任务：调度中心使用
	 * 
	 * @param jobId
	 * @return
	 */
	public ReturnT<String> kill(int jobId);

	/**
	 * log获取Rolling Log：调度中心使用
	 * 
	 * @param logDateTim
	 * @param logId
	 * @param fromLineNum
	 * @return
	 */
	public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum);

	/**
	 * run触发任务执行：调度中心使用；本地进行任务开发时，可使用该API服务模拟触发任务；
	 * 
	 * @param triggerParam
	 * @return
	 */
	public ReturnT<String> run(TriggerParam triggerParam);

}
