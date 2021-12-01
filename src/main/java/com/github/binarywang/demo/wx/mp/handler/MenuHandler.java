package com.github.binarywang.demo.wx.mp.handler;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class MenuHandler extends AbstractHandler {
	@Value("#{extProps['mp.msg.media.brokerGroupChat']}") String brokerGroupChatQrcodeMediaId;
	@Value("#{extProps['mp.msg.media.contact']}") String contactQrcodeMediaId;
	@Value("#{extProps['mp.msg.media.rootBroker']}") String rootBrokerQrcodeMediaId;
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) {
    String msg = String.format("type:%s, event:%s, key:%s",
      wxMessage.getMsgType(), wxMessage.getEvent(),
      wxMessage.getEventKey());
    if (WxConsts.MenuButtonType.VIEW.equals(wxMessage.getEvent())) {
      return null;
    }

    if("SX_JOIN".equalsIgnoreCase(wxMessage.getEventKey())) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(wxMessage.getFromUser())
			  .mediaId(rootBrokerQrcodeMediaId)
			  .build();
    	try {
  			wxMpService.getKefuService().sendKefuMessage(kfMsg);
    	}catch(Exception ex) {
    		//do nothing
    	}
	    return WxMpXmlOutMessage.TEXT().content("小确幸大生活，选出好的，分享对的，扫码即可加入，等你哦~~")
	  	      .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
	  	      .build();
    }else if("SX_GROUPCHAT".equalsIgnoreCase(wxMessage.getEventKey())) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
  			  .IMAGE()
  			  .toUser(wxMessage.getFromUser())
  			  .mediaId(brokerGroupChatQrcodeMediaId)
  			  .build();
    	try {
  			wxMpService.getKefuService().sendKefuMessage(kfMsg);
    	}catch(Exception ex) {
    		//do nothing
    	}
	    return WxMpXmlOutMessage.TEXT().content("请扫码加入群聊，和更多的生活家们一起交流分享~~")
	  	      .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
	  	      .build();
    }else if("SX_CONTACT".equalsIgnoreCase(wxMessage.getEventKey())) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(wxMessage.getFromUser())
			  .mediaId(contactQrcodeMediaId)
			  .build();
    	try {
  			wxMpService.getKefuService().sendKefuMessage(kfMsg);
    	}catch(Exception ex) {
    		//do nothing
    	}
	    return WxMpXmlOutMessage.TEXT().content("关于小确幸大生活的任何问题，请扫码，我们随时解答~~")
	  	      .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
	  	      .build();
    }else {//出错了，用这个兜底
	    return WxMpXmlOutMessage.TEXT().content(msg)
	      .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
	      .build();
    }
  }


}
