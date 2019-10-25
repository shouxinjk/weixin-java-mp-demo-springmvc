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

    //设置data server Authorization
    Map<String,String> header = new HashMap<String,String>();
    header.put("Authorization","Basic aWxpZmU6aWxpZmU=");
    
    if(userWxInfo.getQrSceneStr().trim().length()>0) {
    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    		String[] params = userWxInfo.getQrSceneStr().trim().split("::");//场景值由两部分组成。TYPE::ID。其中Type为User 或Broker，ID为openId或brokerId
    		if(params.length<2) {//如果无识别标识，不做任何处理
    			logger.error("Wrong scene str.[str]"+userWxInfo.getQrSceneStr());
    		}else if("User".equalsIgnoreCase(params[0])) {//如果是用户邀请则发送
    			//注册新用户，并建立新用户与推荐用的关联
    			JSONObject result = HttpClientHelper.getInstance().post(ilifeConfig.getRegisterUserUrl(), JSONObject.parseObject(userWxInfo.toString()),header);
    			if(result!=null && result.getString("_id")!=null) {//成功创建则继续创建关联关系
    				//建立用户关联：
    				JSONObject conn = new JSONObject();
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
	    	    	        	      .templateId("FL1WVQzCmL5_1bOsPlu5QV_mdeeZJv6WO57pQ5FGjnA")
	    	    	        	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/connection.html")//跳转到好友查看界面
	    	    	        	      .build();
	    	    	
	    	    	        msg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，有亲友注册成功"))
	    	    	        	    		.addData(new WxMpTemplateData("keyword2", userWxInfo.getNickname()))
	    	    	        	    		.addData(new WxMpTemplateData("keyword1", dateFormatLong.format(new Date())))
	    	    	        	    		.addData(new WxMpTemplateData("remark", "为帮助TA获得更好的推荐结果，可以到关心的人查看并完成设置，并查看到特定于TA的推荐结果哦~~"));
	    	    	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(msg);       					
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
    	        	      .templateId("ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI")
    	        	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/user-register.html?openId="+userWxInfo.getOpenId()+"&fromUserOpenId="+params[1])
    	        	      .build();
    	
    	        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，欢迎关注小确幸大生活"))
    	        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
    	        	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
    	        	    		.addData(new WxMpTemplateData("remark", "我们只做一件事，那就是用小确幸填满你的的大生活。成为您的私人生活助手是我们的目标。\nLife is all about having a good time. \nEnjoy ~~"));
    	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);      			
    		}else if("Broker".equalsIgnoreCase(params[0])) {//如果是扫描上级达人二维码关注，则发送模板消息完善达人信息
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
    			JSONObject result = client.post(url, data);
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
	        	      .templateId("oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk")
	        	      .url(redirectUrl)
	        	      .build();
	
	        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，您已成功注册达人"))
	        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
	        	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
	        	    		.addData(new WxMpTemplateData("keyword3", "待完善","#FF0000"))
	        	    		.addData(new WxMpTemplateData("remark", "为完成审核，还需要填写真实姓名和电话号码，请点击完善。"));
	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);  
    		}else {//场景错误
    			logger.error("Unsupport scene str.[str]"+userWxInfo.getQrSceneStr());
    		}  
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
