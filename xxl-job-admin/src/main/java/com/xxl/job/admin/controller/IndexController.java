package com.xxl.job.admin.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xxl.job.admin.controller.annotation.PermessionLimit;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;

/**
 * index controller index和登录控制层
 * 
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
public class IndexController {

	@Resource
	private XxlJobService xxlJobService;

	/**
	 * 首页信息
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/")
	public String index(Model model) {
		// 获取仪表板展示信息
		Map<String, Object> dashboardMap = xxlJobService.dashboardInfo();
		model.addAllAttributes(dashboardMap);

		return "index";
	}

	/**
	 * 图表信息
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@RequestMapping("/chartInfo")
	@ResponseBody
	public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
		ReturnT<Map<String, Object>> chartInfo = xxlJobService.chartInfo(startDate, endDate);
		return chartInfo;
	}

	/**
	 * 去登录
	 * 
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping("/toLogin")
	@PermessionLimit(limit = false)
	public String toLogin(Model model, HttpServletRequest request) {
		if (PermissionInterceptor.ifLogin(request)) {
			return "redirect:/";
		}
		return "login";
	}

	/**
	 * 登录
	 * 
	 * @param request
	 * @param response
	 * @param userName
	 * @param password
	 * @param ifRemember
	 * @return
	 */
	@RequestMapping(value = "login", method = RequestMethod.POST)
	@ResponseBody
	@PermessionLimit(limit = false)
	public ReturnT<String> loginDo(HttpServletRequest request, HttpServletResponse response, String userName,
			String password, String ifRemember) {
		// valid
		if (PermissionInterceptor.ifLogin(request)) {
			return ReturnT.SUCCESS;
		}

		// param
		if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
			return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
		}
		boolean ifRem = (StringUtils.isNotBlank(ifRemember) && "on".equals(ifRemember)) ? true : false;

		// do login
		boolean loginRet = PermissionInterceptor.login(response, userName, password, ifRem);
		if (!loginRet) {
			return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
		}
		return ReturnT.SUCCESS;
	}

	/**
	 * 退出登录
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "logout", method = RequestMethod.POST)
	@ResponseBody
	@PermessionLimit(limit = false)
	public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
		if (PermissionInterceptor.ifLogin(request)) {
			PermissionInterceptor.logout(request, response);
		}
		return ReturnT.SUCCESS;
	}

	/**
	 * 帮助页面
	 * 
	 * @return
	 */
	@RequestMapping("/help")
	public String help() {

		/*
		 * if (!PermissionInterceptor.ifLogin(request)) { return "redirect:/toLogin"; }
		 */

		return "help";
	}

	/**
	 * 处理Date参数
	 * 
	 * @param binder
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}

}
