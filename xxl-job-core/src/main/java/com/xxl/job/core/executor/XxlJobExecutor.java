package com.xxl.job.core.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.call.CallType;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.net.NetEnum;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.serialize.Serializer;
import com.xxl.rpc.util.IpUtil;
import com.xxl.rpc.util.NetUtil;

/**
 * 父执行器,最终都是这个启动
 */
public class XxlJobExecutor {

	private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

	// ---------------------- param 参数 TODO----------------------
	private String adminAddresses;// 调度中心地址
	private String appName;// 执行器名称
	private String ip;// 执行器ip地址
	private int port;// 执行器端口号
	private String accessToken;
	private String logPath;
	private int logRetentionDays;

	public void setAdminAddresses(String adminAddresses) {
		this.adminAddresses = adminAddresses;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public void setLogRetentionDays(int logRetentionDays) {
		this.logRetentionDays = logRetentionDays;
	}

	// ---------------------- start + stop ----TODO------------------
	/**
	 * 初始化
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {

		// 初始化log日志
		XxlJobFileAppender.initLogPath(logPath);

		// init admin-client
		// 初始化调用调度中心的client列表
		initAdminBizList(adminAddresses, accessToken);

		// init JobLogFileCleanThread
		// 初始化日志文件清理线程
		JobLogFileCleanThread.getInstance().start(logRetentionDays);

		// init TriggerCallbackThread
		// 初始化触发器回调线程(用RPC回调调度中心接口)
		TriggerCallbackThread.getInstance().start();

		// init executor-server
		// 初始化执行器服务
		port = port > 0 ? port : NetUtil.findAvailablePort(9999);
		// 获取ip
		ip = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();
		initRpcProvider(ip, port, appName, accessToken);
	}

	/**
	 * 注销
	 */
	public void destroy() {
		// destory jobThreadRepository
		if (jobThreadRepository.size() > 0) {
			for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
				removeJobThread(item.getKey(), "web container destroy and kill the job.");
			}
			jobThreadRepository.clear();
		}

		// destory JobLogFileCleanThread
		JobLogFileCleanThread.getInstance().toStop();

		// destory TriggerCallbackThread
		TriggerCallbackThread.getInstance().toStop();

		// destory executor-server
		stopRpcProvider();
	}

	// -------- admin-client (rpc invoker) --------调度中心client(用这个去调用调度中心的接口)--------TODO---------------
	// 调度中心client列表
	private static List<AdminBiz> adminBizList;

