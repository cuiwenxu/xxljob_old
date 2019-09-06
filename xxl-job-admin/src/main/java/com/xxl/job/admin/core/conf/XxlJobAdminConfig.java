package com.xxl.job.admin.core.conf;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.core.biz.AdminBiz;

/**
 * xxl-job config config配置参数
 *
 * @author xuxueli 2017-04-28
 */
@Configuration
public class XxlJobAdminConfig implements InitializingBean {

	/**
	 * 在加载这个类的时候把spring加载的自己给adminConfig对象，实现单例
	 */
	private static XxlJobAdminConfig adminConfig = null;

	public static XxlJobAdminConfig getAdminConfig() {
		return adminConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		adminConfig = this;
	}

	// --------------------------- conf ------------------------------------------

	// --------------邮箱信息---------------
	@Value("${xxl.job.mail.host}")
	private String mailHost;
	@Value("${xxl.job.mail.port}")
	private String mailPort;
	@Value("${xxl.job.mail.ssl}")
	private boolean mailSSL;
	@Value("${xxl.job.mail.username}")
	private String mailUsername;
	@Value("${xxl.job.mail.password}")
	private String mailPassword;
	@Value("${xxl.job.mail.sendNick}")
	private String mailSendNick;

	// --------------登陆信息---------------
	@Value("${xxl.job.login.username}")
	private String loginUsername;
	@Value("${xxl.job.login.password}")
	private String loginPassword;

	// --------------i18n---------------
	@Value("${xxl.job.i18n}")
	private String i18n;

	// --------------accessToken---------------
	@Value("${xxl.job.accessToken}")
	private String accessToken;

	// --------------------------- dao, service TODO ------------------------------------------
	@Resource
	public XxlJobLogDao xxlJobLogDao;// 日志信息dao
	@Resource
	public XxlJobInfoDao xxlJobInfoDao;// 调度任务信息dao
	@Resource
	public XxlJobRegistryDao xxlJobRegistryDao;// 在线的执行器和调度中心信息表操作类dao
	@Resource
	public XxlJobGroupDao xxlJobGroupDao;// 执行器信息dao
	@Resource
	public AdminBiz adminBiz;// 提供给执行器的API服务service

	public String getMailHost() {
		return mailHost;
	}

	public String getMailPort() {
		return mailPort;
	}

	public boolean isMailSSL() {
		return mailSSL;
	}

	public String getMailUsername() {
		return mailUsername;
	}

	public String getMailPassword() {
		return mailPassword;
	}

	public String getMailSendNick() {
		return mailSendNick;
	}

	public String getLoginUsername() {
		return loginUsername;
	}

	public String getLoginPassword() {
		return loginPassword;
	}

	public String getI18n() {
		return i18n;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public XxlJobLogDao getXxlJobLogDao() {
		return xxlJobLogDao;
	}

	public XxlJobInfoDao getXxlJobInfoDao() {
		return xxlJobInfoDao;
	}

	public XxlJobRegistryDao getXxlJobRegistryDao() {
		return xxlJobRegistryDao;
	}

	public XxlJobGroupDao getXxlJobGroupDao() {
		return xxlJobGroupDao;
	}

	public AdminBiz getAdminBiz() {
		return adminBiz;
	}

}
