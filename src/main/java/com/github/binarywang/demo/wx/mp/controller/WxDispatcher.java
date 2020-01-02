package com.github.binarywang.demo.wx.mp.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.google.common.collect.Maps;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

@RestController
@RequestMapping("/wechat/ilife")
public class WxDispatcher {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private WxMpService wxMpService;

	/**
	 * 接收从菜单入口传入的code和state值，并且使用code获取access token
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/auth")
	@ResponseBody
	public WxMpOAuth2AccessToken wxLogin( @RequestParam("code")String code) throws IOException {
		WxMpOAuth2AccessToken wxMpOAuth2AccessToken = null;
		try {
			logger.debug("try to get access token with code.[code]"+code,code);
			wxMpOAuth2AccessToken = wxMpService.oauth2getAccessToken(code);
		} catch (WxErrorException e) {
			logger.error("failed to get access token.[code]"+code,e);
		}
		return wxMpOAuth2AccessToken;
	}
	
	/**
	 * 用户授权后获取UserInfo。如果未授权则无信息返回
	 * 从公众号菜单进入时，回调地址会请求到该服务，并且传递code
	 * 网页访问则在用户授权后进入本服务
	 * @param code,state
	 * @param response
	 * @throws WxErrorException
	 * @throws IOException
	 */
	@RequestMapping("/login")
	@ResponseBody
	public WxMpUser WxRedirect(String code) throws WxErrorException, IOException {
		logger.debug("try to get access token with code.[code]"+code,code);
		//用户同意授权后，通过code获得access token，其中也包含openid
		WxMpOAuth2AccessToken wxMpOAuth2AccessToken = wxMpService.oauth2getAccessToken(code);
		//获取基本信息
		logger.debug("try to get userInfo with access_token.",wxMpOAuth2AccessToken);
		WxMpUser wxMpUser = wxMpService.oauth2getUserInfo(wxMpOAuth2AccessToken, null);
		logger.debug("Got userInfo",wxMpUser);
		return wxMpUser;
	}
	  
	
	/**
	 * 生成达人推广二维码。需要通过前端完成，操作逻辑为：
	 * 1，先通过ilife注册达人，并获得达人ID
	 * 2，请求生成二维码，参数为达人ID。返回达人ID、二维码URL
	 * 3，通过ilife更新达人，将二维码URL写入达人信息
	 */
	@RequestMapping("/qrcode")
	@ResponseBody
	public Map<String, Object> generateBrokerQRCode(@RequestParam("brokerId")String brokerId) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to generate QRcode for broker.[id]"+brokerId);
		//用户同意授权后，通过code获得access token，其中也包含openid
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateLastTicket("Broker::"+brokerId);
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		Map<String, Object> data = Maps.newHashMap();
		data.put("id", brokerId);
		data.put("url", url);
		result.put("status",true);
		result.put("data",data);
		result.put("description","Broker QRCode created successfully");
		return result;
	}
	
	/**
	 * 生成用户特定的临时二维码。分享后可以关注用户
	 */
	@RequestMapping("/tempQRcode")
	@ResponseBody
	public Map<String, Object> generateUserQRCode(@RequestParam("userId")String userId) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to generate temp QRcode for user.[id]"+userId);
		//用户同意授权后，通过code获得access token，其中也包含openid
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("User::"+userId,2592000);//有效期30天
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		Map<String, Object> data = Maps.newHashMap();
		data.put("id", userId);
		data.put("url", url);
		result.put("status",true);
		result.put("data",data);
		result.put("description","User Temp QRCode created successfully");
		return result;
	}	
	
	@RequestMapping(value = "/notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendTemplateMessage(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		
		logger.debug("try to send notification message.[params]",params);
		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
   	 	//发送通知信息给上级达人
		/**
{{first.DATA}}
注册用户：{{keyword1.DATA}}
注册时间：{{keyword2.DATA}}
注册来源：{{keyword3.DATA}}
{{remark.DATA}}
		 */
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("parentBorkerOpenId"))//注意：场景值存放的是上级达人的openid
      	      .templateId("hj3ZcC37s4IRo5iJO_TUwJ7ID-VkJ3XQLBMJQeEYNrE")
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html")//跳转到团队页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", "新成员注册成功"))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("name")))
  	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
  	    		.addData(new WxMpTemplateData("keyword3", "微信"))
  	    		.addData(new WxMpTemplateData("remark", "你有新用户已经完成注册，还需要你的帮助才能完成设置并开始推荐。请保持关注并做必要的示范，让团队变的更强大。\n姓名："+params.get("name")+"\n电话："+params.get("phone")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	

	/**
	 * 根据清分结果发送通知。需要根据beneficiary区分不同的消息类型：
	 * 店返：beneficiary=broker
	 * 团返：beneficiary=grandpa 或 beneficiary = parent
	 * 积分：beneficiary = credit
	 * 意向：beneficiary = buy
	 * 
	 * 消息格式：
	 * 恭喜，你有新订单成交：
	 * 商品名称：xxxx。 item
	 * 订单时间：xxx。orderTime
	 * 预估佣金：xx。commissionEstimate
	 * 结算状态：xx。status
	 * 
	 * 目标用户：brokerOpenid
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/clearing-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendOrderNotificationMsg(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("brokerOpenid")==null || params.get("brokerOpenid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send clearing notification msg.[params]",params);
		Map<String, String> titles = Maps.newHashMap();
		titles.put("broker", "店返");
		titles.put("parent", "团返");
		titles.put("grandpa", "团返");
		titles.put("buy", "平台激励");
		titles.put("credit", "平台激励");
		
		Map<String, String> statusTitles = Maps.newHashMap();
		statusTitles.put("locked", "锁定：团队达标后结算");
		statusTitles.put("cleared", "待结算");
		
		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String remark = "";
		remark+=params.get("item")!=null?"商品名称："+params.get("item"):"";
		remark+=params.get("platform")!=null?"\n来源平台："+params.get("platform"):"";
		remark+=params.get("orderTime")!=null?"\n订单时间："+dateFormatLong.format(params.get("orderTime")):"";
		remark+=params.get("seller")!=null?"\n团队成员："+params.get("seller"):"";
		
		if(remark.trim().length()==0)remark = "贡献越大，收益越多哦~~";
		
		logger.debug("try to send order notification message.[params]",params);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("brokerOpenid"))
      	      .templateId("hj3ZcC37s4IRo5iJO_TUwJ7ID-VkJ3XQLBMJQeEYNrE")//TODO：待确定模板编号
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", "恭喜，有新订单成交"))
  	    		.addData(new WxMpTemplateData("keyword1", titles.get(params.get("beneficiary"))))//收益类别
  	    		.addData(new WxMpTemplateData("keyword2", params.get("amountProfit")))//收益金额
  	    		.addData(new WxMpTemplateData("keyword3", statusTitles.get(params.get("status"))))//清分状态
  	    		.addData(new WxMpTemplateData("remark", remark));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 发送绩效订单通知。是一个汇总信息
	 * 消息格式：
	 * 绩效{日报/周报/月报}：type:daily/weekly/monthly
	 * 分享数：xxxx。 shares
	 * 浏览数：xxx。views
	 * 意向数：xx。buys
	 * 订单数：xx。orders
	 * 
	 * 附加信息：xxx。msg
	 * 
	 * 目标用户：brokerOpenid、brokerName
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/performance-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendPerformanceNotificationMsg(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("brokerOpenid")==null || params.get("brokerOpenid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send performance notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		
		Map<String, String> titles = Maps.newHashMap();
		titles.put("daily", "这是今天的推广效果哦");
		titles.put("weekly", "本周的绩效汇总来了");
		titles.put("monthly", "上月的绩效汇总来了");
		titles.put("yearly", "这是今年的绩效汇总");
		
		logger.info("start send performance notification message.[params]",params);
		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("brokerOpenid"))
      	      .templateId("hj3ZcC37s4IRo5iJO_TUwJ7ID-VkJ3XQLBMJQeEYNrE")//TODO：待确定模板编号
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/money.html")//订单汇总通知需要跳转到绩效界面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("brokerName")+"，"+titles.get(params.get("brokerName"))))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("shares")))
  	    		.addData(new WxMpTemplateData("keyword2", params.get("views")))
  	    		.addData(new WxMpTemplateData("keyword3", params.get("buys")))
  	    		.addData(new WxMpTemplateData("remark", "订单数："+params.get("orders")+
  	    				(params.get("msg")!=null&&params.get("msg").toString().trim().length()>0?("\n备注："+params.get("msg")):"")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	

}
