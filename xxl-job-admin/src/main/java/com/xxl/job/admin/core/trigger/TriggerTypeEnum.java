package com.xxl.job.admin.core.trigger;

import com.xxl.job.admin.core.util.I18nUtil;

/**
 * trigger type enum
 * 触发器类型
 * @author xuxueli 2018-09-16 04:56:41
 */
public enum TriggerTypeEnum {
	
	//手工的
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    //cron定时任务的
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    //重试的
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    API(I18nUtil.getString("jobconf_trigger_type_api"));

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }

}
