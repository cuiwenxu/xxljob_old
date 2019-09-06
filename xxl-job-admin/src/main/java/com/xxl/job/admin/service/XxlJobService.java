package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * core job action for xxl-job <br/>
 * 操作调度任务相关接口
 * 
 * @author xuxueli 2016-5-28 15:30:33
 */
public interface XxlJobService {

	/**
	 * page list 任务列表查询
	 * 
	 * @param start
	 *            开始处
	 * @param length
	 *            取几条
	 * @param jobGroup
	 *            执行器id
	 * @param jobDesc
	 *            执行器简介
	 * @param executorHandler
	 *            执行器，任务Handler名称
	 * @param filterTime
	 * @return
	 */
	public Map<String, Object> pageList(int start, int length, int jobGroup, String jobDesc, String executorHandler,
			String filterTime);

	/**
	 * add job, default quartz stop <br/>
	 * 增加任务(默认quartz是没启动的)
	 * 
	 * @param jobInfo
	 * @return
	 */
	public ReturnT<String> add(XxlJobInfo jobInfo);

	/**
	 * update job, update quartz-cron if started <br/>
	 * 修改任务,修改quartz-cron如果quartz启动了
	 * 
	 * @param jobInfo
	 * @return
	 */
	public ReturnT<String> update(XxlJobInfo jobInfo);

	/**
	 * remove job, unbind quartz <br/>
	 * 删除任务,停止quartz
	 * 
	 * @param id
	 * @return
	 */
	public ReturnT<String> remove(int id);

	/**
	 * start job, bind quartz <br/>
	 * 启动quartz任务
	 * 
	 * @param id
	 * @return
	 */
	public ReturnT<String> start(int id);

	/**
	 * stop job, unbind quartz <br/>
	 * 停止quartz任务
	 * 
	 * @param id
	 * @return
	 */
	public ReturnT<String> stop(int id);

	/**
	 * dashboard info 获取仪表板展示信息
	 * 
	 * @return
	 */
	public Map<String, Object> dashboardInfo();

	/**
	 * chart info	获取图表展示信息
	 *
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);

}
