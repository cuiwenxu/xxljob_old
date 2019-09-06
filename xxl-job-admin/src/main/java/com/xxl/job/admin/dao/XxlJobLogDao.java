package com.xxl.job.admin.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * 任务执行的日志信息
 * 
 * @author duhai
 * @date 2019年3月20日
 * @see
 * @since JDK 1.8.0
 */
@Mapper
public interface XxlJobLogDao {

	/**
	 * 条件分页查询日志列表
	 * 
	 * @param offset
	 *            跳过几条：0开始
	 * @param pagesize
	 *            获取几条
	 * @param jobGroup
	 *            执行器主键ID
	 * @param jobId
	 *            任务id
	 * @param triggerTimeStart
	 *            开始时间
	 * @param triggerTimeEnd
	 *            结束时间
	 * @param logStatus
	 *            1(成功)：handle_code = 200;<br/>
	 *            2(失败)：;<br/>
	 *            3(进行中)：trigger_code = 200 AND t.handle_code = 0<br/>
	 * @return
	 */
	public List<XxlJobLog> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize,
			@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
			@Param("triggerTimeStart") Date triggerTimeStart, @Param("triggerTimeEnd") Date triggerTimeEnd,
			@Param("logStatus") int logStatus);

	/**
	 * 查询日志总数
	 * 
	 * @param offset
	 *            跳过几条：0开始
	 * @param pagesize
	 *            获取几条
	 * @param jobGroup
	 *            执行器主键ID
	 * @param jobId
	 *            任务id
	 * @param triggerTimeStart
	 *            开始时间
	 * @param triggerTimeEnd
	 *            结束时间
	 * @param logStatus
	 *            1(成功)：handle_code = 200;<br/>
	 *            2(失败)：;<br/>
	 *            3(进行中)：trigger_code = 200 AND t.handle_code = 0<br/>
	 * @return
	 */
	public int pageListCount(@Param("offset") int offset, @Param("pagesize") int pagesize,
			@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
			@Param("triggerTimeStart") Date triggerTimeStart, @Param("triggerTimeEnd") Date triggerTimeEnd,
			@Param("logStatus") int logStatus);

	/**
	 * 主键查询日志
	 * 
	 * @param id
	 * @return
	 */
	public XxlJobLog load(@Param("id") int id);

	/**
	 * 保存日志
	 * 
	 * @param xxlJobLog
	 * @return
	 */
	public int save(XxlJobLog xxlJobLog);

	/**
	 * 修改日志触发信息
	 * 
	 * @param xxlJobLog
	 * @return
	 */
	public int updateTriggerInfo(XxlJobLog xxlJobLog);

	/**
	 * 修改结束信息(执行器回调后调用)
	 * 
	 * @param xxlJobLog
	 * @return
	 */
	public int updateHandleInfo(XxlJobLog xxlJobLog);

	/**
	 * 删除日志
	 * 
	 * @param jobId
	 * @return
	 */
	public int delete(@Param("jobId") int jobId);

	/**
	 * 通过handleCode查询日志的数量
	 * 
	 * @param handleCode
	 * @return
	 */
	public int triggerCountByHandleCode(@Param("handleCode") int handleCode);

	/**
	 * 时间段查询-按触发时间的天分组,统计触发总数、正在进行中的总数、成功总数
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Map<String, Object>> triggerCountByDay(@Param("from") Date from, @Param("to") Date to);

	/**
	 * 条件清除日志
	 * 
	 * @param jobGroup
	 *            执行器主键ID
	 * @param jobId
	 *            任务id
	 * @param clearBeforeTime
	 *            清理这个时间之前的
	 * @param clearBeforeNum
	 * @return
	 */
	public int clearLog(@Param("jobGroup") int jobGroup, @Param("jobId") int jobId,
			@Param("clearBeforeTime") Date clearBeforeTime, @Param("clearBeforeNum") int clearBeforeNum);

}
