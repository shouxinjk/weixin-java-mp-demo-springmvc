package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.helper.HttpClientHelper;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.google.common.collect.Maps;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class SubscribeHandler extends AbstractHandler {
	  @Autowired
	  private iLifeConfig ilifeConfig;	
	  
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) throws WxErrorException {

    this.logger.info("新关注用户 OPENID: " + wxMessage.getFromUser());

    WeixinService weixinService = (WeixinService) wxMpService;

    // 获取微信用户基本信息
    WxMpUser userWxInfo = weixinService.getUserService().userInfo(wxMessage.getFromUser(), null);


    //准备发起HTTP请求：设置data server Authorization
    Map<String,String> header = new HashMap<String,String>();
    header.put("Authorization","Basic aWxpZmU6aWxpZmU=");
    JSONObject result = null;
    if (userWxInfo != null) {
    		result = HttpClientHelper.getInstance().get(ilifeConfig.getDataApi()+"/_api/document/user_users/"+userWxInfo.getOpenId(),null, header);
    		if(result!=null && result.getString("_id")!=null) {//如果查到则表示用户已经建立，不做处理
    			//do nothing
    		}else {//把微信用户薅到本地，自己先留着
			JSONObject user = JSONObject.parseObject(userWxInfo.toString());
			user.put("_key", userWxInfo.getOpenId());//重要：使用openId作为key
			result = HttpClientHelper.getInstance().post(ilifeConfig.getRegisterUserUrl(), user,header);    
		}	
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

    
    if(userWxInfo.getQrSceneStr().trim().length()>0) {
    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    		String[] params = userWxInfo.getQrSceneStr().trim().split("::");//场景值由两部分组成。TYPE::ID。其中Type为User 或Broker，ID为openId或brokerId
    		if(params.length<2) {//如果无识别标识，不做任何处理
    			logger.error("Wrong scene str.[str]"+userWxInfo.getQrSceneStr());
    		}else if("User".equalsIgnoreCase(params[0])) {//如果是用户邀请则发送。User::openId
    			if(result!=null && result.getString("_id")!=null) {//成功创建则继续创建关联关系
        			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
        			JSONObject example = new JSONObject();
        			example.put("_from", "user_users/"+params[1]);
        			example.put("_to", "user_users/"+userWxInfo.getOpenId());
        			JSONObject data = new JSONObject();
        			data.put("collection", "connections");
        			data.put("example", example);
        			result = HttpClientHelper.getInstance().put(ilifeConfig.getDataApi()+"/_api/simple/by-example", data, header);
        			if(result!=null && result.getIntValue("count")>0) {//该关联已经存在。不做任何处理。
        				//能到这里，说明这货之前已经加了好友了，但是又取消关注了。发个消息提示一下就可以了
        				return new TextBuilder().build("已经添加为关心的人了哦，点击【我】然后点击【关心的人】看看吧~~", wxMessage, weixinService);
        			}else {//建立用户关联：
	    				//建立双向用户关联：
	    				JSONObject conn = new JSONObject();
	    				//将推荐者加为当前用户好友
	    				conn.put("_from", "user_users/"+userWxInfo.getOpenId());//端是新加入的用户
	    				conn.put("_to", "user_users/"+params[1]);//源是推荐者
	    				conn.put("name", "关心我的TA");//关系名称
	    				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
	    				//将当前用户加为推荐者好友
	    				conn.put("_from", "user_users/"+params[1]);//源是推荐者
	    				conn.put("_to", "user_users/"+userWxInfo.getOpenId());//端是新加入的用户
	    				conn.put("name", "我关心的TA");//关系名称
	    				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
	    				if(result !=null && result.get("_id") !=null) {//成功建立关联。可以发送通知了
		    		    		//发送消息给推荐用户，让他感觉开心点
	    					/**
							{{first.DATA}}
							加入时间：{{keyword1.DATA}}
							亲友姓名：{{keyword2.DATA}}
							{{remark.DATA}}
	    					 */
		    	    	        WxMpTemplateMessage msg = WxMpTemplateMessage.builder()
		    	    	        	      .toUser(params[1])//推荐者openId
		    	    	        	      .templateId(ilifeConfig.getMsgIdConnect())//FL1WVQzCmL5_1bOsPlu5QV_mdeeZJv6WO57pQ5FGjnA
		    	    	        	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/connection.html")//跳转到好友查看界面
		    	    	        	      .build();
		    	    	
		    	    	        msg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+" 已添加你为关心的人"))
		    	    	        	    		.addData(new WxMpTemplateData("keyword2", userWxInfo.getNickname()))
		    	    	        	    		.addData(new WxMpTemplateData("keyword1", dateFormatLong.format(new Date())))
		    	    	        	    		.addData(new WxMpTemplateData("remark", "可以进入【大生活】查看特定于TA的推荐，也可以进入【关心的人】查看并进行设置获得更好的推荐结果哦~~ \n\nEnjoy ~~"));
		    	    	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(msg);       					
	    				}
        			}
    			}

	    		//发送消息给新注册用户，引导完成设置个人设置
    			/**
				{{first.DATA}}
				会员昵称：{{keyword1.DATA}}
				注册时间：{{keyword2.DATA}}
				{{remark.DATA}}
    			 */
    	        WxMpTemplateMessage welcomeMsg = WxMpTemplateMessage.builder()
    	        	      .toUser(userWxInfo.getOpenId())
    	        	      .templateId(ilifeConfig.getMsgIdBroker())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
    	        	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/user-register.html?fromUserOpenId="+userWxInfo.getOpenId()+"&toUserOpenId="+params[1])
    	        	      .build();
    	
    	        welcomeMsg.addData(new WxMpTemplateData("first", "成功添加你关心的人"))
    	        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
    	        	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
    	        	    		.addData(new WxMpTemplateData("remark", "可以进入【大生活】查看特定于TA的推荐，也可以进入【关心的人】查看并进行设置获得更好的推荐结果哦~~ \n\nEnjoy ~~"));
    	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);      			
    		}else if("Broker".equalsIgnoreCase(params[0])) {//如果是扫描上级达人二维码关注，则发送模板消息完善达人信息。Broker::brokerId
    			//根据openId查找是否已经注册达人
    			result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerByOpenid/"+userWxInfo.getOpenId(),null, header);
    			if(result!=null && result.getBooleanValue("status")) {//特殊情况：已经注册打人后取消关注，再次扫码关注时还是保留原来的达人信息，不另外新建记录
    				//能到这里，说明这货之前已经加入达人，但是又取消关注了。发个消息提示一下就可以了
    				return new TextBuilder().build("已经注册达人了哦，自购省钱，分享赚钱，赶紧点击【我】然后点击【进入达人后台】看看吧~~", wxMessage, weixinService);
    			}else {//如果不是达人，则完成注册
	    			//注册新达人。并建立新达人与上级达人的关联
	    			//String url = "http://localhost:8080/iLife/a/mod/broker/rest/"+params[1];
	    			String url = ilifeConfig.getRegisterBrokerUrl()+params[1];//针对上级达人创建
	    			JSONObject data = new JSONObject();
	    			data.put("hierarchy", "9");
	    			data.put("level", "推广达人");
	    			data.put("upgrade", "无");
	    			data.put("status", "pending");
	    			data.put("openid", userWxInfo.getOpenId());
	    			//data.put("name", "测试账户");//等待用户自己填写
	    			//data.put("phone", "12345678");//等待用户自己填写
	    			HttpClientHelper client = new HttpClientHelper();
	    			result = client.post(url, data);
	    			String redirectUrl = ilifeConfig.getUpdateBrokerUrl();
	    			if(result.get("data")!=null) {//创建成功，则返回修改界面
		    			data = (JSONObject)result.get("data");
		    			String brokerId = data.get("id").toString();
		    			redirectUrl += "?brokerId="+brokerId;//根据该ID进行修改
		    			redirectUrl += "&parentBrokerId="+params[1];//根据上级达人ID发送通知
		    			//注意：由于未填写电话和姓名，此处不发送注册完成通知给上级达人。待填写完成后再发送
	    			}else {//否则返回界面根据openId和上级brokerId创建
	    				redirectUrl += "?openId="+userWxInfo.getOpenId();
	    				redirectUrl += "&parentBrokerId="+params[1];
	    			}

	    			//将推荐者加为当前用户好友：要不然这个新加入的达人就找不到TA的推荐者的么
	    			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
	    			JSONObject parentBrokerJson = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerById/"+params[1], null, header);
        			JSONObject example = new JSONObject();
        			example.put("_from", "user_users/"+userWxInfo.getOpenId());
        			example.put("_to", "user_users/"+parentBrokerJson.getJSONObject("data").getString("openid"));
        			JSONObject query = new JSONObject();
        			query.put("collection", "connections");
        			query.put("example", example);
        			result = HttpClientHelper.getInstance().put(ilifeConfig.getDataApi()+"/_api/simple/by-example", query, header);
        			if(result!=null && result.getIntValue("count")>0) {//该关联已经存在。不做任何处理。
        				//do nothing
        			}else {
        				JSONObject conn = new JSONObject();
        				conn.put("_from", "user_users/"+userWxInfo.getOpenId());//端是新加入的用户
        				conn.put("_to", "user_users/"+parentBrokerJson.getJSONObject("data").getString("openid"));//源是推荐者
        				conn.put("name", "上级达人");//关系名称
        				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
        			}
	    			
		    		//发送消息给新注册达人，提示完成信息
	    			/**
					{{first.DATA}}
					用户名：{{keyword1.DATA}}
					注册时间：{{keyword2.DATA}}
					用户状态：{{keyword3.DATA}}
					{{remark.DATA}}
	    			 */
		        WxMpTemplateMessage welcomeMsg = WxMpTemplateMessage.builder()
		        	      .toUser(userWxInfo.getOpenId())
		        	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
		        	      .url(redirectUrl)
		        	      .build();
		
		        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，您已成功注册达人"))
		        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
		        	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
		        	    		.addData(new WxMpTemplateData("remark", "自购省钱，分享赚钱。为完成审核，还需要填写真实姓名和电话号码，请点击完善。","#FF0000"));
		        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);  
    			}
    		}else {//场景错误
    			logger.error("Unsupport scene str.[str]"+userWxInfo.getQrSceneStr());
    		}  
    }else {//如果是不带参数扫描则作为用户反馈信息：
	    try {
	      return new TextBuilder().build("Life is all about having a good time."
	      		+ "\n\n我们的目标是成为您的私人生活助手，用全面的数据做出合理的消费决策，用小确幸丰富你的的大生活。 "
	      		+ "\n\nEnjoy ~~", wxMessage, weixinService);
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
