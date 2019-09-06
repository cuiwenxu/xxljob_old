package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.annotation.PermessionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.util.CookieUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * 权限拦截, 简易版
 *
 * @author xuxueli 2015-12-12 18:09:04
 */
@Component
public class PermissionInterceptor extends HandlerInterceptorAdapter {


	public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";
	private static String LOGIN_IDENTITY_TOKEN;
	
	
	/**
	 * 生成LOGIN_IDENTITY_TOKEN
	 * @return
	 */
	public static String getLoginIdentityToken() {
		if (LOGIN_IDENTITY_TOKEN == null) {
			String username = XxlJobAdminConfig.getAdminConfig().getLoginUsername();
			String password = XxlJobAdminConfig.getAdminConfig().getLoginPassword();

			// login token
			String tokenTmp = DigestUtils.md5DigestAsHex(String.valueOf(username + "_" + password).getBytes());		//.getBytes("UTF-8")
			tokenTmp = new BigInteger(1, tokenTmp.getBytes()).toString(16);

			LOGIN_IDENTITY_TOKEN = tokenTmp;
		}
		return LOGIN_IDENTITY_TOKEN;
	}

	/**
	 * 登陆
	 * @param response
	 * @param username		账号
	 * @param password		密码
	 * @param ifRemember	是否记住
	 * @return
	 */
	public static boolean login(HttpServletResponse response, String username, String password, boolean ifRemember){
    	// login token
		String tokenTmp = DigestUtils.md5DigestAsHex(String.valueOf(username + "_" + password).getBytes());
		tokenTmp = new BigInteger(1, tokenTmp.getBytes()).toString(16);

		//登陆校验
		if (!getLoginIdentityToken().equals(tokenTmp)){
			//登陆失败
			return false;
		}

		// do login
		CookieUtil.set(response, LOGIN_IDENTITY_KEY, getLoginIdentityToken(), ifRemember);
		return true;
	}
	
	/**
	 * 退出登陆
	 * @param request
	 * @param response
	 */
	public static void logout(HttpServletRequest request, HttpServletResponse response){
		//cookie中删除信息
		CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
	}
	
	
	/**
	 * 是否登录了
	 * @param request
	 * @return
	 */
	public static boolean ifLogin(HttpServletRequest request){
		//cookie中取值
		String indentityInfo = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
		if (indentityInfo==null || !getLoginIdentityToken().equals(indentityInfo.trim())) {
			return false;
		}
		return true;
	}


	/**
	 * {@inheritDoc}
	 * 权限链接
	 * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter#preHandle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		if (!(handler instanceof HandlerMethod)) {
			return super.preHandle(request, response, handler);
		}
		
		if (!ifLogin(request)) {//没有登录的进入,登录了过
			
			HandlerMethod method = (HandlerMethod)handler;
			//如果上面有免登陆的就跳过
			PermessionLimit permission = method.getMethodAnnotation(PermessionLimit.class);
			if (permission == null || permission.limit()) {
				response.sendRedirect(request.getContextPath() + "/toLogin");
				return false;
			}
		}
		
		return super.preHandle(request, response, handler);
	}
	
}
