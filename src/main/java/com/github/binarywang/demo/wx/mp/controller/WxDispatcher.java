package com.github.binarywang.demo.wx.mp.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.binarywang.demo.wx.mp.service.WeixinService;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import me.chanjar.weixin.mp.bean.result.WxMpUser;

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
	
	/**we
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
	public WxMpUser WxRedirect( @RequestParam("code")String code) throws WxErrorException, IOException {
		logger.debug("try to get access token with code.[code]"+code,code);
		//用户同意授权后，通过code获得access token，其中也包含openid
		WxMpOAuth2AccessToken wxMpOAuth2AccessToken = wxMpService.oauth2getAccessToken(code);
		//获取基本信息
		logger.debug("try to get userInfo with access_token.",wxMpOAuth2AccessToken);
		WxMpUser wxMpUser = wxMpService.oauth2getUserInfo(wxMpOAuth2AccessToken, null);
		logger.debug("Got userInfo",wxMpUser);
		return wxMpUser;
	}
	  
}
