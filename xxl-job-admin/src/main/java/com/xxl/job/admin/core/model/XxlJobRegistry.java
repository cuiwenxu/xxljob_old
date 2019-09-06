package com.xxl.job.admin.core.model;

import java.util.Date;

/**
 * 执行器注册表，维护在线的执行器和调度中心机器地址信息；<br/>
 * Created by xuxueli on 16/9/30.
 */
public class XxlJobRegistry {

	/**
	 * 主键
	 */
	private int id;
	/**
	 * EXECUTOR、ADMIN
	 */
	private String registryGroup;
	/**
	 * 执行器的名字:XxlJobGroup表的appName
	 */
	private String registryKey;
	/**
	 * 注册的ip端口号:10.122.4.69:19998
	 */
	private String registryValue;
	/**
	 * 最后修改时间:超时就要从在线的执行器里面删除
	 */
	private Date updateTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRegistryGroup() {
		return registryGroup;
	}

	public void setRegistryGroup(String registryGroup) {
		this.registryGroup = registryGroup;
	}

	public String getRegistryKey() {
		return registryKey;
	}

	public void setRegistryKey(String registryKey) {
		this.registryKey = registryKey;
	}

	public String getRegistryValue() {
		return registryValue;
	}

	public void setRegistryValue(String registryValue) {
		this.registryValue = registryValue;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
}
