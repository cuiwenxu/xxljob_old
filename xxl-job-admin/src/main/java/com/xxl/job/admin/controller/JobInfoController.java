package com.xxl.job.admin.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;

/**
 * 任务控制层
 * 
 * @author duhai
 * @date 2019年3月20日
 * @see
 * @since JDK 1.8.0
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	private XxlJobService xxlJobService;

	/**
	 * 
	 * @param model
	 * @param jobGroup
	 *            执行器id
	 * @return
	 */
	@RequestMapping
	public String index(Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

		// 枚举-字典
		model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values()); // 路由策略-列表
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values()); // Glue类型-字典
		model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values()); // 阻塞处理策略-字典

		// 任务组
		// 获取全部执行器
		List<XxlJobGroup> jobGroupList = xxlJobGroupDao.findAll();
		model.addAttribute("JobGroupList", jobGroupList);
		model.addAttribute("jobGroup", jobGroup);

		return "jobinfo/jobinfo.index";
	}

	/**
	 * 任务列表查询
	 * 
	 * @param start
	 *            开始处
	 * @param length
	 *            取几条
	 * @param jobGroup
	 *            执行器id
	 * @param jobDesc
	 *            执行器简介
	 * @param executorHandler
	 *            执行器，任务Handler名称
	 * @param filterTime
	 * @return
	 */
	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
			@RequestParam(required = false, defaultValue = "10") int length, int jobGroup, String jobDesc,
			String executorHandler, String filterTime) {
		// page list 任务列表查询
		return xxlJobService.pageList(start, length, jobGroup, jobDesc, executorHandler, filterTime);
	}

	/**
	 * 任务新增
	 * 
	 * @param jobInfo
	 * @return
	 */
	@RequestMapping("/add")
	@ResponseBody
	public ReturnT<String> add(XxlJobInfo jobInfo) {
		return xxlJobService.add(jobInfo);
	}

	/**
	 * 任务更新
	 * 
	 * @param jobInfo
	 * @return
	 */
	@RequestMapping("/update")
	@ResponseBody
	public ReturnT<String> update(XxlJobInfo jobInfo) {
		return xxlJobService.update(jobInfo);
	}

	/**
	 * 任务删除
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping("/remove")
	@ResponseBody
	public ReturnT<String> remove(int id) {
		return xxlJobService.remove(id);
	}

	/**
	 * 任务启动
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping("/start")
	@ResponseBody
	public ReturnT<String> start(int id) {
		return xxlJobService.start(id);
	}

	/**
	 * 任务停止
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping("/stop")
	@ResponseBody
	public ReturnT<String> pause(int id) {
		return xxlJobService.stop(id);
	}

	/**
	 * 任务触发
	 * 
	 * @param id				调度任务信息XxlJobInfo表主键
	 * @param executorParam		执行参数
	 * @return
	 */
	@RequestMapping("/trigger")
	@ResponseBody
	// @PermessionLimit(limit = false)
	public ReturnT<String> triggerJob(int id, String executorParam) {
		// force cover job param
		if (executorParam == null) {
			executorParam = "";
		}
		// 执行一次任务
		JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam);
		return ReturnT.SUCCESS;
	}

}
