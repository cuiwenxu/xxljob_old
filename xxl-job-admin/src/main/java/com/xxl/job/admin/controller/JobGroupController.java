package com.xxl.job.admin.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;

/**
 * job group controller <br/>
 * 执行器管理控制层
 * 
 * @author xuxueli 2016-10-02 20:52:56
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

	@Resource
	public XxlJobInfoDao xxlJobInfoDao;
	@Resource
	public XxlJobGroupDao xxlJobGroupDao;

	/**
	 * 获取全部的执行器
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping
	public String index(Model model) {

		// 获取全部执行器
		List<XxlJobGroup> list = xxlJobGroupDao.findAll();

		model.addAttribute("list", list);
		return "jobgroup/jobgroup.index";
	}

	/**
	 * 保存执行器
	 * 
	 * @param xxlJobGroup
	 * @return
	 */
	@RequestMapping("/save")
	@ResponseBody
	public ReturnT<String> save(XxlJobGroup xxlJobGroup) {

		// valid
		if (xxlJobGroup.getAppName() == null || StringUtils.isBlank(xxlJobGroup.getAppName())) {
			return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + "AppName"));
		}
		if (xxlJobGroup.getAppName().length() < 4 || xxlJobGroup.getAppName().length() > 64) {
			return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appName_length"));
		}
		if (xxlJobGroup.getTitle() == null || StringUtils.isBlank(xxlJobGroup.getTitle())) {
			return new ReturnT<String>(500,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
		}
		if (xxlJobGroup.getAddressType() != 0) {
			if (StringUtils.isBlank(xxlJobGroup.getAddressList())) {
				return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
			}
			String[] addresss = xxlJobGroup.getAddressList().split(",");
			for (String item : addresss) {
				if (StringUtils.isBlank(item)) {
					return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
				}
			}
		}

		// 保存执行器
		int ret = xxlJobGroupDao.save(xxlJobGroup);
		return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
	}

	/**
	 * 修改执行器
	 * 
	 * @param xxlJobGroup
	 * @return
	 */
	@RequestMapping("/update")
	@ResponseBody
	public ReturnT<String> update(XxlJobGroup xxlJobGroup) {
		// valid
		if (xxlJobGroup.getAppName() == null || StringUtils.isBlank(xxlJobGroup.getAppName())) {
			return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + "AppName"));
		}
		if (xxlJobGroup.getAppName().length() < 4 || xxlJobGroup.getAppName().length() > 64) {
			return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appName_length"));
		}
		if (xxlJobGroup.getTitle() == null || StringUtils.isBlank(xxlJobGroup.getTitle())) {
			return new ReturnT<String>(500,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
		}
		if (xxlJobGroup.getAddressType() == 0) {
			// 0=自动注册
			// 根据执行器名称查询还活着的执行器de注册的ip端口号:10.122.4.69:19998列表
			List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppName());
			String addressListStr = null;
			if (CollectionUtils.isNotEmpty(registryList)) {
				Collections.sort(registryList);
				addressListStr = StringUtils.join(registryList, ",");
			}
			xxlJobGroup.setAddressList(addressListStr);
		} else {
			// 1=手动录入
			if (StringUtils.isBlank(xxlJobGroup.getAddressList())) {
				return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
			}
			String[] addresss = xxlJobGroup.getAddressList().split(",");
			for (String item : addresss) {
				if (StringUtils.isBlank(item)) {
					return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
				}
			}
		}

		// 修改执行器信息
		int ret = xxlJobGroupDao.update(xxlJobGroup);
		return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
	}

	/**
	 * 根据执行器名称查询还活着的执行器de注册的ip端口号:10.122.4.69:19998列表
	 * 
	 * @param appNameParam
	 * @return
	 */
	private List<String> findRegistryByAppName(String appNameParam) {
		HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
		// 查询没有超时的在线的执行器记录
		List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
				.findAll(RegistryConfig.DEAD_TIMEOUT);
		if (list != null) {
			// 循环没有超时的在线的执行器记录
			for (XxlJobRegistry item : list) {
				// 是执行器
				if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
					// 执行器的名字:XxlJobGroup表的appName
					String appName = item.getRegistryKey();
					// 执行器地址列表
					List<String> registryList = appAddressMap.get(appName);
					if (registryList == null) {
						registryList = new ArrayList<String>();
					}

					// 列表中是否包含这个注册的ip端口号:10.122.4.69:19998
					if (!registryList.contains(item.getRegistryValue())) {
						registryList.add(item.getRegistryValue());
					}

					appAddressMap.put(appName, registryList);
				}
			}
		}
		return appAddressMap.get(appNameParam);
	}

	/**
	 * 删除执行器
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping("/remove")
	@ResponseBody
	public ReturnT<String> remove(int id) {

		// valid 条件查询调度任务信息总数
		int count = xxlJobInfoDao.pageListCount(0, 10, id, null, null);
		if (count > 0) {
			return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_0"));
		}

		// 获取全部执行器
		List<XxlJobGroup> allList = xxlJobGroupDao.findAll();
		if (allList.size() == 1) {// 至少要有一个
			return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_1"));
		}

		// 删除执行器
		int ret = xxlJobGroupDao.remove(id);
		return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
	}

}
