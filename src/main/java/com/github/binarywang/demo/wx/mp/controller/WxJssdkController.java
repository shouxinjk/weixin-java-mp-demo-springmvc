package com.github.binarywang.demo.wx.mp.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.binarywang.demo.wx.mp.service.WeixinService;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxJsapiSignature;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
//import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import me.chanjar.weixin.mp.bean.result.WxMpUser;

@RestController
@RequestMapping("/wechat/jssdk")
public class WxJssdkController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private WxMpService wxMpService;
	
	/**we
	 * 根据指定url构建jssdk签名
	 * @param url
	 * @param response
	 * @throws WxErrorException
	 * @throws IOException
	 */
	@RequestMapping("/ticket")
	@ResponseBody
	public WxJsapiSignature WxRedirect(String url) throws WxErrorException, IOException {
		//获取基本信息
		logger.debug("try to get jssdk ticket with url.[url]"+url,url);
		WxJsapiSignature wxJsapiSignature = wxMpService.createJsapiSignature( url);
		logger.debug("Got wxJsapiSignature",wxJsapiSignature);
		return wxJsapiSignature;
	}
	  
}
