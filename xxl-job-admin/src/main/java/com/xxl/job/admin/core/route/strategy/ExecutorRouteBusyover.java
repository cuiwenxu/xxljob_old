package com.xxl.job.admin.core.route.strategy;

import java.util.List;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * 忙碌转移<br/>
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteBusyover extends ExecutorRouter {

	@Override
	public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
		StringBuffer idleBeatResultSB = new StringBuffer();
		// 循环集群地址
		for (String address : addressList) {
			// beat
			ReturnT<String> idleBeatResult = null;
			try {
				// 向执行服务器发送消息，判断当前jobId对应的线程是否忙碌，接下来可以看一下idleBeat这个方法
				ExecutorBiz executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
				// 忙碌检测
				idleBeatResult = executorBiz.idleBeat(triggerParam.getJobId());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				idleBeatResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
			}

			// 拼接日志 ， 收集日志信息，后期一起返回
			idleBeatResultSB.append((idleBeatResultSB.length() > 0) ? "<br><br>" : "")
					.append(I18nUtil.getString("jobconf_idleBeat") + "：").append("<br>address：").append(address)
					.append("<br>code：").append(idleBeatResult.getCode()).append("<br>msg：")
					.append(idleBeatResult.getMsg());

			// 返回成功，代表这台执行服务器对应的线程处于空闲状态
			if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
				idleBeatResult.setMsg(idleBeatResultSB.toString());
				idleBeatResult.setContent(address);
				return idleBeatResult;
			}
		}

		return new ReturnT<String>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
	}

}
