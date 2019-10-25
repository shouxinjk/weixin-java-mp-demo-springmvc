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

}
