package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.ilife.util.SxHelper;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.builder.outxml.NewsBuilder;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Binary Wang
 */
@Component
public class MsgHandler extends AbstractHandler {
  @Autowired
  private SxHelper helper;
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) throws WxErrorException {

    WeixinService weixinService = (WeixinService) wxMpService;
    
    // 获取微信用户基本信息
    String openid = "dummy";
    WxMpUser userWxInfo = weixinService.getUserService().userInfo(wxMessage.getFromUser(), null);
    if (userWxInfo != null) {
    		openid = userWxInfo.getOpenId();
    }

    if (!wxMessage.getMsgType().equals(WxConsts.XmlMsgType.EVENT)) {
      //TODO 可以选择将消息保存到本地
    }

    //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
    if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
      && weixinService.hasKefuOnline()) {
      return WxMpXmlOutMessage
        .TRANSFER_CUSTOMER_SERVICE().fromUser(wxMessage.getToUser())
        .toUser(wxMessage.getFromUser()).build();
    }
    
    //如果检测到内容里有淘口令，则提交数据到broker-seeds，并返回信息“正在查找对应的商品，请稍等”
    if(helper.isTaobaoToken(wxMessage.getContent())) {
    		//提交到broker-seed等待采集
    		helper.insertBrokerSeedByText(openid, wxMessage.getContent());
    		//发送消息告知不要着急，我正在找
	    try {
	      return new TextBuilder().build("收到淘口令，正在转换，请稍等哦亲~~", wxMessage, weixinService);
	    } catch (Exception e) {
	      this.logger.error(e.getMessage(), e);
	    }
    }

    
    //否则，根据关键词搜索符合内容
    String keyword = wxMessage.getContent();
    String xml = helper.loadDefaultItem();
    try {
    		xml = helper.searchMatchedItem(keyword);
    }catch(Exception ex) {
    		logger.error("Error occured while search items.[keyword]"+keyword,ex);
    }
    XStream xstream = new XStream();
    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
	WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
	return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
	        .toUser(wxMessage.getFromUser()).build();
  }


}