	/**
	 * 初始化调度中心
	 * 
	 * @param adminAddresses
	 *            xxl.job.admin.addresses
	 * @param accessToken
	 *            xxl.job.accessToken
	 * @throws Exception
	 */
	private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
		if (adminAddresses != null && adminAddresses.trim().length() > 0) {
			for (String address : adminAddresses.trim().split(",")) {
				if (address != null && address.trim().length() > 0) {

					String addressUrl = address.concat(AdminBiz.MAPPING);

					// 获取调度中心的AdminBiz,用这个类可以调用到调度中心的RPC接口
					AdminBiz adminBiz = (AdminBiz) new XxlRpcReferenceBean(NetEnum.JETTY,
							Serializer.SerializeEnum.HESSIAN.getSerializer(), CallType.SYNC, AdminBiz.class, null,
							10000, addressUrl, accessToken, null).getObject();

					if (adminBizList == null) {
						adminBizList = new ArrayList<AdminBiz>();
					}
					adminBizList.add(adminBiz);
				}
			}
		}
	}

	/**
	 * 获取调度中心client
	 * 
	 * @return
	 */
	public static List<AdminBiz> getAdminBizList() {
		return adminBizList;
	}

	/**
	 * ---------------------------------------------------------------------------------------------------------------
	 * -------------executor-server (rpc provider) 执行器服务(RPC的服务提供者,被调用中心调用)--TODO------------------------
	 * ---------------------------------------------------------------------------------------------------------------
	 */
	// invoker factory
	private XxlRpcInvokerFactory xxlRpcInvokerFactory = null;
	// RpcProviderRPC接口提供者
	private XxlRpcProviderFactory xxlRpcProviderFactory = null;

	/**
	 * 初始化RpcProvider----- TODO ----<br/>
	 * (与调度中心不同，这边内嵌了一个jetty服务器<br/>
	 * 有一个web端口server.port，一个RPC接口端口xxl.job.executor.port)
	 * 
	 * @param ip
	 * @param port
	 * @param appName
	 * @param accessToken
	 * @throws Exception
	 */
	private void initRpcProvider(String ip, int port, String appName, String accessToken) throws Exception {
		// init invoker factory
		xxlRpcInvokerFactory = new XxlRpcInvokerFactory();

		// init, provider factory 初始化提供者工厂
		String address = IpUtil.getIpPort(ip, port);
		Map<String, String> serviceRegistryParam = new HashMap<String, String>();
		serviceRegistryParam.put("appName", appName);
		serviceRegistryParam.put("address", address);
		// 初始化提供者工厂
		xxlRpcProviderFactory = new XxlRpcProviderFactory();
		// 最后两个参数与执行器注册线程的相关 TODO
		xxlRpcProviderFactory.initConfig(NetEnum.JETTY, Serializer.SerializeEnum.HESSIAN.getSerializer(), ip, port,
				accessToken, ExecutorServiceRegistry.class, serviceRegistryParam);

		// add services 增加服务接口和服务实现,供给调用中心调用
		xxlRpcProviderFactory.addService(ExecutorBiz.class.getName(), null, new ExecutorBizImpl());

		// RpcProvider工厂启动:
		// 1.执行器注册线程serviceRegistry启动
		// 2.内置jetty service启动
		xxlRpcProviderFactory.start();

	}

	/**
	 * 执行器远程注册到调度中心()
	 * 
	 * @author duhai
	 * @date 2019年3月20日
	 * @see
	 * @since JDK 1.8.0
	 */
	public static class ExecutorServiceRegistry extends ServiceRegistry {

		/**
		 * xxlRpcProviderFactory.start();内部启用:远程注册到调度中心
		 * 
		 * @see com.xxl.rpc.registry.ServiceRegistry#start(java.util.Map)
		 */
		@Override
		public void start(Map<String, String> param) {
			// start registry:开始执行器注册线程
			ExecutorRegistryThread.getInstance().start(param.get("appName"), param.get("address"));
		}

		/**
		 * 停止注册到调度中心
		 * 
		 * @see com.xxl.rpc.registry.ServiceRegistry#stop()
		 */
		@Override
		public void stop() {
			// stop registry
			ExecutorRegistryThread.getInstance().toStop();
		}

		@Override
		public boolean registry(String key, String value) {
			return false;
		}

		@Override
		public boolean remove(String key, String value) {
			return false;
		}

		@Override
		public TreeSet<String> discovery(String key) {
			return null;
		}

	}

	// 停止RpcProvider
	private void stopRpcProvider() {
		// stop invoker factory
		try {
			xxlRpcInvokerFactory.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		// stop provider factory
		try {
			xxlRpcProviderFactory.stop();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	// ---------------------- job handler repository ----TODO------------------
	/**
	 * 执行器中的jobHandler存储的Map,调度中心调用执行器的时候就从这个里面找jobHandler然后再用反射来调用execute方法
	 */
	private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();

	/**
	 * 注册JobHandler到jobHandlerRepository
	 * 
	 * @param name
	 * @param jobHandler
	 * @return
	 */
	public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
		logger.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
		return jobHandlerRepository.put(name, jobHandler);
	}

	/**
	 * 查询jobHandlerRepository里面是否有该名字的JobHandler
	 * 
	 * @param name
	 * @return
	 */
	public static IJobHandler loadJobHandler(String name) {
		return jobHandlerRepository.get(name);
	}

	// ---------------------- job thread repository ---TODO-------------------
	// job线程工厂(每一个jobId一个线程)
	private static ConcurrentHashMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();

	/**
	 * 注册新的job线程,关闭老的job线程
	 * 
	 * @param jobId
	 * @param handler
	 * @param removeOldReason
	 * @return
	 */
	public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason) {
		JobThread newJobThread = new JobThread(jobId, handler);
		newJobThread.start();
		logger.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}",
				new Object[] { jobId, handler });

		// putIfAbsent | oh my god, map's put method return the old value!!!
		// put进去新的,把老的job线程返回
		JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);

		if (oldJobThread != null) {
			oldJobThread.toStop(removeOldReason);
			oldJobThread.interrupt();
		}

		return newJobThread;
	}

	/**
	 * 关闭老的job线程
	 * 
	 * @param jobId
	 * @param removeOldReason
	 */
	public static void removeJobThread(int jobId, String removeOldReason) {
		JobThread oldJobThread = jobThreadRepository.remove(jobId);
		if (oldJobThread != null) {
			oldJobThread.toStop(removeOldReason);
			oldJobThread.interrupt();
		}
	}

	/**
	 * 该jobId是否有正在进行中的线程
	 * 
	 * @param jobId
	 * @return
	 */
	public static JobThread loadJobThread(int jobId) {
		JobThread jobThread = jobThreadRepository.get(jobId);
		return jobThread;
	}

}
