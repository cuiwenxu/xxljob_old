package com.xxl.job.core.enums;

/**
 * 注册中心注册参数配置
 * Created by xuxueli on 17/5/10.
 */
public class RegistryConfig {
	
	//调用执行器的心跳检测时间:30s
    public static final int BEAT_TIMEOUT = 30;
    //超时时间
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    //EXECUTOR是执行器、ADMIN是调度中心
    public enum RegistType{ EXECUTOR, ADMIN }

}
