package com.xxl.job.admin.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * 执行器信息表，维护任务执行器信息；<br/>
 * Created by xuxueli on 16/9/30.
 */
public class XxlJobGroup {

	/**
	 * 执行器主键
	 */
	private int id;
	/**
	 * 执行器名称
	 */
	private String appName;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 优先级
	 */
	private int order;
	/**
	 * 执行器地址类型：0=自动注册、1=手动录入
	 */
	private int addressType;
	/**
	 * 执行器地址列表，多地址逗号分隔
	 */
	private String addressList;
	/**
	 * 执行器地址列表(非数据库字段)
	 */
	private List<String> registryList;

	/**
	 * 获取执行器地址列表
	 * 
	 * @return
	 */
	public List<String> getRegistryList() {
		if (StringUtils.isNotBlank(addressList)) {
			registryList = new ArrayList<String>(Arrays.asList(addressList.split(",")));
		}
		return registryList;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getAddressType() {
		return addressType;
	}

	public void setAddressType(int addressType) {
		this.addressType = addressType;
	}

	public String getAddressList() {
		return addressList;
	}

	public void setAddressList(String addressList) {
		this.addressList = addressList;
	}

}
