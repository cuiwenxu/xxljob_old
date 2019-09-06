package com.xxl.job.core.thread;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.FileUtil;
import com.xxl.job.core.util.JacksonUtil;

/**
 * 回调调度中心接口线程 Created by xuxueli on 16/7/22.
 */
public class TriggerCallbackThread {

	private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

	/**
	 * 单例模式保证回调线程的唯一性
	 */
	private static TriggerCallbackThread instance = new TriggerCallbackThread();

	public static TriggerCallbackThread getInstance() {
		return instance;
	}

	/**
	 * job results callback queue 任务结果回调队列
	 */
	private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<HandleCallbackParam>();

	// 任务执行后，放入回调队列
	public static void pushCallBack(HandleCallbackParam callback) {
		getInstance().callBackQueue.add(callback);
		logger.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
	}

	/**
	 * callback thread
	 */
	private Thread triggerCallbackThread;// 回调线程,对回调队列中的进行回调
	private Thread triggerRetryCallbackThread;// 重试回调进程,对回调失败文件中的回调进行回调
	// 停止
	private volatile boolean toStop = false;

	/**
	 * 开始执行,不停的回调
	 */
	public void start() {

		// valid
		if (XxlJobExecutor.getAdminBizList() == null) {
			logger.warn(">>>>>>>>>>> xxl-job, executor callback config fail, adminAddresses is null.");
			return;
		}

		// callback
		triggerCallbackThread = new Thread(new Runnable() {
			@Override
			public void run() {

				// 正常的回调
				while (!toStop) {
					try {
						HandleCallbackParam callback = getInstance().callBackQueue.take();
						if (callback != null) {

							// callback list param
							List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
							// int drainToNum =
							// 移除此队列中所有可用的元素，并将它们添加到给定 callbackParamList 中
							getInstance().callBackQueue.drainTo(callbackParamList);
							callbackParamList.add(callback);

							// callback, will retry if error
							if (callbackParamList != null && callbackParamList.size() > 0) {
								doCallback(callbackParamList);
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}

				// last callback===停止后最后的回调
				try {
					List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
					// int drainToNum =
					// 移除此队列中所有可用的元素，并将它们添加到给定 callbackParamList 中
					getInstance().callBackQueue.drainTo(callbackParamList);
					if (callbackParamList != null && callbackParamList.size() > 0) {
						doCallback(callbackParamList);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				logger.info(">>>>>>>>>>> xxl-job, executor callback thread destory.");

			}
		});
		triggerCallbackThread.setDaemon(true);
		triggerCallbackThread.start();

		// 失败回调文件重试回调进程
		triggerRetryCallbackThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!toStop) {
					try {
						// 读取失败回调文件,重试进行回调操作
						retryFailCallbackFile();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					try {
						TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
					} catch (InterruptedException e) {
						logger.warn(">>>>>>>>>>> xxl-job, executor retry callback thread interrupted, error msg:{}",
								e.getMessage());
					}
				}
				logger.info(">>>>>>>>>>> xxl-job, executor retry callback thread destory.");
			}
		});
		triggerRetryCallbackThread.setDaemon(true);
		triggerRetryCallbackThread.start();

	}

	/**
	 * 停止回调进程
	 */
	public void toStop() {
		toStop = true;
		// stop callback, interrupt and wait
		triggerCallbackThread.interrupt();
		try {
			triggerCallbackThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}

		// stop retry, interrupt and wait
		triggerRetryCallbackThread.interrupt();
		try {
			triggerRetryCallbackThread.join();
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * do callback, will retry if error 回调操作
	 * 
	 * @param callbackParamList
	 */
	private void doCallback(List<HandleCallbackParam> callbackParamList) {
		boolean callbackRet = false;
		// callback, will retry if error
		// 获取调度中心的adminBiz列表，在执行器启动的时候，初始化的，
		for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
			try {
				// 这里的adminBiz 调用的callback方法，因为是通过NetComClientProxy 这个factoryBean创建的代理对象，
				// 在getObject方法中，最终是没有调用的目标类方法的invoke的。 只是将目标类的方法名，参数，类名，等信息发送给调度中心了
				// 发送的地址调度中心的接口地址是 ：“调度中心IP/api” 这个接口 。 这个是在执行器启动的时候初始化设置好的。
				// 调度中心的API接口拿到请求之后，通过参数里面的类名，方法，参数，反射出来一个对象，然后invoke， 最终将结果写入数据库
				ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
				if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
					callbackLog(callbackParamList, "<br>----------- xxl-job job callback finish.");
					// 因为调度中心是集群式的，所以只要有一台机器返回success，那么就算成功，直接break
					callbackRet = true;
					break;
				} else {
					// callback log 回调日志
					callbackLog(callbackParamList,
							"<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
				}
			} catch (Exception e) {
				// callback log 回调日志
				callbackLog(callbackParamList,
						"<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
			}
		}
		if (!callbackRet) {
			// 增加失败回调文件信息
			appendFailCallbackFile(callbackParamList);
		}
	}

	/**
	 * callback log 回调日志
	 */
	private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
		for (HandleCallbackParam callbackParam : callbackParamList) {
			String logFileName = XxlJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()),
					callbackParam.getLogId());
			XxlJobFileAppender.contextHolder.set(logFileName);
			XxlJobLogger.log(logContent);
		}
	}

	// ---------------------- fail-callback file ----------------------
	// 失败的回调文件地址
	private static String failCallbackFileName = XxlJobFileAppender.getLogPath().concat(File.separator)
			.concat("xxl-job-callback").concat(".log");

	/**
	 * 增加失败回调文件信息
	 * 
	 * @param callbackParamList
	 */
	private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
		// append file
		String content = JacksonUtil.writeValueAsString(callbackParamList);
		FileUtil.appendFileLine(failCallbackFileName, content);
	}

	/**
	 * 读取失败回调文件,重试进行回调操作
	 */
	private void retryFailCallbackFile() {
		// load and clear file
		List<String> fileLines = FileUtil.loadFileLines(failCallbackFileName);
		FileUtil.deleteFile(failCallbackFileName);

		// parse
		List<HandleCallbackParam> failCallbackParamList = new ArrayList<>();
		if (fileLines != null && fileLines.size() > 0) {
			for (String line : fileLines) {
				List<HandleCallbackParam> failCallbackParamListTmp = JacksonUtil.readValue(line, List.class,
						HandleCallbackParam.class);
				if (failCallbackParamListTmp != null && failCallbackParamListTmp.size() > 0) {
					failCallbackParamList.addAll(failCallbackParamListTmp);
				}
			}
		}

		// retry callback, 100 lines per page
		if (failCallbackParamList != null && failCallbackParamList.size() > 0) {
			int pagesize = 100;
			List<HandleCallbackParam> pageData = new ArrayList<>();
			for (int i = 0; i < failCallbackParamList.size(); i++) {
				pageData.add(failCallbackParamList.get(i));
				if (i > 0 && i % pagesize == 0) {
					doCallback(pageData);
					pageData.clear();
				}
			}
			if (pageData.size() > 0) {
				doCallback(pageData);
			}
		}
	}

}
