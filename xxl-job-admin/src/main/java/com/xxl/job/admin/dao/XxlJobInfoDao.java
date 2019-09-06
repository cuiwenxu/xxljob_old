package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job info <br/>
 * 调度任务信息数据库表操作类
 * 
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface XxlJobInfoDao {

	/**
	 * 条件分页查询调度任务信息列表
	 * @param offset			跳过几条：0开始
	 * @param pagesize			获取几条
	 * @param jobGroup			执行器主键ID
	 * @param jobDesc			执行器简介
	 * @param executorHandler	执行器，任务Handler名称
	 * @return
	 */
	public List<XxlJobInfo> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize,
			@Param("jobGroup") int jobGroup, @Param("jobDesc") String jobDesc,
			@Param("executorHandler") String executorHandler);

	/**
	 * 条件查询调度任务信息总数
	 * @param offset			跳过几条：0开始
	 * @param pagesize			获取几条
	 * @param jobGroup			执行器主键ID
	 * @param jobDesc			执行器简介
	 * @param executorHandler	执行器，任务Handler名称
	 * @return
	 */
	public int pageListCount(@Param("offset") int offset, @Param("pagesize") int pagesize,
			@Param("jobGroup") int jobGroup, @Param("jobDesc") String jobDesc,
			@Param("executorHandler") String executorHandler);

	/**
	 * 保存调度任务信息
	 * @param info
	 * @return
	 */
	public int save(XxlJobInfo info);

	/**
	 * 主键查询调度任务信息
	 * @param id
	 * @return
	 */
	public XxlJobInfo loadById(@Param("id") int id);

	/**
	 * 修改调度任务信息
	 * @param item
	 * @return
	 */
	public int update(XxlJobInfo item);

	/**
	 * 主键删除调度任务信息
	 * @param id
	 * @return
	 */
	public int delete(@Param("id") int id);

	/**
	 * 通过执行器主键ID查询调度任务信息列表
	 * @param jobGroup
	 * @return
	 */
	public List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

	/**
	 * 查询全部调度任务信息总数
	 * @return
	 */
	public int findAllCount();

}
