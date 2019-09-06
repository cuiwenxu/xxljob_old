package com.xxl.job.admin.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.xxl.job.admin.controller.annotation.PermessionLimit;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.core.biz.AdminBiz;

/**
 * RPC中以spring而不是以额外的jetty启动，所以要加一个调用过来的控制层
 * 
 * @author Administrator
 *
 */
@Controller
public class JobApiController implements InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	/**
	 * RPC远程执行的时候要调用的api请求
	 * 
	 * @param request
	 *            内涵了request请求参数信息的byte数组
	 * @param response
	 *            内涵了response结果参数信息的byte数组
	 * @throws IOException
	 * @throws ServletException
	 */
	@RequestMapping(AdminBiz.MAPPING)
	@PermessionLimit(limit = false)
	public void api(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// RPC远程执行的请求时候要调用的
		XxlJobDynamicScheduler.invokeAdminService(request, response);
	}

}
