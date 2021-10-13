package com.github.binarywang.demo.wx.mp.handler;

import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.helper.HttpClientHelper;
import com.github.binarywang.demo.wx.mp.service.WeixinService;

import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class UnsubscribeHandler extends AbstractHandler {
	  @Autowired
	  private iLifeConfig ilifeConfig;	
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) {
	  WeixinService weixinService = (WeixinService) wxMpService;
	  
    String openId = wxMessage.getFromUser();
    this.logger.info("取消关注用户 OPENID: " + openId);
    //检查是否是达人，如果是达人，则变更其状态为offline
    disableBroker(openId);
    try {
	      return new TextBuilder().build("\n\nLife is all about having a good time.\n\n我们致力于用小确幸填满你的的大生活。 \n\n欢迎随时回来 ~~", wxMessage, weixinService);
	    } catch (Exception e) {
	      this.logger.error(e.getMessage(), e);
	    }
    return null;
  }
  
  private void disableBroker(String openid) {
	  String url = ilifeConfig.getDisableBrokerUrl()+openid;//达人openid 
		JSONObject data = new JSONObject();
		data.put("status", "offline");//设置为offline，禁用
		JSONObject  result = HttpClientHelper.getInstance().post(url, data);
		logger.debug("disable broker done.",result);
  }

}
