package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

/**
 * 执行器回调调度中心的方法实现
 * 
 * @author xuxueli 2017-07-27 21:54:20
 */
@Service
public class AdminBizImpl implements AdminBiz {
	private static Logger logger = LoggerFactory.getLogger(AdminBizImpl.class);

	@Resource
	public XxlJobLogDao xxlJobLogDao;
	@Resource
	private XxlJobInfoDao xxlJobInfoDao;
	@Resource
	private XxlJobRegistryDao xxlJobRegistryDao;

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.AdminBiz#callback(java.util.List)
	 */
	@Override
	public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
		for (HandleCallbackParam handleCallbackParam : callbackParamList) {
			//执行器回调后的处理
			ReturnT<String> callbackResult = callback(handleCallbackParam);
			
			logger.info(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
					(callbackResult.getCode() == IJobHandler.SUCCESS.getCode() ? "success" : "fail"),
					handleCallbackParam, callbackResult);
		}

		return ReturnT.SUCCESS;
	}

	/**
	 * 执行器回调后的处理
	 * @param handleCallbackParam
	 * @return
	 */
	private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
		// 主键查询日志
		XxlJobLog log = xxlJobLogDao.load(handleCallbackParam.getLogId());
		if (log == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
		}
		if (log.getHandleCode() > 0) {
			// avoid repeat callback, trigger child job etc
			return new ReturnT<String>(ReturnT.FAIL_CODE, "log repeate callback."); 
		}

		// trigger success, to trigger child job
		String callbackMsg = null;
		//执行成功
		if (IJobHandler.SUCCESS.getCode() == handleCallbackParam.getExecuteResult().getCode()) {
			//主键查询调度任务信息
			XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(log.getJobId());
			
			// 存在子任务ID，多个逗号分隔
			if (xxlJobInfo != null && StringUtils.isNotBlank(xxlJobInfo.getChildJobId())) {
				callbackMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"
						+ I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>";

				String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
				for (int i = 0; i < childJobIds.length; i++) {
					int childJobId = (StringUtils.isNotBlank(childJobIds[i]) && StringUtils.isNumeric(childJobIds[i]))
							? Integer.valueOf(childJobIds[i]) : -1;
					if (childJobId > 0) {

						JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null);
						ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

						// add msg
						callbackMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"), (i + 1),
								childJobIds.length, childJobIds[i],
								(triggerChildResult.getCode() == ReturnT.SUCCESS_CODE
										? I18nUtil.getString("system_success") : I18nUtil.getString("system_fail")),
								triggerChildResult.getMsg());
					} else {
						callbackMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"), (i + 1),
								childJobIds.length, childJobIds[i]);
					}
				}

			}
		}

		// handle msg 执行结果的消息拼装
		StringBuffer handleMsg = new StringBuffer();
		if (log.getHandleMsg() != null) {
			handleMsg.append(log.getHandleMsg()).append("<br>");
		}
		if (handleCallbackParam.getExecuteResult().getMsg() != null) {
			handleMsg.append(handleCallbackParam.getExecuteResult().getMsg());
		}
		if (callbackMsg != null) {
			handleMsg.append(callbackMsg);
		}

		// success, save log修改日志信息
		log.setHandleTime(new Date());
		log.setHandleCode(handleCallbackParam.getExecuteResult().getCode());
		log.setHandleMsg(handleMsg.toString());
		//修改结束信息(执行器回调后调用)
		xxlJobLogDao.updateHandleInfo(log);

		return ReturnT.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.AdminBiz#registry(com.xxl.job.core.biz.model.RegistryParam)
	 */
	@Override
	public ReturnT<String> registry(RegistryParam registryParam) {
		//在线的执行器修改最后修改时间
		int ret = xxlJobRegistryDao.registryUpdate(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
				registryParam.getRegistryValue());
		if (ret < 1) {
			//新增在线的执行器记录
			xxlJobRegistryDao.registrySave(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
					registryParam.getRegistryValue());
		}
		return ReturnT.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.core.biz.AdminBiz#registryRemove(com.xxl.job.core.biz.model.RegistryParam)
	 */
	@Override
	public ReturnT<String> registryRemove(RegistryParam registryParam) {
		//删除在线的执行器记录
		xxlJobRegistryDao.registryDelete(registryParam.getRegistGroup(), registryParam.getRegistryKey(),
				registryParam.getRegistryValue());
		return ReturnT.SUCCESS;
	}

}
