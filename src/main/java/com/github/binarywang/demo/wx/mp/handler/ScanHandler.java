package com.github.binarywang.demo.wx.mp.handler;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * @author Binary Wang
 */
public abstract class ScanHandler extends AbstractHandler {
	  private static final ObjectMapper JSON = new ObjectMapper();

	  static {
	    JSON.setSerializationInclusion(Include.NON_NULL);
	    JSON.configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE);
	  }

	  @Override
	  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
	                                  Map<String, Object> context, WxMpService wxMpService,
	                                  WxSessionManager sessionManager) {
	    try {
	      this.logger.info("\n\n========received scan msg.=========\n\n", JSON.writeValueAsString(wxMessage));
	    } catch (JsonProcessingException e) {
	      e.printStackTrace();
	    }

	    return null;
	  }
}
