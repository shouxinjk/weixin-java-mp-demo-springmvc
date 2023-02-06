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

import org.apache.commons.lang3.StringEscapeUtils;
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
    if("你好 你是谁 你叫什么".indexOf(keyword)>-1) {
    	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(contactQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
		//回复文字消息
	    try {
	    	return new TextBuilder().build("我是你的生活助手，能提供推荐、评价、定制服务，让每一个生活决策都带来小确幸，填满你的大生活。可以输入关键字查找也可以随意聊天的哦😊😊", wxMessage, weixinService);
	    } catch (Exception e) {
	    	this.logger.error(e.getMessage(), e);
	    }
    }     
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
    
    //匹配商品URL，仅对于已经支持的电商URL进行过滤，
    //如果在不支持的范围，则过滤掉URL，当成文字处理
    //如果再支持范围内，则转换为标准URL格式直接搜索标准URL，如果已经入库则直接返回
    String url = helper.getUrl(keyword);//判断是否是URL地址
    if(url.trim().length()>0) {
    	String sUrl = StringEscapeUtils.escapeHtml4(keyword); //!!!注意：判断包含URL后需要采用原文处理，以支持淘口令，避免直接采用url。需要进行转义处理
    	//检查URL是否在支持范围内
    	String targetUrl = helper.convertUrl(sUrl);
    	if(targetUrl.trim().length()==0) {//是不支持的URL，看看还有没有其他内容可用
    		//把url信息从文本中去掉
    		keyword = keyword.replace(url, "").trim();
    		if(keyword.length()==0) {//如果没有其他内容了，直接返回吧，说不知道是个啥
    			return new TextBuilder().build("还不支持这个URL哈，可以支持京东、拼多多、唯品会、淘宝~~", wxMessage, weixinService);
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
    
    /**
    //商品搜索：
    //如果keyword还有内容的话直接搜索，则根据关键词搜索符合内容
    //先返回一条提示信息
    if((keyword.indexOf("查找")>-1 || keyword.indexOf("商品")>-1)&&keyword.trim().length()<12) {//仅在关键字有限时才搜索
    	keyword = keyword.replace("查找", "").replace("商品", "");
    	String tips = "";
    	if("*".equalsIgnoreCase(keyword)) {
    		keyword = "*";
    		tips = "可以输入关键字查找商品哦，也可以进入查看更多~~";
    	}else {
    		tips = "找到 "+keyword+" 相关的商品，点击可以查看更多~~";
    	}
		//然后返回一条搜索结果：微信限制只能返回一条
	    String xml = null;
	    try {
	    		xml = helper.searchMatchedItem(keyword);
	    }catch(Exception ex) {
	    		logger.error("Error occured while search items.[keyword]"+keyword,ex);
	    }
	    if(xml != null && xml.trim().length() > 0){
	    	//先发送客服消息
			WxMpKefuMessage kfMsg = WxMpKefuMessage
					  .TEXT().content(tips)
					  .toUser(userWxInfo.getOpenId())
					  .build();
				wxMpService.getKefuService().sendKefuMessage(kfMsg);
				
			//然后返回找到的商品图文
		    XStream xstream = new XStream();
		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
		    XStream.setupDefaultSecurity(xstream);
		    xstream.allowTypes(classes);
		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
			        .toUser(wxMessage.getFromUser()).build();
	    }
	}
    //**/
    
    //清单、方案、排行榜搜索：
    //如果keyword还有内容的话直接搜索，则根据关键词搜索符合内容
    //先返回一条提示信息
    String[] articleMagicWords = {"清单","集合","列表","方案","个性化","定制","排行"};//类型识别词
    String[] articleTypes = {"主题清单","主题清单","主题清单","定制方案","定制方案","定制方案","排行榜"};//与识别词一一对应
    String matchedArticleTag = "";
    String matchedAttcleType = "内容";
    int idx = 0;
    for(String token:articleMagicWords) {
    	if(keyword.indexOf(token)>-1) {//找到了就返回
    		matchedArticleTag = token;
    		matchedAttcleType = articleTypes[idx];
    		break;
    	}
    	idx++;
    }
    if(matchedArticleTag.trim().length()>0) {//需要触发特定关键词
    	String bearKeyword = keyword.replace(matchedArticleTag, "").trim();
    	if(bearKeyword.length()==0)
    		bearKeyword = "*";
    	String tips = "";
    	if("*".equalsIgnoreCase(bearKeyword)) {
    		tips = "清单、方案、排行榜有很多的哦，加个关键词可以更准哦😉";
    	}else {
    		tips = "好安逸，找到相关的"+matchedAttcleType+"🤩，赶紧看哦~~";
    	}
		//然后返回一条搜索结果：微信限制只能返回一条
	    String xml = null;
	    try {
	    		xml = helper.searchMatchedArticle(bearKeyword);
	    }catch(Exception ex) {
	    		logger.error("Error occured while search articles.[keyword]"+keyword,ex);
	    }
	    if(xml != null && xml.trim().length() > 0) {
	    	//发送一条客服消息
			WxMpKefuMessage kfMsg = WxMpKefuMessage
					  .TEXT().content(tips)
					  .toUser(userWxInfo.getOpenId())
					  .build();
				wxMpService.getKefuService().sendKefuMessage(kfMsg);
	    	//返回找到的内容
		    XStream xstream = new XStream();
		    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
		    XStream.setupDefaultSecurity(xstream);
		    xstream.allowTypes(classes);
		    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
			WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
			return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
			        .toUser(wxMessage.getFromUser()).build();
	    }
	}    

	//搜索商品或内容，返回得分更高的结果：微信限制只能返回一条
    String xml = null;
    String[] kfMsgTpl = {
    		"正在查找__keyword相关的内容，请稍等一下下哦😊😊",
    		"找到__keyword相关的内容🥰，点击查看哦~~",
    		"哇塞🤩，我找到你要的__keyword了，赶紧查看吧~~",
    		"众里寻他千百度，__keyword就在灯火阑珊处😉",
    };
    try {
    		xml = helper.searchContent(keyword);
    }catch(Exception ex) {
    		logger.error("Error occured while search content.[keyword]"+keyword,ex);
    }
    if(xml != null && xml.trim().length() > 0){
    	//先发送客服消息
    	//随机选一条回复语
    	int random = (int)Math.floor(Math.random()*100)%kfMsgTpl.length;
		WxMpKefuMessage kfMsg = WxMpKefuMessage
				  .TEXT().content(kfMsgTpl[random].replace("__keyword", keyword))
				  .toUser(userWxInfo.getOpenId())
				  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
			
		//然后返回找到的商品图文
	    XStream xstream = new XStream();
	    Class<?>[] classes = new Class[] { WxMpXmlOutNewsMessage.Item.class };
	    XStream.setupDefaultSecurity(xstream);
	    xstream.allowTypes(classes);
	    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
		WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
		return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
		        .toUser(wxMessage.getFromUser()).build();
    }
    
    //如果都没有则由ChatGPT回答
    String answer = "";
	//chatgpt比较慢，先回复一条消息
    String[] chatGptMsgTpl = {
    		"让我想想哈，稍等一下下哦😊😊",
    		"有点忙不过来了哦，要稍等等哦~~",
    		"正在全力思考中😉",
    };
	int random = (int)Math.floor(Math.random()*100)%chatGptMsgTpl.length;
	WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .TEXT().content(chatGptMsgTpl[random].replace("__keyword", keyword))
			  .toUser(userWxInfo.getOpenId())
			  .build();
		wxMpService.getKefuService().sendKefuMessage(kfMsg);
	//请求chatgpt
    try {
    	answer = helper.requestChatGPT(keyword);
    	if(answer!=null&&answer.trim().length()>0) {
    		//等待时间过长无法直接返回，采用客服消息
    	    //return new TextBuilder().build(answer, wxMessage, weixinService);
    		kfMsg = WxMpKefuMessage
    				  .TEXT().content(answer)
    				  .toUser(userWxInfo.getOpenId())
    				  .build();
    			wxMpService.getKefuService().sendKefuMessage(kfMsg);
    	}
    }catch(Exception ex) {
    	logger.error("Error occured while access chatgpt.[keyword]"+keyword,ex);
    }
    
  	//最后返回不懂说啥，给出联系人方式
    return new TextBuilder().build("可以输入清单、方案、商品、排行榜等内容直接查找，也可以直接进入菜单哦~~", wxMessage, weixinService);
  
}
}
