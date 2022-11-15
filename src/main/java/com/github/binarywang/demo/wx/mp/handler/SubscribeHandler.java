package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.config.WxMpConfig;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.helper.HttpClientHelper;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ilife.util.CacheSingletonUtil;
import com.ilife.util.SxHelper;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage.WxArticle;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class SubscribeHandler extends AbstractHandler {
	@Value("#{extProps['mp.msg.media.brokerGroupChat']}") String brokerGroupChatQrcodeMediaId;
  @Autowired
  private iLifeConfig ilifeConfig;	
  @Autowired
  private WxMpConfig wxMpConfig;
  @Autowired
  private SxHelper sxHelper;
  
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  
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
				//建立关心的人
				sxHelper.createDefaultConnections(userWxInfo.getOpenId());
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

    
//    if(userWxInfo.getQrSceneStr().trim().length()>0) {
//    		String[] params = userWxInfo.getQrSceneStr().trim().split("::");//场景值由两部分组成。TYPE::ID。其中Type为User 或Broker，ID为openId或brokerId。对于通过预定义用户添加关心的人的情况，其场景值为User::userId::shadowUserId
    //!!! 注意，不是用户场景码，而是根据Message场景码即EventKey判断
	logger.error("\n\nEvent Key: "+ wxMessage.getEventKey());
	logger.error("\n\nQrSceneStr: "+ userWxInfo.getQrSceneStr());
    if(wxMessage.getEventKey().trim().length()>0) {
	  		String[] params = wxMessage.getEventKey().trim().split("::");//场景值由两部分组成。TYPE::ID。其中Type为User 或Broker，ID为openId或brokerId。对于通过预定义用户添加关心的人的情况，其场景值为User::userId::shadowUserId
	  		if(wxMessage.getEventKey().trim().startsWith("qrscene_")) {//关注时event前缀为 qrscene_，如：qrscene_Inst::Ynqiqm，切换为通过qrscene处理
	  			params = userWxInfo.getQrSceneStr().trim().split("::");
	  		}
	  		if(params.length<2) {//如果无识别标识，不做任何处理
    			logger.error("====\nWrong scene str.[str]"+userWxInfo.getQrSceneStr()+"\n======");
    		}else if("User".equalsIgnoreCase(params[0])) {//如果是用户邀请则发送。User::openId
    			if(result!=null && result.getString("_id")!=null) {//成功创建则继续创建关联关系
    				
      				//如果有shadowUserId则使用shadowUser更新当前用户
      				if(params.length>2) {
      					logger.debug("Try to update user by shadowUser settings.");
      					//查询得到shadowUser信息
      					JSONObject shadowUser = HttpClientHelper.getInstance().get(ilifeConfig.getDataApi()+"/_api/document/user_users/"+params[2],null, header);
      					//更新当前用户
      					if(shadowUser!=null && shadowUser.getString("_id")!=null) {//如果查到虚拟用户则更新吧
      						JSONObject newUser = HttpClientHelper.getInstance().post(ilifeConfig.getDataApi()+"/_api/document/user_users/"+userWxInfo.getOpenId(),shadowUser, header);
      						logger.debug("Target user updated by shadowUser.[result]",newUser);
      					}
      				}
    				
        			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
        			JSONObject example = new JSONObject();
        			example.put("_from", "user_users/"+params[1]);
        			example.put("_to", "user_users/"+userWxInfo.getOpenId());
        			JSONObject data = new JSONObject();
        			data.put("collection", "connections");
        			data.put("example", example);
      				//更新用户关系，如果已经建立则忽略
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
		    	    	        	    		.addData(new WxMpTemplateData("remark", "点击查看详情"));
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
    	        	    		.addData(new WxMpTemplateData("remark", "可以看到或设置特定于TA的推荐：\n\n查看:从底部进入【大生活】\n设置：进入【我-关心的人】 \n\nEnjoy ~~"));
    	        	    String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);      			
    		}else if("Broker".equalsIgnoreCase(params[0])) {//如果是扫描上级达人二维码关注，则发送模板消息完善达人信息。Broker::brokerId
    			//根据openId查找是否已经注册达人
    			result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/checkBrokerByOpenid/"+userWxInfo.getOpenId(),null, header);
    			if(result!=null && result.getBooleanValue("status")) {//特殊情况：已经注册打人后取消关注，再次扫码关注时还是保留原来的达人信息，不另外新建记录
    				//能到这里，说明这货之前已经加入达人，但是又取消关注了。发个消息提示一下就可以了，同时推送加群提示，加强运营支持
					//推送 客服消息，发送加群二维码：二维码图片需要预先上传，此处仅根据mediaId发送
					WxMpKefuMessage kfMsg = WxMpKefuMessage
					  .IMAGE()
					  .toUser(userWxInfo.getOpenId())
					  .mediaId(brokerGroupChatQrcodeMediaId)
					  .build();
					wxMpService.getKefuService().sendKefuMessage(kfMsg);
    				return new TextBuilder().build("欢迎回来，请扫码进群，便于交流~~", wxMessage, weixinService);
    			}else {//如果不是达人，则完成注册
  					//先获取上级达人
  					JSONObject parentBrokerJson = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerById/"+params[1], null, header);
					String level = parentBrokerJson.getJSONObject("data").getString("level");
					
	    			//注册新达人。并建立新达人与上级达人的关联
	    			//String url = "http://localhost:8080/iLife/a/mod/broker/rest/"+params[1];
	    			String url = ilifeConfig.getRegisterBrokerUrl()+params[1];//针对上级达人创建
	    			JSONObject data = new JSONObject();
//	    			data.put("hierarchy", "9");
	    			data.put("level", level);
	    			data.put("upgrade", "无");
	    			data.put("status", "pending");
	    			data.put("openid", userWxInfo.getOpenId());
	    			data.put("nickname", userWxInfo.getNickname());//昵称
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
        				conn.put("name", "邀请人");//关系名称
        				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
        			}
	    			
        			//发送消息给新注册达人，提示完成信息
	    			/**
					{{first.DATA}}
					用户名：{{keyword1.DATA}}
					注册时间：{{keyword2.DATA}}
					{{remark.DATA}}
	    			 */
		        WxMpTemplateMessage welcomeMsg = WxMpTemplateMessage.builder()
		        	      .toUser(userWxInfo.getOpenId())
		        	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
		        	      .url(redirectUrl)
		        	      .build();
		        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+" 恭喜注册成功"))
	    		.addData(new WxMpTemplateData("keyword1", level))//当前无法直接获取昵称，采用类别：生活家、流量主
	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
	    		.addData(new WxMpTemplateData("remark", "感谢关注，我们提供个性化选品服务，以及，流量主运营支持工具。"
	    				+ "\n\n生活家：客观评价，用心选品，轻松选出对的商品，并分享给对的人。"
	    				+ "\n\n流量主：公众号运营、互动及变现交流，提升运营效率，节省时间以输出更优质的内容。"
	    				+ "\n\n无论是个人，还是流量主，都能在分享生活方式和知识内容的同时给出可执行的商品和服务，渔鱼兼授，增加收益。"
	    				+ "\n\n请扫码进群，便于讨论交流。"));
		        String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg); 
				//推送 客服消息，发送加群二维码：二维码图片需要预先上传，此处仅根据mediaId发送
				WxMpKefuMessage kfMsg = WxMpKefuMessage
				  .IMAGE()
				  .toUser(userWxInfo.getOpenId())
				  .mediaId(brokerGroupChatQrcodeMediaId)
				  .build();
				wxMpService.getKefuService().sendKefuMessage(kfMsg);
				
				//给上级达人添加阅豆
				JSONObject postData = new JSONObject();
				JSONObject pointsReward = HttpClientHelper.getInstance().post(ilifeConfig.getSxApi()+"/mod/broker/rest/reward/invite/"+params[1], postData, header);
		        //根据上级达人类型，区分跳转链接
				String targetUrl = "http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html";
				if("流量主".equalsIgnoreCase(level)) {
					targetUrl = "http://www.biglistoflittlethings.com/ilife-web-wx/publiser/team.html";
				}
				
		     //发送通知信息给上级达人
		         WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
            	      .toUser(parentBrokerJson.getJSONObject("data").getString("openid"))//上级达人的openid
            	      .templateId(ilifeConfig.getMsgIdBroker())
            	      .url(targetUrl)//跳转到团队页面：根据生活家或流量主区分
            	      .build();

        	    templateMessage.addData(new WxMpTemplateData("first", "有新成员接受邀请"))
        	    		.addData(new WxMpTemplateData("keyword1", level +" "+userWxInfo.getNickname()))
        	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
        	    		.addData(new WxMpTemplateData("remark", "邀请奖励："+pointsReward.getString("points")+"阅豆\n权益激活：将分享新成员内容带货收益\n\n点击查看团队列表。"));
        	      msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
    			}
    		}else if("Bind".equalsIgnoreCase(params[0])) {//在选品工具中扫码绑定达人账号。Bind::uuid，uuid为本次扫码使用的唯一识别码
	  			logger.error("\n\ngot Scan event.[type]Bind[Scene]"+userWxInfo.getQrSceneStr()+"\n\n");
	  			//根据openId查找是否已经注册达人
	  			result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/checkBrokerByOpenid/"+userWxInfo.getOpenId(),null, header);
	  			if(result!=null && result.getBooleanValue("status")) {//已经注册达人
	  				
	  				logger.debug("The broker exists. try to update openid.[openid]"+userWxInfo.getOpenId());
	  				//直接将openId写入缓存，等待客户端查询完成绑定操作
	  				CacheSingletonUtil.getInstance().addCacheData(params[1], userWxInfo.getOpenId());
			        //发送通知消息
	  		        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
		  		      	      .toUser(userWxInfo.getOpenId())
		  		      	      .templateId("G4ah8DnXccJJydrBEoz0D9XksaFifwVA44hK8o2dIog")//已经注册则直接发送登录状态提醒
		  		      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/index.html")
		  		      	      .build();
		  		  	    templateMessage.addData(new WxMpTemplateData("first", "扫码登录成功"))
		  		  	    		.addData(new WxMpTemplateData("keyword1", dateFormat.format(new Date())))//操作时间
		  		  	    		.addData(new WxMpTemplateData("keyword2", "登录成功"))//登录状态
		  		  	    		.addData(new WxMpTemplateData("keyword3", "小确幸大生活"))//登录网站
		  		  	    		.addData(new WxMpTemplateData("remark", "正在与已注册账户绑定，请进入Web端查看并开始后续操作"));
	  		  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage); 
	  			}else {//如果不是达人，则先完成注册
	  				logger.debug("The broker does not exist. try to register new one.[openid]"+userWxInfo.getOpenId());
	    			String url = ilifeConfig.getRegisterBrokerUrl()+"system";//针对上级达人创建，上级达人默认为系统达人
	    			JSONObject data = new JSONObject();
//	    			data.put("hierarchy", "9");
	    			data.put("level", "生活家");
	    			data.put("upgrade", "无");
	    			data.put("status", "ready");//默认直接设置为ready，后续接收清单推送
	    			data.put("openid", userWxInfo.getOpenId());
	    			data.put("nickname", userWxInfo.getNickname());//昵称
	    			data.put("name", userWxInfo.getNickname());//默认用昵称
	    			//data.put("phone", "12345678");//等待用户自己填写

	    			result = HttpClientHelper.getInstance().post(url, data);
	    			String redirectUrl = ilifeConfig.getUpdateBrokerUrl();
	    			if(result.get("data")!=null) {//创建成功，则返回修改界面
		    			data = (JSONObject)result.get("data");
		    			String brokerId = data.get("id").toString();
		    			redirectUrl += "?brokerId="+brokerId;//根据该ID进行修改
		    			redirectUrl += "&parentBrokerId="+params[1];//根据上级达人ID发送通知
		    			//建立默认的客群画像便于推广
						sxHelper.createDefaultPersonas(userWxInfo.getOpenId());//注意：根据openid建立客群关系，而不是brokerId
		    			//注意：由于未填写电话和姓名，此处不发送注册完成通知给上级达人。待填写完成后再发送
	    			}else {//否则返回界面根据openId和上级brokerId创建
	    				redirectUrl += "?openId="+userWxInfo.getOpenId();
	    				redirectUrl += "&parentBrokerId="+params[1];
	    			}
	    			
	    			//将推荐者加为当前用户好友：要不然这个新加入的达人就找不到TA的推荐者的么
	    			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
	    			JSONObject parentBrokerJson = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerById/system", null, header);
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
	      				conn.put("name", "邀请人");//关系名称
	      				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
	      			}
      			
		    		//发送消息给新注册达人，提示完成信息
	    			/**
					{{first.DATA}}
					用户名：{{keyword1.DATA}}
					注册时间：{{keyword2.DATA}}
					{{remark.DATA}}
	    			 */
			        WxMpTemplateMessage welcomeMsg = WxMpTemplateMessage.builder()
			        	      .toUser(userWxInfo.getOpenId())
			        	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
			        	      .url(redirectUrl)
			        	      .build();
			        //发送通知消息
			        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，成功注册达人"))
			        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
			        	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
			        	    		.addData(new WxMpTemplateData("remark", "已经完成注册，正在绑定账户到选品工具，请进入PC端选品工具查看。为立即开始，请填写真实姓名和电话号码，请点击卡片，一步即可完善。","#FF0000"));
			        String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);  
		        	  //推送 客服消息，发送加群二维码：二维码图片需要预先上传，此处仅根据mediaId发送
						WxMpKefuMessage kfMsg = WxMpKefuMessage
						  .IMAGE()
						  .toUser(userWxInfo.getOpenId())
						  .mediaId(brokerGroupChatQrcodeMediaId)
						  .build();
						wxMpService.getKefuService().sendKefuMessage(kfMsg);
			     //发送通知信息给上级达人
			         WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                	      .toUser(parentBrokerJson.getJSONObject("data").getString("openid"))//上级达人的openid
                	      .templateId(ilifeConfig.getMsgIdBroker())
                	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html")//跳转到团队页面
                	      .build();

            	    templateMessage.addData(new WxMpTemplateData("first", "有新机构用户从工具端扫码加入"))
            	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
            	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
            	    		.addData(new WxMpTemplateData("remark", "请进入团队列表查看。","#FF0000"));
            	      msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	  				//完成注册后将openId写入缓存，等待客户端查询完成绑定操作
  	  				CacheSingletonUtil.getInstance().addCacheData(params[1], userWxInfo.getOpenId());
	  			}
	  		}else if("Inst".equalsIgnoreCase(params[0])) {//通过分享链接直接进入系统时，即时扫码关注。格式为Inst:xxxxxx,其中xxxxxx为6位短码。
	  			logger.debug("\n\ngot Scan event.[type]Inst[Scene]"+userWxInfo.getQrSceneStr());
	  			//根据openId查找是否已经注册达人
	  			result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/checkBrokerByOpenid/"+userWxInfo.getOpenId(),null, header);
	  			if(result!=null && result.getBooleanValue("status")) {//已经注册达人
	  				
	  				logger.debug("The broker exists. try to update openid.[openid]"+userWxInfo.getOpenId());
	  				//直接将openId写入缓存，等待客户端查询完成绑定操作
	  				CacheSingletonUtil.getInstance().addCacheData(params[1], userWxInfo.getOpenId());
	  				
			        //发送通知消息：由于扫码关注后将跳转到公众号界面，需要通过模板消息回到访问前页面。
	  				//通过短地址中转：由前端负责根据短码跳转
	  				//**
	  		        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
	  		      	      .toUser(userWxInfo.getOpenId())
	  		      	      .templateId("G4ah8DnXccJJydrBEoz0D9XksaFifwVA44hK8o2dIog")//已经注册则直接发送登录状态提醒
	  		      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/s.html?s="+params[1])
	  		      	      .build();
	  		  	    templateMessage.addData(new WxMpTemplateData("first", "扫码成功，请点击进入"))
	  		  	    		.addData(new WxMpTemplateData("keyword1", dateFormat.format(new Date())))//操作时间
	  		  	    		.addData(new WxMpTemplateData("keyword2", "登录成功"))//登录状态
	  		  	    		.addData(new WxMpTemplateData("keyword3", "小确幸大生活"))//登录网站
	  		  	    		.addData(new WxMpTemplateData("remark", "已经完成账户准备，请点击进入"));
	  		  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage); 
	  		  	     //**/
	  			}else {//如果不是达人，则先完成注册：注意，当前主要由流量主完成，自动注册达人
	  				logger.debug("The broker does not exist. try to register new one.[openid]"+userWxInfo.getOpenId());
//	    			String url = ilifeConfig.getRegisterBrokerUrl()+"system";//针对上级达人创建，上级达人默认为系统达人
	    			String url = ilifeConfig.getRegisterBrokerUrl()+ilifeConfig.getDefaultParentBrokerId();//注册为普通达人
	    			JSONObject data = new JSONObject();
//	    			data.put("hierarchy", "9");
	    			data.put("level", "流量主");
	    			data.put("upgrade", "无");
	    			data.put("status", "ready");//默认直接设置为ready，后续接收清单推送
	    			data.put("openid", userWxInfo.getOpenId());
	    			data.put("nickname", userWxInfo.getNickname());//昵称
	    			data.put("name", userWxInfo.getNickname());//默认用昵称
	    			//data.put("phone", "12345678");//等待用户自己填写

	    			result = HttpClientHelper.getInstance().post(url, data);
	    			String redirectUrl = ilifeConfig.getUpdateBrokerUrl();
	    			if(result.get("data")!=null) {//创建成功，则返回修改界面
		    			data = (JSONObject)result.get("data");
		    			String brokerId = data.get("id").toString();
		    			redirectUrl += "?brokerId="+brokerId;//根据该ID进行修改
		    			redirectUrl += "&parentBrokerId="+params[1];//根据上级达人ID发送通知
		    			//建立默认的客群画像便于推广
						sxHelper.createDefaultPersonas(userWxInfo.getOpenId());//注意：根据openid建立客群关系，而不是brokerId
		    			//注意：由于未填写电话和姓名，此处不发送注册完成通知给上级达人。待填写完成后再发送
	    			}else {//否则返回界面根据openId和上级brokerId创建
	    				redirectUrl += "?openId="+userWxInfo.getOpenId();
	    				redirectUrl += "&parentBrokerId="+params[1];
	    			}
	    			
	    			//将推荐者加为当前用户好友：要不然这个新加入的达人就找不到TA的推荐者的么
	    			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
	    			JSONObject parentBrokerJson = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerById/"+ilifeConfig.getDefaultParentBrokerId(), null, header);
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
	      				conn.put("name", "邀请人");//关系名称
	      				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
	      			}
      			
		    		//发送消息给新注册达人，提示完成信息
	    			/**
					{{first.DATA}}
					用户名：{{keyword1.DATA}}
					注册时间：{{keyword2.DATA}}
					{{remark.DATA}}
	    			 */
	      			/*
			        WxMpTemplateMessage welcomeMsg = WxMpTemplateMessage.builder()
			        	      .toUser(userWxInfo.getOpenId())
			        	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
			        	      .url(redirectUrl)
			        	      .build();
			        //发送通知消息
			        welcomeMsg.addData(new WxMpTemplateData("first", userWxInfo.getNickname()+"，成功注册达人"))
			        	    		.addData(new WxMpTemplateData("keyword1", userWxInfo.getNickname()))
			        	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
			        	    		.addData(new WxMpTemplateData("remark", "已经完成注册，正在绑定账户到选品工具，请进入PC端选品工具查看。为立即开始，请填写真实姓名和电话号码，请点击卡片，一步即可完善。","#FF0000"));
			        String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(welcomeMsg);  
			        //**/
	      			//发送通知信息给上级达人
	      			WxMpTemplateMessage msg = buildParentBrokerNotifyMsg("有流量主即时扫码加入",userWxInfo.getNickname(),
	      					parentBrokerJson.getJSONObject("data").getString("openid"),
	      					"http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html");
	      			String  msgId = weixinService.getTemplateMsgService().sendTemplateMsg(msg); 
  	  				//完成注册后将openId写入缓存，等待客户端查询完成绑定操作
  	  				CacheSingletonUtil.getInstance().addCacheData(params[1], userWxInfo.getOpenId());
	  			}
	  		}else {//场景错误
    			logger.error("Unsupport scene str.[str]"+userWxInfo.getQrSceneStr());
    		}  
    }else {//如果是不带参数扫描则作为用户反馈信息：
    	//根据openId查找是否已经注册达人
		result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/checkBrokerByOpenid/"+userWxInfo.getOpenId(),null, header);
		if(result!=null && result.getBooleanValue("status")) {//特殊情况：已经注册打人后取消关注，再次扫码关注时还是保留原来的达人信息，不另外新建记录
			//能到这里，说明这货之前已经加入达人，但是又取消关注了。发个消息提示一下就可以了，同时发送加群消息，加强运营支持
			//推送 客服消息，发送加群二维码：二维码图片需要预先上传，此处仅根据mediaId发送
			/**
			WxMpKefuMessage kfMsg = WxMpKefuMessage
			  .IMAGE()
			  .toUser(userWxInfo.getOpenId())
			  .mediaId(brokerGroupChatQrcodeMediaId)
			  .build();
			wxMpService.getKefuService().sendKefuMessage(kfMsg);
			//**/
			return new TextBuilder().build("感谢关注，我们是决策助手，提供推荐、评价、定制服务，让每一个生活决策都带来小确幸，填满大生活~~", wxMessage, weixinService);
		}else if(ilifeConfig.isAutoRegisterBroker()) {//推广早期，所有注册者均 直接作为达人加入。完成后返回上级达人群二维码图片，便于加群维护
			  try {
				  //自动注册为达人
				  String redirectUrl = registerBroker(userWxInfo.getOpenId(),userWxInfo.getNickname());
				  String url = redirectUrl + "#wechat_redirect";//经由微信OAuth授权后返回
				  //返回通知消息：给新注册达人
//				  WxMpTemplateMessage msg = buildBrokerNotifyMsg(userWxInfo.getNickname(),userWxInfo.getOpenId(),url);
//			      String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(msg); 
				  //返回通知消息：给新注册达人。注意通过dispatch获取授权，得到基本信息
				  /**
				  String img = "http://www.shouxinjk.net/list/images/welcome.jpeg";
				  List<WxArticle> articles = Lists.newArrayList();
				  WxArticle article = new WxArticle("成功注册达人","点击补充基本信息",url,img);
				  articles.add(article);
				  WxMpKefuMessage kfMsg0 = WxMpKefuMessage.NEWS().toUser(userWxInfo.getOpenId()).articles(articles).build();
				  wxMpService.getKefuService().sendKefuMessage(kfMsg0);
				  //**/
			      //返回通知消息：给默认达人用户：由于是扫码绑定，默认直接归属于平台
				  WxMpTemplateMessage msg = buildParentBrokerNotifyMsg("有新用户自动注册达人",userWxInfo.getNickname(),ilifeConfig.getDefaultParentBrokerOpenid(),"http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html");
				  String msgId = weixinService.getTemplateMsgService().sendTemplateMsg(msg);
					//推送 客服消息，发送加群二维码：二维码图片需要预先上传，此处仅根据mediaId发送
				  /**
					WxMpKefuMessage kfMsg = WxMpKefuMessage
					  .IMAGE()
					  .toUser(userWxInfo.getOpenId())
					  .mediaId(brokerGroupChatQrcodeMediaId)
					  .build();
					wxMpService.getKefuService().sendKefuMessage(kfMsg);
					//**/
			  }catch(Exception ex) {
				  //do nothing
			  }
		  }else {
			//否则发送三条提示文章：分别是生活专家、流量主指南、达人指南。注意：客服消息下行最多3条
			  String titles[] = {"每一个人都是生活的专家",
					  "流量主指南",
					  "生活家指南"};
			  String urls[] = {"https://mp.weixin.qq.com/s?__biz=MzU2NTc3OTQ0MA==&mid=2247485093&idx=1&sn=66b0000ce1dfefdf531653c1620c3c97&chksm=fcb7c83acbc0412c35939ad3d4f2769acef590cc7bf5c721160c490aa29116739a8b73b9306c&token=711315459&lang=zh_CN#rd",
					  "https://mp.weixin.qq.com/s?__biz=MzU2NTc3OTQ0MA==&mid=2247484495&idx=1&sn=60f598192331dffc96928bac1f82d5d9&chksm=fcb7cad0cbc043c65d0b6eaea648182982fe12a94fbe618efa4c78dc8a875477edf9efe5daa1#rd",
					  "https://mp.weixin.qq.com/s?__biz=MzU2NTc3OTQ0MA==&mid=2247484348&idx=3&sn=c45cd6910f0df27011e4b487139bae1f&chksm=fcb7cd23cbc044359021b6ef47b77c0ccd53371a6131dfde45285dd17141942560defef74b9c&token=711315459&lang=zh_CN#rd"};
			  String images[] = {"https://mmbiz.qpic.cn/mmbiz_jpg/wRApiaFsiakTQicDIIJ0c0d0OibWvu7EhL9sppyeMweOiaKlVspFicUkZFMuicCoIvKgicJvI8rwwoF4ktvGDoNSa8TQzw/0?wx_fmt=jpeg",
					  "https://mmbiz.qpic.cn/mmbiz_png/wRApiaFsiakTSBVB91UnnXFovdzkBMsmBh6T99WoWaYRmUDtz6DGYoeYBicA8cOVSLYCJ7nyrAEMaicDXo0xH6DYxA/0?wx_fmt=png",
					  "https://mmbiz.qpic.cn/mmbiz_jpg/wRApiaFsiakTTDPM4SuWibFqgnKm4cRo2zDgV1vY26dSdFtGbaCzxPWSgnklAj2PxOu4UKWzv4Xgv6Acr5DibaRbTQ/0?wx_fmt=jpeg"};
			  String descriptions[] = {"做出好的消费决策，建立好的消费方式，成为生活的专家",
					  "作为流量主，不仅要输出好内容，也要让受众所阅及所得，快速选品工具少不了",
					  "选出好的，分享对的，用心挑选小确幸，建立自己的第二份收入"};
			  for(int i=0;i<titles.length;i++) {
				  List<WxArticle> articles = Lists.newArrayList();
				  WxArticle article = new WxArticle(titles[i],descriptions[i],urls[i],images[i]);
				  articles.add(article);
				  WxMpKefuMessage kfMsg = WxMpKefuMessage.NEWS().toUser(userWxInfo.getOpenId()).articles(articles).build();
				  wxMpService.getKefuService().sendKefuMessage(kfMsg);
			  }
		  }
		  //最后都要返回申明
		  return new TextBuilder().build("感谢遇见。我们是决策助手，提供推荐、评价、定制服务，让每一个生活决策都带来小确幸，填满大生活 ~~", wxMessage, weixinService);
    }

    return null;
  }

  
  /**
   * 处理特殊请求，比如如果是扫码进来的，可以做相应处理
   */
  protected WxMpXmlOutMessage handleSpecial(WxMpXmlMessage wxMessage) throws Exception {
    //TODO
	logger.info("\n\n特殊关注 \n[OPENID]\t" + wxMessage.getFromUser()+"\n[Scene]\t"+wxMessage.getScene());
	
    return null;
  }
  

	/**
	 * 构建新注册达人通知模板消息
	 * @param name 达人昵称，或姓名
	 * @param openid openid
	 * @param url 跳转地址
	 * @return
	 */
	private  WxMpTemplateMessage buildBrokerNotifyMsg(String name,String openid,String url) {
      WxMpTemplateMessage msg = WxMpTemplateMessage.builder()
    	      .toUser(openid)
    	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
    	      .url(url)
    	      .build();
      msg.addData(new WxMpTemplateData("first", name+"，感谢关注"))
    	    		.addData(new WxMpTemplateData("keyword1", name))
    	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
    	    		.addData(new WxMpTemplateData("remark", "我们同时提供了自购省钱、分享赚钱功能，点击注册就可以马上开始，欢迎扫码进群交流哦~~","#FF0000"));
    
      return msg;
	}
	
	/**
	 * 构建达人注册后，上级达人通知信息
	 * @param name 达人昵称，或姓名
	 * @param openid openid
	 * @param url 跳转地址
	 * @return
	 */
	private  WxMpTemplateMessage buildParentBrokerNotifyMsg(String title,String name,String openid,String url) {
      WxMpTemplateMessage msg = WxMpTemplateMessage.builder()
    	      .toUser(openid)
    	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
    	      .url(url)
    	      .build();
      msg.addData(new WxMpTemplateData("first", title))
    	    		.addData(new WxMpTemplateData("keyword1", name))
    	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
    	    		.addData(new WxMpTemplateData("remark", "请进入团队列表查看。","#FF0000"));
    
      return msg;
	}
  
	private String registerBroker(String openid,String nickname) {
//		String redirectUrl = ilifeConfig.getUpdateBrokerUrl();
		  String redirectUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+wxMpConfig.getAppid()
	  		+ "&redirect_uri=https://www.biglistoflittlethings.com/ilife-web-wx/dispatch.html"
	  		+ "&response_type=code"
	  		+ "&scope=snsapi_userinfo"
	  		+ "&state=broker__team-register";//前往broker/team-register.html页面
//	  		+ "#wechat_redirect";
		//准备发起HTTP请求：设置data server Authorization
		Map<String,String> header = new HashMap<String,String>();
		header.put("Authorization","Basic aWxpZmU6aWxpZmU=");
		//根据openId查找是否已经注册达人
		JSONObject result = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/checkBrokerByOpenid/"+openid,null, header);
		if(result!=null && result.getBooleanValue("status")) {//已经注册达人
			//do nothing
		}else {//如果不是达人，则完成注册
			String url = ilifeConfig.getRegisterBrokerUrl()+ilifeConfig.getDefaultParentBrokerId();//固定达人ID 
			JSONObject data = new JSONObject();
			data.put("hierarchy", "3");//是一个3级达人
			data.put("level", "生活家");
			data.put("upgrade", "无");
			data.put("status", "ready");//默认直接设置为ready，后续接收清单推送
			data.put("openid", openid);
			data.put("nickname", nickname);//昵称
			data.put("name", nickname);//默认用nickName
			//data.put("phone", "12345678");//等待用户自己填写

			result = HttpClientHelper.getInstance().post(url, data);
			if(result.get("data")!=null) {//创建成功，则返回修改界面
	  			data = (JSONObject)result.get("data");
	  			String brokerId = data.get("id").toString();
//	  			redirectUrl += "?brokerId="+brokerId;//根据该ID进行修改
//	  			redirectUrl += "&parentBrokerId="+ilifeConfig.getDefaultParentBrokerId();//根据上级达人ID发送通知
	  			redirectUrl += "___brokerId="+brokerId;//根据该ID进行修改
	  			redirectUrl += "__parentBrokerId="+ilifeConfig.getDefaultParentBrokerId();//根据上级达人ID发送通知
	  			//建立默认的客群画像便于推广
				sxHelper.createDefaultPersonas(openid);//注意：根据openid建立客群关系，而不是brokerId
				//注意：由于未填写电话和姓名，此处不发送注册完成通知给上级达人。待填写完成后再发送
			}else {//否则返回界面根据openId和上级brokerId创建
//				redirectUrl += "?openId="+openid;
//				redirectUrl += "&parentBrokerId="+ilifeConfig.getDefaultParentBrokerId();
				redirectUrl += "___openId="+openid;
				redirectUrl += "__parentBrokerId="+ilifeConfig.getDefaultParentBrokerId();
			}
			
			//将推荐者加为当前用户好友：要不然这个新加入的达人就找不到TA的推荐者的么
			//检查用户关联是否存在:对于特殊情况，用户已经添加好友，然后取消关注，再次扫码关注后避免重复建立关系
			JSONObject parentBrokerJson = HttpClientHelper.getInstance().get(ilifeConfig.getSxApi()+"/mod/broker/rest/brokerById/"+ilifeConfig.getDefaultParentBrokerId(), null, header);
			JSONObject example = new JSONObject();
			example.put("_from", "user_users/"+openid);
			example.put("_to", "user_users/"+parentBrokerJson.getJSONObject("data").getString("openid"));
			JSONObject query = new JSONObject();
			query.put("collection", "connections");
			query.put("example", example);
			result = HttpClientHelper.getInstance().put(ilifeConfig.getDataApi()+"/_api/simple/by-example", query, header);
			if(result!=null && result.getIntValue("count")>0) {//该关联已经存在。不做任何处理。
				//do nothing
			}else {
				JSONObject conn = new JSONObject();
				conn.put("_from", "user_users/"+openid);//端是新加入的用户
				conn.put("_to", "user_users/"+parentBrokerJson.getJSONObject("data").getString("openid"));//源是推荐者
				conn.put("name", "邀请人");//关系名称
				result = HttpClientHelper.getInstance().post(ilifeConfig.getConnectUserUrl(), conn,header);
			}
		}
		return redirectUrl;
	}

}
