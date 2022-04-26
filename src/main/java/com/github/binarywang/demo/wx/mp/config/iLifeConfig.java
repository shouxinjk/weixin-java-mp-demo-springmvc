package com.github.binarywang.demo.wx.mp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jdk.nashorn.internal.objects.annotations.Getter;

/**
 * @author Binary Wang
 */
@Configuration
public class iLifeConfig {
  @Value("#{ilifePropertyies.register_broker_url}")
  private String registerBrokerUrl;
  
  @Value("#{ilifePropertyies.disable_broker_url}")
  private String disableBrokerUrl;
  
  @Value("#{ilifePropertyies.update_broker_url}")
  private String updateBrokerUrl;
  
  @Value("#{ilifePropertyies.register_user_url}")
  private String registerUserUrl;
  
  @Value("#{ilifePropertyies.connect_user_url}")
  private String connectUserUrl;

  @Value("#{ilifePropertyies.data_api}")
  private String dataApi;
  
  @Value("#{ilifePropertyies.frontend_prefix}")
  private String frontendPrefix;

  @Value("#{ilifePropertyies.auto_register_broker}")
  private boolean autoRegisterBroker;
  
  @Value("#{ilifePropertyies.default_parent_broker_id}")
  private String defaultParentBrokerId;
  
  @Value("#{ilifePropertyies.default_parent_broker_openid}")
  private String defaultParentBrokerOpenid;
  
  @Value("#{ilifePropertyies.default_tech_guy_openid}")
  private String defaultTechGuyOpenid;
  
  @Value("#{ilifePropertyies.default_system_broker_openid}")
  private String defaultSystemBrokerOpenid;
  
  @Value("#{ilifePropertyies.sx_api}")
  private String sxApi;
  
  @Value("#{ilifePropertyies.template_subscribe}")
  private String msgIdSubscribe;
  
  @Value("#{ilifePropertyies.template_order}")
  private String msgIdOrder;
  
  @Value("#{ilifePropertyies.template_task}")
  private String msgIdTask;
  
  @Value("#{ilifePropertyies.template_guide}")
  private String msgIdGuide;
  
  @Value("#{ilifePropertyies.template_report}")
  private String msgIdReport;
  
  @Value("#{ilifePropertyies.template_broker}")
  private String msgIdBroker;
  
  @Value("#{ilifePropertyies.template_publisher}")
  private String msgIdPublisher;
  
  @Value("#{ilifePropertyies.template_connect}")
  private String msgIdConnect;
  
  @Value("#{ilifePropertyies.template_payment}")
  private String msgIdPayment;
  
  public String getSxApi() {
    return this.sxApi;
  }
  
  public String getDataApi() {
    return this.dataApi;
  }
  
  public String getDefaultSystemBrokerOpenid() {
	return defaultSystemBrokerOpenid;
}

public void setDefaultSystemBrokerOpenid(String defaultSystemBrokerOpenid) {
	this.defaultSystemBrokerOpenid = defaultSystemBrokerOpenid;
}

public String getMsgIdPayment() {
	return msgIdPayment;
}

public void setMsgIdPayment(String msgIdPayment) {
	this.msgIdPayment = msgIdPayment;
}

public String getDisableBrokerUrl() {
	return disableBrokerUrl;
}

public void setDisableBrokerUrl(String disableBrokerUrl) {
	this.disableBrokerUrl = disableBrokerUrl;
}

public boolean isAutoRegisterBroker() {
	return autoRegisterBroker;
}

public void setAutoRegisterBroker(boolean autoRegisterBroker) {
	this.autoRegisterBroker = autoRegisterBroker;
}

public String getFrontendPrefix() {
	return frontendPrefix;
}

public void setFrontendPrefix(String frontendPrefix) {
	this.frontendPrefix = frontendPrefix;
}

public String getDefaultParentBrokerId() {
	return defaultParentBrokerId;
}

public void setDefaultParentBrokerId(String defaultParentBrokerId) {
	this.defaultParentBrokerId = defaultParentBrokerId;
}

public String getDefaultParentBrokerOpenid() {
	return defaultParentBrokerOpenid;
}

public void setDefaultParentBrokerOpenid(String defaultParentBrokerOpenid) {
	this.defaultParentBrokerOpenid = defaultParentBrokerOpenid;
}

public String getDefaultTechGuyOpenid() {
	return defaultTechGuyOpenid;
}

public void setDefaultTechGuyOpenid(String defaultTechGuyOpenid) {
	this.defaultTechGuyOpenid = defaultTechGuyOpenid;
}

public String getConnectUserUrl() {
    return this.connectUserUrl;
  }
    
  public String getRegisterBrokerUrl() {
    return this.registerBrokerUrl;
  }
  
  public String getUpdateBrokerUrl() {
	    return this.updateBrokerUrl;
	  }

  public String getRegisterUserUrl() {
    return this.registerUserUrl;
  }

public String getMsgIdSubscribe() {
	return msgIdSubscribe;
}

public void setMsgIdSubscribe(String msgIdSubscribe) {
	this.msgIdSubscribe = msgIdSubscribe;
}

public String getMsgIdOrder() {
	return msgIdOrder;
}

public void setMsgIdOrder(String msgIdOrder) {
	this.msgIdOrder = msgIdOrder;
}

public String getMsgIdTask() {
	return msgIdTask;
}

public void setMsgIdTask(String msgIdTask) {
	this.msgIdTask = msgIdTask;
}

public String getMsgIdGuide() {
	return msgIdGuide;
}

public void setMsgIdGuide(String msgIdGuide) {
	this.msgIdGuide = msgIdGuide;
}

public String getMsgIdReport() {
	return msgIdReport;
}

public void setMsgIdReport(String msgIdReport) {
	this.msgIdReport = msgIdReport;
}

public String getMsgIdBroker() {
	return msgIdBroker;
}

public void setMsgIdBroker(String msgIdBroker) {
	this.msgIdBroker = msgIdBroker;
}

public String getMsgIdPublisher() {
	return msgIdPublisher;
}

public void setMsgIdPublisher(String msgIdPublisher) {
	this.msgIdPublisher = msgIdPublisher;
}

public String getMsgIdConnect() {
	return msgIdConnect;
}

public void setMsgIdConnect(String msgIdConnect) {
	this.msgIdConnect = msgIdConnect;
}

}
