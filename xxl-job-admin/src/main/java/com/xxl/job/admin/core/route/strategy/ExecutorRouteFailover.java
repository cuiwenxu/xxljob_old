package com.xxl.job.admin.core.route.strategy;

import java.util.List;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * 故障转移 这个策略比较简单，遍历集群地址列表，如果失败，则继续调用下一台机器，成功则跳出循环，返回成功信息 <br/>
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteFailover extends ExecutorRouter {

	@Override
	public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

		StringBuffer beatResultSB = new StringBuffer();
		// 循环集群地址
		for (String address : addressList) {
			// beat
			ReturnT<String> beatResult = null;
			try {
				// 向执行器发送 执行beat信息 ， 试探该机器是否可以正常工作
				ExecutorBiz executorBiz = XxlJobDynamicScheduler.getExecutorBiz(address);
				// 心跳检测
				beatResult = executorBiz.beat();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
			}
			// 拼接日志 ， 收集日志信息，后期一起返回
			beatResultSB.append((beatResultSB.length() > 0) ? "<br><br>" : "")
					.append(I18nUtil.getString("jobconf_beat") + "：").append("<br>address：").append(address)
					.append("<br>code：").append(beatResult.getCode()).append("<br>msg：").append(beatResult.getMsg());

			// 返回状态为成功
			if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {
				beatResult.setMsg(beatResultSB.toString());
				beatResult.setContent(address);
				return beatResult;
			}
		}

		return new ReturnT<String>(ReturnT.FAIL_CODE, beatResultSB.toString());

	}
}
