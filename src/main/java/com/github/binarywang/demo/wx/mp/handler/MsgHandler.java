package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arangodb.entity.BaseDocument;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.config.WxMpConfig;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.ilife.util.HttpClientHelper;
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
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
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
import java.text.SimpleDateFormat;
import java.util.Date;
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
    //如果涉及到退款、退钱、退费等，需要优先处理，并且人工介入
    if("退款 退钱 退费 骗子 欺骗".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(contactQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("非常抱歉，给亲带来麻烦了。如果是充值或付费遇到问题，请扫码添加，我们会收到了立即反馈的~~", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }
    }     
    if("充值 充钱 阅豆 购买 置顶".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(contactQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("对于有需求的流量主，我们提供了充值和广告服务。充值会增加豆豆，能够让公众号和文章在大厅排序更靠前。而置顶广告能够在指定时段把公众号或文章显示在顶部指定位置，便于更多人看到。充值和置顶都可以进入流量主后台直接完成，也欢迎扫码，我们邀请进群~~", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }
    }    
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
    if("客服 咨询 了解 什么 ? ？ 小确幸大生活 联系人 联系方式 问题 官方 公司 关于 SX_CONTACT".indexOf(keyword)>-1) {
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
    
    //匹配微信文章URL
     String pattern = "https://mp\\.weixin\\.qq\\.com/s[-a-zA-Z0-9+&@#/%?=~_|!:,.;]+";
	 try {
	     Pattern r = Pattern.compile(pattern);
	     Matcher m = r.matcher(keyword);
	     if (m.find()) {
	         logger.debug("\n\nmatch wechat article: " + m.group());
	         //创建微信文章：直接post即可
	     	String  xml = helper.publishArticle(userWxInfo.getOpenId(), userWxInfo.getNickname(), m.group());
	     	logger.debug("got article publish result.",xml);
	     	//返回卡片
	        XStream xstream = new XStream();
	        Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
	        XStream.setupDefaultSecurity(xstream);
	        xstream.allowTypes(classes);
	        xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
	    	WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
	    	return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
	    	        .toUser(wxMessage.getFromUser()).build();
	     }
	 }catch(Exception ex) {
	 	logger.error("Failed to match wechat article url.",ex);
	 }
	 
	 //CPS: pdd
	 //匹配拼多多链接，如果匹配则自动上架
	 /**
	 if(keyword.indexOf("yangkeduo.com")>0 || keyword.indexOf("pinduoduo.com")>0) {
		 JSONObject result = helper.checkPddUrl(keyword,wxMessage.getFromUser());
		 if(result.getBooleanValue("success")) {//是CPS商品则自动上架
			 JSONObject data = result.getJSONObject("data");
			String docXml = helper.item(data.getString("title"), 
					data.getString("summary"), 
					data.getString("logo"), 
					"https://www.biglistoflittlethings.com/ilife-web-wx/info2.html?id="+data.getString("itemKey"));
 		    XStream xstream = new XStream();
 		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
 		    XStream.setupDefaultSecurity(xstream);
 		    xstream.allowTypes(classes);
 		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
 			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(docXml);
 			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
 			        .toUser(wxMessage.getFromUser()).build();
		 }else if(result.getJSONObject("broker")!=null){//如果有对应达人，则发送上架通知，等候手动处理
			 //需要发送通知给管理员，告知手动采集：直接发送消息到企业微信即可。完成后需要通过微信后台回复。
			 helper.sendWeworkMsg("拼多多商品上架", "请求达人："+result.getJSONObject("broker").getString("nickname"), result.getJSONObject("broker").getString("avatarUrl"), keyword);
			 return new TextBuilder().build("请稍等，已转发客服，稍后回复~~", wxMessage, weixinService);
		 }else {
			 //do nothing
			 return new TextBuilder().build("啊哦，这个商品没在推广哦，看看其他的吧~~", wxMessage, weixinService);
		 }
	 }
	 //**/
	 
	 //CPS：taobao tmall fliggy 
	 //当前未实现接口，统一采用手动上架通知：通过下方对URL识别统一处理
	 /**
	 if(keyword.indexOf("taobao")>0 || keyword.indexOf("fliggy")>0 || keyword.indexOf("tmall")>0 || keyword.indexOf("tb.cn")>0 ||
			 keyword.indexOf("jd.cn")>0 || keyword.indexOf("jd.com")>0 ||
			 keyword.indexOf("vip.com")>0 || keyword.indexOf("vip.cn")>0 || 
			 keyword.indexOf("ly.com")>0 ||
			 keyword.indexOf("ctrip.com")>0 ||
			 keyword.indexOf("amazon.cn")>0 ||
			 keyword.indexOf("lvmama.com")>0 ||
			 keyword.indexOf("dangdang.com")>0 || 
			 keyword.indexOf("kaola.com")>0 || 
			 keyword.indexOf("vip.com")>0 || 
			 keyword.indexOf("163.com")>0 || 
			 keyword.indexOf("suning.com")>0 || 
			 keyword.indexOf("gome.com")>0 || 
			 keyword.indexOf("mi.com")>0 || 
			 keyword.indexOf("maoyan.com")>0 || 
			 keyword.indexOf("vip.com")>0 || 
			 keyword.indexOf("qunar.com")>0 || 
			 keyword.indexOf("mafengwo.cn")>0 || 
			 keyword.indexOf("1919.com")>0 || 
			 keyword.indexOf("biyao.com")>0 || 
			 keyword.indexOf("dhc.net.cn")>0 || 
			 keyword.indexOf("ikang.com")>0 || 
			 keyword.indexOf("zbird.com")>0
			 ) {
		 JSONObject result = helper.checkManualEnhouseUrl(keyword,wxMessage.getFromUser());
		 if(result.getBooleanValue("success")) {//是CPS商品则自动上架
			 JSONObject data = result.getJSONObject("data");
			String docXml = helper.item(data.getString("title"), 
					data.getString("summary"), 
					data.getString("logo"), 
					"https://www.biglistoflittlethings.com/ilife-web-wx/info2.html?id="+data.getString("itemKey"));
 		    XStream xstream = new XStream();
 		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
 		    XStream.setupDefaultSecurity(xstream);
 		    xstream.allowTypes(classes);
 		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
 			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(docXml);
 			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
 			        .toUser(wxMessage.getFromUser()).build();
		 }else if(result.getJSONObject("broker")!=null){//如果有对应达人，则发送上架通知，等候手动处理
			 //需要发送通知给管理员，告知手动采集：直接发送消息到企业微信即可。完成后需要通过微信后台回复。
			 helper.sendWeworkMsg("手动商品上架：" +keyword, "请求达人："+result.getJSONObject("broker").getString("nickname"), result.getJSONObject("broker").getString("avatarUrl"), keyword);
			 return new TextBuilder().build("请稍等，已转发客服，稍后回复。超过半小时未回复也可以直接联系客服哦~~", wxMessage, weixinService);
		 }else {
			 //do nothing
			 return new TextBuilder().build("啊哦，这个商品没在推广哦，看看其他的吧~~", wxMessage, weixinService);
		 }
	 }
	 //**/
    
    //匹配商品URL，仅对于已经支持的电商URL进行过滤，
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
    		String docXml = helper.queryDocByUrl(targetUrl);//注意：需要根据转换后的URL查找
    		if(docXml!=null) {//查询到了，直接返回指定内容
    		    XStream xstream = new XStream();
    		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
    		    XStream.setupDefaultSecurity(xstream);
    		    xstream.allowTypes(classes);
    		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
    			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(docXml);
    			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
    			        .toUser(wxMessage.getFromUser()).build();
    		}else {//提交到broker_seed库，等待采集。并发送 安抚消息
    	        //尝试自动采集、发送通知手动采集、或通知不予采集
    			 JSONObject result = helper.autoEnhouse(targetUrl, wxMessage.getContent(), wxMessage.getFromUser()); //注意要发送原始内容
    			 if(result.getBooleanValue("success")) {//是CPS商品则自动上架
    				 JSONObject data = result.getJSONObject("data");
    				 docXml = helper.item(data.getString("title"), 
    						data.getString("summary")==null?"新提交商品已上架":data.getString("summary"), 
    						data.getString("logo"), 
    						"https://www.biglistoflittlethings.com/ilife-web-wx/info2.html?id="+data.getString("itemKey"));
    	 		    XStream xstream = new XStream();
    	 		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
    	 		    XStream.setupDefaultSecurity(xstream);
    	 		    xstream.allowTypes(classes);
    	 		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
    	 			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(docXml);
    	 			helper.insertBrokerSeed(openid,"url",targetUrl, wxMessage.getContent(),true);//在种子库里写一条记录，但无需再发通知：另一种方式是此处不发送通知，等待自动任务完成。有一定的时延
    	 			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
    	 			        .toUser(wxMessage.getFromUser()).build();
    			 }else if(result.getJSONObject("broker")!=null){//如果有对应达人，则发送上架通知，等候手动处理
    				 //在convertUrl时已经判定过，此处不需要再次写入
    	    		 //helper.insertBrokerSeed(openid,"url",targetUrl, wxMessage.getContent());
    				 //需要发送通知给管理员，告知手动采集：直接发送消息到企业微信即可。完成后需要通过微信后台回复。
    				 helper.sendWeworkMsg("手动商品上架：" +keyword, "请求达人："+result.getJSONObject("broker").getString("nickname"), result.getJSONObject("broker").getString("avatarUrl"), keyword);
    				 return new TextBuilder().build("请稍等，已转发客服，稍后回复。超过半小时未回复请直接找客服哦~~", wxMessage, weixinService);
    			 }else {
    				 //do nothing
    				 return new TextBuilder().build("啊哦，这个商品没在推广哦，看看其他的吧~~", wxMessage, weixinService);
    			 }  
    		}
    	}
    }

    //匹配口令，当前支持淘口令 [a-zA-Z0-9]{11} 写入broker_seeds，等待采集入库，采集脚本将自动触发通知，并返回信息“正在查找对应的商品，请稍等”
    //已经不需要处理，在convertUrl过程中已经一并处理
    /**
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
    //**/
    
    //如果keyword还有内容的话直接搜索，则根据关键词搜索符合内容
    //先返回一条提示信息
    if(keyword.trim().length()<12) {//仅在关键字有限时才搜索
		WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .TEXT().content("找到 "+keyword+" 相关的内容，点击可以查看更多~~")
			  .toUser(userWxInfo.getOpenId())
			  .build();
		wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//然后返回一条搜索结果：微信限制只能返回一条
	    String xml = null;
	    try {
	    		xml = helper.searchMatchedItem(keyword);
	    }catch(Exception ex) {
	    		logger.error("Error occured while search items.[keyword]"+keyword,ex);
	    }
	    if(xml == null || xml.trim().length() == 0)
	    		xml = helper.loadDefaultItem();
	    XStream xstream = new XStream();
	    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
	    XStream.setupDefaultSecurity(xstream);
	    xstream.allowTypes(classes);
	    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
		WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
		return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
		        .toUser(wxMessage.getFromUser()).build();
	}
    
  	//最后返回不懂说啥，给出联系人方式
    return new TextBuilder().build("没听懂哦，输入关键字可以推荐好物哦~~", wxMessage, weixinService);
  
}
}
