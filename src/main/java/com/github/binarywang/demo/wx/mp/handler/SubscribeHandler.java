package com.github.binarywang.demo.wx.mp.handler;

import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class SubscribeHandler extends AbstractHandler {

  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) throws WxErrorException {

    this.logger.info("新关注用户 OPENID: " + wxMessage.getFromUser());

    WeixinService weixinService = (WeixinService) wxMpService;

    // 获取微信用户基本信息
    WxMpUser userWxInfo = weixinService.getUserService().userInfo(wxMessage.getFromUser(), null);

    if (userWxInfo != null) {
      // TODO 可以添加关注用户到本地
    }

    WxMpXmlOutMessage responseResult = null;
    try {
      responseResult = handleSpecial(wxMessage);
    } catch (Exception e) {
      this.logger.error(e.getMessage(), e);
    }

    if (responseResult != null) {
      return responseResult;
    }

    if(userWxInfo.getQrSceneStr().trim().length()>0) {//如果是扫描上级达人二维码关注，则发送模板消息完善达人信息
    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
        	      .toUser(userWxInfo.getOpenId())
        	      .templateId("oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk")
        	      .url("http://www.biglistoflittlethings.com/list/")
        	      .build();

        	    templateMessage.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，您已成功注册达人"))
        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
        	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
        	    		.addData(new WxMpTemplateData("keyword3", "待完善","ff0000"))
        	    		.addData(new WxMpTemplateData("remark", "感谢注册，还有几个基本信息需要填写，请点击完善"));
        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(templateMessage);    	
    }else {//如果是不带参数扫描则作为用户反馈信息：
	    try {
	      return new TextBuilder().build("感谢关注。我们用小确幸充满你的大生活。", wxMessage, weixinService);
	    } catch (Exception e) {
	      this.logger.error(e.getMessage(), e);
	    }
    }

    return null;
  }

  /**
   * 处理特殊请求，比如如果是扫码进来的，可以做相应处理
   */
  protected WxMpXmlOutMessage handleSpecial(WxMpXmlMessage wxMessage) throws Exception {
    //TODO
    return null;
  }

}
