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
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateLastTicket(brokerId);
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
	
	@RequestMapping(value = "/notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendTemplateMessage(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		
		logger.debug("try to send notification message.[params]",params);
		SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
   	 	//发送通知信息给上级达人
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("parentBorkerOpenId"))//注意：场景值存放的是上级达人的openid
      	      .templateId("hj3ZcC37s4IRo5iJO_TUwJ7ID-VkJ3XQLBMJQeEYNrE")
      	      //.url("http://www.biglistoflittlethings.com/list/")//当前不做跳转
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", "新成员注册成功"))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("name")))
  	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
  	    		.addData(new WxMpTemplateData("keyword3", "微信"))
  	    		.addData(new WxMpTemplateData("remark", "新用户已经完成注册，还需要你的帮助才能完成设置并开始推荐。请保持关注并做必要的示范，让团队变的更强大。\n姓名："+params.get("name")+"\n电话："+params.get("phone")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}

}
