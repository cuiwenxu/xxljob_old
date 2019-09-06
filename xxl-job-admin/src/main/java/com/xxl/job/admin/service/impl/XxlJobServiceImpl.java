package com.xxl.job.admin.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.schedule.XxlJobDynamicScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;

/**
 * core job action for xxl-job
 * 
 * @author xuxueli 2016-5-28 15:30:33
 */
@Service
public class XxlJobServiceImpl implements XxlJobService {
	private static Logger logger = LoggerFactory.getLogger(XxlJobServiceImpl.class);

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	private XxlJobInfoDao xxlJobInfoDao;
	@Resource
	public XxlJobLogDao xxlJobLogDao;
	@Resource
	private XxlJobLogGlueDao xxlJobLogGlueDao;

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#pageList(int, int, int, java.lang.String, java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public Map<String, Object> pageList(int start, int length, int jobGroup, String jobDesc, String executorHandler,
			String filterTime) {

		// 条件分页查询调度任务信息列表
		List<XxlJobInfo> list = xxlJobInfoDao.pageList(start, length, jobGroup, jobDesc, executorHandler);
		int list_count = xxlJobInfoDao.pageListCount(start, length, jobGroup, jobDesc, executorHandler);

		// fill job info
		if (list != null && list.size() > 0) {
			for (XxlJobInfo jobInfo : list) {
				// 填充任务信息:cron表达式和JobStatus
				XxlJobDynamicScheduler.fillJobInfo(jobInfo);
			}
		}

		// package result
		Map<String, Object> maps = new HashMap<String, Object>();
		maps.put("recordsTotal", list_count); // 总记录数
		maps.put("recordsFiltered", list_count); // 过滤后的总记录数
		maps.put("data", list); // 分页列表
		return maps;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#add(com.xxl.job.admin.core.model.XxlJobInfo)
	 */
	@Override
	public ReturnT<String> add(XxlJobInfo jobInfo) {
		// 执行器信息
		XxlJobGroup group = xxlJobGroupDao.load(jobInfo.getJobGroup());
		if (group == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
		}
		if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid"));
		}
		if (StringUtils.isBlank(jobInfo.getJobDesc())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
		}
		if (StringUtils.isBlank(jobInfo.getAuthor())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
		}
		if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
		}
		if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
		}
		if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
		}
		if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())
				&& StringUtils.isBlank(jobInfo.getExecutorHandler())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
		}

		// fix "\r" in shell
		if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource() != null) {
			jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
		}

		// ChildJobId valid
		if (StringUtils.isNotBlank(jobInfo.getChildJobId())) {
			String[] childJobIds = StringUtils.split(jobInfo.getChildJobId(), ",");
			for (String childJobIdItem : childJobIds) {
				if (StringUtils.isNotBlank(childJobIdItem) && StringUtils.isNumeric(childJobIdItem)) {
					XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.valueOf(childJobIdItem));
					if (childJobInfo == null) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})"
										+ I18nUtil.getString("system_not_found")), childJobIdItem));
					}
				} else {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})"
									+ I18nUtil.getString("system_unvalid")), childJobIdItem));
				}
			}
			jobInfo.setChildJobId(StringUtils.join(childJobIds, ","));
		}

		// add in db
		xxlJobInfoDao.save(jobInfo);
		if (jobInfo.getId() < 1) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
		}

		return new ReturnT<String>(String.valueOf(jobInfo.getId()));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#update(com.xxl.job.admin.core.model.XxlJobInfo)
	 */
	@Override
	public ReturnT<String> update(XxlJobInfo jobInfo) {

		// valid
		if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_unvalid"));
		}
		if (StringUtils.isBlank(jobInfo.getJobDesc())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
		}
		if (StringUtils.isBlank(jobInfo.getAuthor())) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
		}
		if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
		}
		if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
		}

		// ChildJobId valid
		if (StringUtils.isNotBlank(jobInfo.getChildJobId())) {
			String[] childJobIds = StringUtils.split(jobInfo.getChildJobId(), ",");
			for (String childJobIdItem : childJobIds) {
				if (StringUtils.isNotBlank(childJobIdItem) && StringUtils.isNumeric(childJobIdItem)) {
					XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.valueOf(childJobIdItem));
					if (childJobInfo == null) {
						return new ReturnT<String>(ReturnT.FAIL_CODE,
								MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})"
										+ I18nUtil.getString("system_not_found")), childJobIdItem));
					}
				} else {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})"
									+ I18nUtil.getString("system_unvalid")), childJobIdItem));
				}
			}
			jobInfo.setChildJobId(StringUtils.join(childJobIds, ","));
		}

		// 主键查询调度任务信息
		XxlJobInfo exists_jobInfo = xxlJobInfoDao.loadById(jobInfo.getId());
		if (exists_jobInfo == null) {
			return new ReturnT<String>(ReturnT.FAIL_CODE,
					(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
		}
		// String old_cron = exists_jobInfo.getJobCron();

		// 设置调度任务信息
		exists_jobInfo.setJobCron(jobInfo.getJobCron());
		exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
		exists_jobInfo.setAuthor(jobInfo.getAuthor());
		exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
		exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
		exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
		exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
		exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
		exists_jobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
		exists_jobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
		exists_jobInfo.setChildJobId(jobInfo.getChildJobId());
		// 修改数据库表调度任务信息
		xxlJobInfoDao.update(exists_jobInfo);

		// update quartz-cron if started
		// 修改quartz信息,如果这个任务以前启动了
		String qz_group = String.valueOf(exists_jobInfo.getJobGroup());
		String qz_name = String.valueOf(exists_jobInfo.getId());
		try {
			XxlJobDynamicScheduler.updateJobCron(qz_group, qz_name, exists_jobInfo.getJobCron());
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
			return ReturnT.FAIL;
		}

		return ReturnT.SUCCESS;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#remove(int)
	 */
	@Override
	public ReturnT<String> remove(int id) {
		// 主键查询调度任务信息
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
		String group = String.valueOf(xxlJobInfo.getJobGroup());
		String name = String.valueOf(xxlJobInfo.getId());

		try {
			// unbind quartz===删除触发器
			XxlJobDynamicScheduler.removeJob(name, group);

			xxlJobInfoDao.delete(id);// 删除任务
			xxlJobLogDao.delete(id);// 删除日志
			xxlJobLogGlueDao.deleteByJobId(id);// 删除

			return ReturnT.SUCCESS;
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
			return ReturnT.FAIL;
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#start(int)
	 */
	@Override
	public ReturnT<String> start(int id) {

		// 主键查询调度任务信息
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);

		String group = String.valueOf(xxlJobInfo.getJobGroup());// 执行器表id
		String name = String.valueOf(xxlJobInfo.getId());// 任务表id
		String cronExpression = xxlJobInfo.getJobCron();// cron表达式

		try {

			// 增加并开始执行任务
			boolean ret = XxlJobDynamicScheduler.addJob(name, group, cronExpression);

			return ret ? ReturnT.SUCCESS : ReturnT.FAIL;
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
			return ReturnT.FAIL;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#stop(int)
	 */
	@Override
	public ReturnT<String> stop(int id) {
		// 主键查询调度任务信息
		XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
		String group = String.valueOf(xxlJobInfo.getJobGroup());
		String name = String.valueOf(xxlJobInfo.getId());

		try {
			// bind quartz == 删除触发器和任务
			boolean ret = XxlJobDynamicScheduler.removeJob(name, group);

			return ret ? ReturnT.SUCCESS : ReturnT.FAIL;

		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
			return ReturnT.FAIL;
		}
	}

	// ---------------------主页的两个查询方法------------------TODO-----------------

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#dashboardInfo()
	 */
	@Override
	public Map<String, Object> dashboardInfo() {
		// 查询全部调度任务信息总数
		int jobInfoCount = xxlJobInfoDao.findAllCount();
		// 通过handleCode查询日志的数量
		int jobLogCount = xxlJobLogDao.triggerCountByHandleCode(-1);
		// 通过handleCode查询日志的数量
		int jobLogSuccessCount = xxlJobLogDao.triggerCountByHandleCode(ReturnT.SUCCESS_CODE);

		// 获取全部执行器
		List<XxlJobGroup> groupList = xxlJobGroupDao.findAll();

		// executor count
		Set<String> executerAddressSet = new HashSet<String>();
		if (CollectionUtils.isNotEmpty(groupList)) {
			for (XxlJobGroup group : groupList) {
				if (CollectionUtils.isNotEmpty(group.getRegistryList())) {
					executerAddressSet.addAll(group.getRegistryList());
				}
			}
		}
		// 执行器地址列表数量
		int executorCount = executerAddressSet.size();

		Map<String, Object> dashboardMap = new HashMap<String, Object>();
		dashboardMap.put("jobInfoCount", jobInfoCount);
		dashboardMap.put("jobLogCount", jobLogCount);
		dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
		dashboardMap.put("executorCount", executorCount);
		return dashboardMap;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.xxl.job.admin.service.XxlJobService#chartInfo(java.util.Date, java.util.Date)
	 */
	@Override
	public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

		// process
		List<String> triggerDayList = new ArrayList<String>();
		List<Integer> triggerDayCountRunningList = new ArrayList<Integer>();
		List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
		List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
		int triggerCountRunningTotal = 0;
		int triggerCountSucTotal = 0;
		int triggerCountFailTotal = 0;

		List<Map<String, Object>> triggerCountMapAll = xxlJobLogDao.triggerCountByDay(startDate, endDate);
		if (CollectionUtils.isNotEmpty(triggerCountMapAll)) {
			for (Map<String, Object> item : triggerCountMapAll) {
				String day = String.valueOf(item.get("triggerDay"));
				int triggerDayCount = Integer.valueOf(String.valueOf(item.get("triggerDayCount")));
				int triggerDayCountRunning = Integer.valueOf(String.valueOf(item.get("triggerDayCountRunning")));
				int triggerDayCountSuc = Integer.valueOf(String.valueOf(item.get("triggerDayCountSuc")));
				int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

				triggerDayList.add(day);
				triggerDayCountRunningList.add(triggerDayCountRunning);
				triggerDayCountSucList.add(triggerDayCountSuc);
				triggerDayCountFailList.add(triggerDayCountFail);

				triggerCountRunningTotal += triggerDayCountRunning;
				triggerCountSucTotal += triggerDayCountSuc;
				triggerCountFailTotal += triggerDayCountFail;
			}
		} else {
			for (int i = 4; i > -1; i--) {
				triggerDayList.add(FastDateFormat.getInstance("yyyy-MM-dd").format(DateUtils.addDays(new Date(), -i)));
				triggerDayCountRunningList.add(0);
				triggerDayCountSucList.add(0);
				triggerDayCountFailList.add(0);
			}
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("triggerDayList", triggerDayList);
		result.put("triggerDayCountRunningList", triggerDayCountRunningList);
		result.put("triggerDayCountSucList", triggerDayCountSucList);
		result.put("triggerDayCountFailList", triggerDayCountFailList);

		result.put("triggerCountRunningTotal", triggerCountRunningTotal);
		result.put("triggerCountSucTotal", triggerCountSucTotal);
		result.put("triggerCountFailTotal", triggerCountFailTotal);

		return new ReturnT<Map<String, Object>>(result);
	}

}
