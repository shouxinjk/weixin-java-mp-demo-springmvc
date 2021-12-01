package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arangodb.entity.BaseDocument;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.ilife.util.SxHelper;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
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
import org.springframework.beans.factory.annotation.Value;
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
	@Value("#{extProps['mp.msg.media.brokerGroupChat']}") String brokerGroupChatQrcodeMediaId;
	@Value("#{extProps['mp.msg.media.contact']}") String contactQrcodeMediaId;
	@Value("#{extProps['mp.msg.media.rootBroker']}") String rootBrokerQrcodeMediaId;
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

    String keyword = wxMessage.getContent();//获取消息内容作为关键词
    
    //不知道为啥会收到null字符串，如果为空则直接返回，不处理了
    if(keyword==null || keyword.trim().length()==0) {
    	return null;
    }
    
    //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
    if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
      && weixinService.hasKefuOnline()) {
      return WxMpXmlOutMessage
        .TRANSFER_CUSTOMER_SERVICE().fromUser(wxMessage.getToUser())
        .toUser(wxMessage.getFromUser()).build();
    }
    
    //处理逻辑：
    
    //TODO 匹配指令：需要特殊处理
    //显示达人群二维码:同时配置达人群菜单
    if("群聊  加群 达人群  聊天群 官方群 微信群 SX_GROUPCHAT".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(brokerGroupChatQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("请扫码加入群聊，和更多的生活家们一起交流分享~~", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }
    }
    //显示平台达人二维码，同时配置加入达人菜单
    if("加入 达人申请 达人注册 扫码 SX_JOIN".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(rootBrokerQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("小确幸大生活，选出好的，分享对的，扫码即可加入，等你哦~~", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }
    }
    //显示平台达人二维码，同时配置加入达人菜单
    if("咨询 了解 什么 ? 小确幸大生活 联系人 联系方式 问题 官方 公司 关于 SX_CONTACT".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(contactQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("关于小确幸大生活的任何问题，请扫码，我们随时解答~~", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }			
    }
    //匹配URL，仅对于已经支持的URL进行过滤，
    //如果在不支持的范围，则过滤掉URL，当成文字处理
    //如果再支持范围内，则转换为标准URL格式直接搜索标准URL，如果已经入库则直接返回
    String url = helper.getUrl(keyword);
    if(url.trim().length()>0) {
    	//检查URL是否在支持范围内
    	String targetUrl = helper.convertUrl(url);
    	if(targetUrl.trim().length()==0) {//是不支持的URL，看看还有没有其他内容可用
    		//把url信息从文本中去掉
    		keyword = keyword.replace(url, "").trim();
    		if(keyword.length()==0) {//如果没有其他内容了，直接返回吧，说不知道是个啥
    			return new TextBuilder().build("还不支持这个URL哈，重新输入看看呢。", wxMessage, weixinService);
    		}else {//表示还有其他内容，口令啊，文字之类的，等着后面处理就是了
    			//do nothing
    		}
    	}else {//是支持的URL，查找是否已经入库，已经入库则返回，否则等待采集后返回
    		String docXml = helper.queryDocByUrl(targetUrl);
    		if(docXml!=null) {//查询到了，直接返回指定内容
    		    XStream xstream = new XStream();
    		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
    			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(docXml);
    			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
    			        .toUser(wxMessage.getFromUser()).build();
    		}else {//提交到broker_seed库，等待采集。并发送 安抚消息
    			helper.insertBrokerSeed(openid,"url",targetUrl, wxMessage.getContent());
        		//先发个消息安抚一下
    		    try {
    		    	return new TextBuilder().build("商品URL收到，正在转换，请稍等~", wxMessage, weixinService);
    		    } catch (Exception e) {
    		    	this.logger.error(e.getMessage(), e);
    		    }
    		}
    	}
    }

    //匹配口令，当前支持淘口令 [a-zA-Z0-9]{11} 写入broker_seeds，等待采集入库，采集脚本将自动触发通知，并返回信息“正在查找对应的商品，请稍等”
    String token = helper.parseTaobaoToken(wxMessage.getContent());
    if(token != null) { //提交到broker-seed等待采集
    		helper.insertBrokerSeed(openid,"taobaoToken",token, wxMessage.getContent());
    		keyword = helper.getKeywordFromTaobaoToken(wxMessage.getContent());
    		//先发个消息安抚一下
		    try {
		    	return new TextBuilder().build("收到淘口令，正在转换，请稍等~", wxMessage, weixinService);
		    } catch (Exception e) {
		    	this.logger.error(e.getMessage(), e);
		    }
    }

    //如果keyword还有内容的话直接搜索，则根据关键词搜索符合内容
    String xml = null;
    try {
    		xml = helper.searchMatchedItem(keyword);
    }catch(Exception ex) {
    		logger.error("Error occured while search items.[keyword]"+keyword,ex);
    }
    if(xml == null || xml.trim().length() == 0)
    		xml = helper.loadDefaultItem();
    XStream xstream = new XStream();
    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
	WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
	return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
	        .toUser(wxMessage.getFromUser()).build();
  }


}
