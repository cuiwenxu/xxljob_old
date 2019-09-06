package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by xuxueli on 16/9/30.<br/>
 * 在线的执行器和调度中心信息表操作类
 */
@Mapper
public interface XxlJobRegistryDao {

	/**
	 * 移除超时的记录
	 * 
	 * @param timeout
	 * @return
	 */
	public int removeDead(@Param("timeout") int timeout);

	/**
	 * 查询没有超时的记录
	 * 
	 * @param timeout
	 * @return
	 */
	public List<XxlJobRegistry> findAll(@Param("timeout") int timeout);

	/**
	 * 修改修改时间
	 * 
	 * @param registryGroup
	 *            EXECUTOR、ADMIN
	 * @param registryKey
	 *            执行器的名字:XxlJobGroup表的appName
	 * @param registryValue
	 *            注册的ip端口号:10.122.4.69:19998
	 * @return
	 */
	public int registryUpdate(@Param("registryGroup") String registryGroup, @Param("registryKey") String registryKey,
			@Param("registryValue") String registryValue);

	/**
	 * 新增记录
	 * 
	 * @param registryGroup
	 *            EXECUTOR、ADMIN
	 * @param registryKey
	 *            执行器的名字:XxlJobGroup表的appName
	 * @param registryValue
	 *            注册的ip端口号:10.122.4.69:19998
	 * @return
	 */
	public int registrySave(@Param("registryGroup") String registryGroup, @Param("registryKey") String registryKey,
			@Param("registryValue") String registryValue);

	/**
	 * 删除记录
	 * 
	 * @param registryGroup
	 *            EXECUTOR、ADMIN
	 * @param registryKey
	 *            执行器的名字:XxlJobGroup表的appName
	 * @param registryValue
	 *            注册的ip端口号:10.122.4.69:19998
	 * @return
	 */
	public int registryDelete(@Param("registryGroup") String registGroup, @Param("registryKey") String registryKey,
			@Param("registryValue") String registryValue);

}
