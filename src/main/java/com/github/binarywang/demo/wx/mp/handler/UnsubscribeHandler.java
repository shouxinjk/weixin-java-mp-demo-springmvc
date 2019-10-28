package com.github.binarywang.demo.wx.mp.handler;

import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.service.WeixinService;

import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class UnsubscribeHandler extends AbstractHandler {

  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) {
	  WeixinService weixinService = (WeixinService) wxMpService;
	  
    String openId = wxMessage.getFromUser();
    this.logger.info("取消关注用户 OPENID: " + openId);
    // TODO 可以更新本地数据库为取消关注状态
    try {
	      return new TextBuilder().build("非常伤心你不再关注我们。\n\nLife is all about having a good time.\n\n我们的目标是成为您的私人生活助手，用全面的数据做出合理的消费决策，用小确幸丰富你的的大生活。 \n\n欢迎随时回来 ~~", wxMessage, weixinService);
	    } catch (Exception e) {
	      this.logger.error(e.getMessage(), e);
	    }
    return null;
  }

}
