package com.xxl.job.admin.core.route;

import com.xxl.job.admin.core.route.strategy.ExecutorRouteBusyover;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteConsistentHash;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteFailover;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteFirst;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteLFU;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteLRU;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteLast;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteRandom;
import com.xxl.job.admin.core.route.strategy.ExecutorRouteRound;
import com.xxl.job.admin.core.util.I18nUtil;

/**
 * 路由策略：当执行器集群部署时，提供丰富的路由策略 Created by xuxueli on 17/3/10.
 */
public enum ExecutorRouteStrategyEnum {

	// FIRST（第一个）：固定选择第一个机器；
	FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),
	// LAST（最后一个）：固定选择最后一个机器；
	LAST(I18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),
	// ROUND（轮询
	ROUND(I18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),
	// RANDOM（随机）：随机选择在线的机器；
	RANDOM(I18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),
	// CONSISTENT_HASH（一致性HASH）：每个任务按照Hash算法固定选择某一台机器，且所有任务均匀散列在不同机器上。
	CONSISTENT_HASH(I18nUtil.getString("jobconf_route_consistenthash"), new ExecutorRouteConsistentHash()),
	// LEAST_FREQUENTLY_USED（最不经常使用）：使用频率最低的机器优先被选举；
	LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),
	// LEAST_RECENTLY_USED（最近最久未使用）：最久为使用的机器优先被选举；
	LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),
	// FAILOVER（故障转移）：按照顺序依次进行心跳检测，第一个心跳检测成功的机器选定为目标执行器并发起调度；
	FAILOVER(I18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),
	// BUSYOVER（忙碌转移）：按照顺序依次进行空闲检测，第一个空闲检测成功的机器选定为目标执行器并发起调度；
	BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyover()),
	// SHARDING_BROADCAST(分片广播)：广播触发对应集群中所有机器执行一次任务，同时系统自动传递分片参数；可根据分片参数开发分片任务；
	SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);

	ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
		this.title = title;
		this.router = router;
	}

	private String title;
	private ExecutorRouter router;

	public String getTitle() {
		return title;
	}

	public ExecutorRouter getRouter() {
		return router;
	}

	/**
	 * 通过名称的对比，找到路由策略对应的枚举信息
	 * 
	 * @param name
	 *            名称
	 * @param defaultItem
	 *            没找到就用默认的
	 * @return
	 */
	public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem) {
		if (name != null) {
			for (ExecutorRouteStrategyEnum item : ExecutorRouteStrategyEnum.values()) {
				if (item.name().equals(name)) {
					return item;
				}
			}
		}
		return defaultItem;
	}

}
