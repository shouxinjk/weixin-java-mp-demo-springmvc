package com.github.binarywang.demo.wx.mp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Binary Wang
 */
@Configuration
public class iLifeConfig {
  @Value("#{ilifePropertyies.register_broker_url}")
  private String registerBrokerUrl;

  @Value("#{ilifePropertyies.update_broker_url}")
  private String updateBrokerUrl;
  
  @Value("#{ilifePropertyies.register_user_url}")
  private String registerUserUrl;
  
  @Value("#{ilifePropertyies.connect_user_url}")
  private String connectUserUrl;

  @Value("#{ilifePropertyies.data_api}")
  private String dataApi;

  @Value("#{ilifePropertyies.sx_api}")
  private String sxApi;
  
  @Value("#{ilifePropertyies.template_subscribe}")
  private String msgIdSubscribe;
  @Value("#{ilifePropertyies.template_order}")
  private String msgIdOrder;
  @Value("#{ilifePropertyies.template_task}")
  private String msgIdTask;
  @Value("#{ilifePropertyies.template_report}")
  private String msgIdReport;
  @Value("#{ilifePropertyies.template_broker}")
  private String msgIdBroker;
  @Value("#{ilifePropertyies.template_connect}")
  private String msgIdConnect;
  
  public String getSxApi() {
    return this.sxApi;
  }
  
  public String getDataApi() {
    return this.dataApi;
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

public String getMsgIdConnect() {
	return msgIdConnect;
}

public void setMsgIdConnect(String msgIdConnect) {
	this.msgIdConnect = msgIdConnect;
}

}
