package com.xxl.job.admin.core.conf;

import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

/**
 * 注册SchedulerFactoryBean 和xxl-job-admin相关类
 * @author xuxueli 2018-10-28 00:18:17
 */
@Configuration
public class XxlJobDynamicSchedulerConfig {

	/**
	 * SchedulerFactoryBean
	 * @param dataSource
	 * @return
	 */
    @Bean
    public SchedulerFactoryBean getSchedulerFactoryBean(DataSource dataSource){

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setDataSource(dataSource);
        schedulerFactory.setAutoStartup(true);                  // 自动启动
        schedulerFactory.setStartupDelay(20);                   // 延时启动，应用启动成功后在启动
        schedulerFactory.setOverwriteExistingJobs(true);        // 覆盖DB中JOB：true、以数据库中已经存在的为准：false
        schedulerFactory.setApplicationContextSchedulerContextKey("applicationContext");
        schedulerFactory.setConfigLocation(new ClassPathResource("quartz.properties"));

        return schedulerFactory;
    }

    /**
     * 启动注册
     * @param schedulerFactory
     * @return
     */
    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobDynamicScheduler getXxlJobDynamicScheduler(SchedulerFactoryBean schedulerFactory){

        Scheduler scheduler = schedulerFactory.getScheduler();
        
        //base quartz scheduler util
        //定时任务工具类，在启动中做很多事
        XxlJobDynamicScheduler xxlJobDynamicScheduler = new XxlJobDynamicScheduler();
        xxlJobDynamicScheduler.setScheduler(scheduler);

        return xxlJobDynamicScheduler;
    }

}
