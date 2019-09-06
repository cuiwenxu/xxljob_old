package com.xxl.job.admin.core.route.strategy;

import java.util.List;
import java.util.Random;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

/**
 * 随机<br/>
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteRandom extends ExecutorRouter {

	private static Random localRandom = new Random();

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.core.route.ExecutorRouter#route(com.xxl.job.core.biz.model.TriggerParam, java.util.List)
	 */
	@Override
	public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
		String address = addressList.get(localRandom.nextInt(addressList.size()));
		return new ReturnT<String>(address);
	}

}
