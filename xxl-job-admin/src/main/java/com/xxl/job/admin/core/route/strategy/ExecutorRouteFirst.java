package com.xxl.job.admin.core.route.strategy;

import java.util.List;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * 直接取集群地址列表里面的第一台机器来进行执行 <br/>
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteFirst extends ExecutorRouter {

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.core.route.ExecutorRouter#route(com.xxl.job.core.biz.model.TriggerParam, java.util.List)
	 */
	@Override
	public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
		return new ReturnT<String>(addressList.get(0));
	}

}
