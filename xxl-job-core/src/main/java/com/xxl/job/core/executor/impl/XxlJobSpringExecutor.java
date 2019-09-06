package com.xxl.job.core.executor.impl;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * 执行器Spring版本实现:第二步加载执行器
 * 
 * @author duhai
 * @date 2019年3月20日
 * @see
 * @since JDK 1.8.0
 */
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware {

	/**
	 * 开始
	 * 
	 * @see com.xxl.job.core.executor.XxlJobExecutor#start()
	 */
	@Override
	public void start() throws Exception {

		//初始化JobHandler的仓库
		initJobHandlerRepository(applicationContext);

		// refresh GlueFactory
		GlueFactory.refreshInstance(1);

		//XxlJobExecutor启动
		super.start();
	}

	/**
	 * 注册jobhandler到map中
	 * 
	 * @param applicationContext
	 */
	private void initJobHandlerRepository(ApplicationContext applicationContext) {
		if (applicationContext == null) {
			return;
		}

		// 获取有JobHandler注解的所有类
		Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(JobHandler.class);

		if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
			for (Object serviceBean : serviceBeanMap.values()) {
				if (serviceBean instanceof IJobHandler) {
					//获取注解中name的值@JobHandler(value="commandJobHandler")
					String name = serviceBean.getClass().getAnnotation(JobHandler.class).value();
					IJobHandler handler = (IJobHandler) serviceBean;
					
					//从jobHandlerRepository中找，如果存在就抛出异常
					if (loadJobHandler(name) != null) {
						throw new RuntimeException("xxl-job jobhandler naming conflicts.");
					}
					//注册到jobHandlerRepository的static的Map中去，方便在调用的时候找到
					registJobHandler(name, handler);
				}
			}
		}
	}

	//----------------------------applicationContext---------TODO--------------------------

	/**
	 * applicationContext 注册
	 */
	private static ApplicationContext applicationContext;

	@SuppressWarnings("static-access")
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
