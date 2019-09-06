package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 执行器信息数据表操作类
 * Created by xuxueli on 16/9/30.
 */
@Mapper
public interface XxlJobGroupDao {

	/**
	 * 获取全部执行器
	 * @return
	 */
    public List<XxlJobGroup> findAll();

    /**
     * 执行器地址类型：0=自动注册、1=手动录入获取全部执行器
     * @param addressType
     * @return
     */
    public List<XxlJobGroup> findByAddressType(@Param("addressType") int addressType);

    /**
     * 保存执行器
     * @param xxlJobGroup
     * @return
     */
    public int save(XxlJobGroup xxlJobGroup);

    /**
     * 修改执行器信息
     * @param xxlJobGroup
     * @return
     */
    public int update(XxlJobGroup xxlJobGroup);

    /**
     * 主键删除执行器信息
     * @param id
     * @return
     */
    public int remove(@Param("id") int id);

    /**
     * 主键查找执行器信息
     * @param id
     * @return
     */
    public XxlJobGroup load(@Param("id") int id);
    
    
    
}
