package com.xxl.job.adminbiz;

import org.junit.Assert;
import org.junit.Test;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.rpc.remoting.invoker.call.CallType;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.net.NetEnum;
import com.xxl.rpc.serialize.Serializer;

/**
 * admin api test 模拟调用注册中心api的测试
 *
 * @author xuxueli 2017-07-28 22:14:52
 */
public class AdminBizTest {

	// admin-client
	private static String addressUrl = "http://127.0.0.1:8080/xxl-job-admin".concat(AdminBiz.MAPPING);
	private static String accessToken = null;

	/**
	 * registry executor 模拟执行器在注册中心上面进行注册
	 * 
	 * @throws Exception
	 */
	@Test
	public void registryTest() throws Exception {
		addressUrl = addressUrl.replace("http://", "");

		AdminBiz adminBiz = (AdminBiz) new XxlRpcReferenceBean(NetEnum.JETTY,
				Serializer.SerializeEnum.HESSIAN.getSerializer(), CallType.SYNC, AdminBiz.class, null, 10000,
				addressUrl, accessToken, null).getObject();

		// test executor registry
		RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(),
				"xxl-job-executor-example", "127.0.0.1:9999");

		ReturnT<String> returnT = adminBiz.registry(registryParam);
		Assert.assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
	}

	/**
	 * registry executor remove 模拟执行器在注册中心上面删除注册
	 * 
	 * @throws Exception
	 */
	@Test
	public void registryRemove() throws Exception {
		addressUrl = addressUrl.replace("http://", "");

		AdminBiz adminBiz = (AdminBiz) new XxlRpcReferenceBean(NetEnum.JETTY,
				Serializer.SerializeEnum.HESSIAN.getSerializer(), CallType.SYNC, AdminBiz.class, null, 10000,
				addressUrl, accessToken, null).getObject();

		// test executor registry remove
		RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(),
				"xxl-job-executor-example", "127.0.0.1:9999");

		ReturnT<String> returnT = adminBiz.registryRemove(registryParam);
		Assert.assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
	}

}
