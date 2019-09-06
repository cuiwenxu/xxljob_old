package com.xxl.job.core.thread;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;

/**
 * 执行器注册线程 Created by xuxueli on 17/3/2.
 */
public class ExecutorRegistryThread extends Thread {
	private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

	/**
	 * 单例，保证唯一性
	 */
	private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

	public static ExecutorRegistryThread getInstance() {
		return instance;
	}

	private Thread registryThread;// 执行器注册线程

	private volatile boolean toStop = false;

	/**
	 * 执行器信息注册到注册中心
	 * 
	 * @param appName
	 * @param address
	 */
	public void start(final String appName, final String address) {

		// valid
		if (appName == null || appName.trim().length() == 0) {
			logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, appName is null.");
			return;
		}
		if (XxlJobExecutor.getAdminBizList() == null) {
			logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
			return;
		}

		// 注册线程
		registryThread = new Thread(new Runnable() {
			@Override
			public void run() {

				// registry
				while (!toStop) {// 每隔一个时间注册一次，保证执行器的活着状态
					try {
						RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(),
								appName, address);
						for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
							try {
								// 执行器信息注册到注册中心(XxlRpcReferenceBean 来代理的adminBiz)
								ReturnT<String> registryResult = adminBiz.registry(registryParam);
								if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
									registryResult = ReturnT.SUCCESS;
									logger.info(
											">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}",
											new Object[] { registryParam, registryResult });
									break;
								} else {
									logger.info(
											">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}",
											new Object[] { registryParam, registryResult });
								}
							} catch (Exception e) {
								logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
							}

						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}

					try {
						// 休息一个时间
						TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
					} catch (InterruptedException e) {
						logger.warn(">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}",
								e.getMessage());
					}
				}

				// 最后结束的时候，把执行器从远程移除
				try {
					RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName,
							address);
					for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
						try {
							// 执行器从注册中心删除
							ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
							if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
								registryResult = ReturnT.SUCCESS;
								logger.info(
										">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}",
										new Object[] { registryParam, registryResult });
								break;
							} else {
								logger.info(
										">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}",
										new Object[] { registryParam, registryResult });
							}
						} catch (Exception e) {
							logger.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}", registryParam,
									e);
						}

					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				logger.info(">>>>>>>>>>> xxl-job, executor registry thread destory.");

			}
		});
		registryThread.setDaemon(true);
		registryThread.start();
	}

	/**
	 * 停止执行器注册进程
	 */
	public void toStop() {
		toStop = true;
		// interrupt and wait
		registryThread.interrupt();
		try {
			registryThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
