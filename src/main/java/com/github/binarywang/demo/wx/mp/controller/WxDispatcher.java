package com.github.binarywang.demo.wx.mp.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.binarywang.demo.wx.mp.config.WxMpConfig;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.google.common.collect.Maps;
import com.ilife.util.CacheSingletonUtil;
import com.ilife.util.SxHelper;
import com.ilife.util.Util;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.bean.result.WxMediaUploadResult;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.material.WxMediaImgUploadResult;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
//import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

@RestController
@RequestMapping("/wechat/ilife")
public class WxDispatcher {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	SimpleDateFormat dateFormatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	@Value("#{extProps['mp.msg.url.prefix']}") String ilifeUrlPrefix;
	
	@Autowired
	private WxMpService wxMpService;
	  @Autowired
	  private iLifeConfig ilifeConfig;
	  @Autowired
	  private WxMpConfig wxMpConfig;
	  @Autowired
	  private SxHelper helper;
	  
	  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	  
	  /*
		 * 构建用户授权URL。
		 * 
		 * @param redirectUrl 授权完成后的跳转地址。在该地址内接收返回code，并完成后续处理
		 * @param state 状态标记。预留，用于控制跳转
		 */
		@RequestMapping("/auth-url")
		@ResponseBody
		public String buildAuthPage( @RequestParam("redirectUrl")String callbackUrl,@RequestParam("state")String state) throws IOException {
			logger.debug("try to build auth url.[callbackUrl]"+callbackUrl+"[state]"+state);
		    WxOAuth2Service oAuth2Service = wxMpService.getOAuth2Service();
		    return oAuth2Service.buildAuthorizationUrl(callbackUrl,WxConsts.OAuth2Scope.SNSAPI_USERINFO, state);
		}	  
	  
	/**
	 * 接收从菜单入口传入的code和state值，并且使用code获取access token
	 * @param response
	 * @throws IOException
	 */
	/**
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
	}//**/
	@RequestMapping("/auth")
	@ResponseBody
	public WxOAuth2AccessToken wxLogin( @RequestParam("code")String code) throws IOException {
		WxOAuth2Service oAuth2Service = wxMpService.getOAuth2Service();
		WxOAuth2AccessToken wxMpOAuth2AccessToken = null;
		try {
			logger.debug("try to get access token with code.[code]"+code,code);
			wxMpOAuth2AccessToken = oAuth2Service.getAccessToken(code);
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
	/**
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
	}//**/
	@RequestMapping("/login")
	@ResponseBody
	public WxOAuth2UserInfo WxRedirect(String code) throws WxErrorException, IOException {
		logger.debug("try to get access token with code.[code]"+code,code);
		//用户同意授权后，通过code获得access token，其中也包含openid
		WxOAuth2Service oAuth2Service = wxMpService.getOAuth2Service();
		WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
		//获取基本信息
		logger.debug("try to get userInfo with access_token.",accessToken);
	    WxOAuth2UserInfo wxMpUser = oAuth2Service.getUserInfo(accessToken, null);
		logger.debug("Got userInfo",wxMpUser);
		return wxMpUser;
	}
	
	/**
	 * 将图文内容中的图片url上传为永久素材。该接口不占用永久素材数量。
	 * 注意：
	 * 该接口不影响素材分辨率尺寸，对于构建小程序卡片不符合 1080*864的要求
	 */
	@RequestMapping(value ="/upload-media", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject uploadMediaByUrl(@RequestBody JSONObject json) throws WxErrorException, IOException {
		JSONObject result = new JSONObject();
		result.put("success", false);
		String imgUrl = json.getString("imgUrl");//获取图片地址
		if(imgUrl == null) {
			result.put("msg", "imgUrl is mandatory.");
			return result;
		}
		
		HttpURLConnection httpUrl = (HttpURLConnection) new URL(imgUrl).openConnection();
        httpUrl.connect();
        InputStream inputStream = httpUrl.getInputStream();
        String fileType = HttpURLConnection.guessContentTypeFromStream( inputStream );//猜测文件类型
        if(fileType==null)fileType=".png";
        File tmpFile = Files.createTempFile("tmp", fileType).toFile();
        FileUtils.copyInputStreamToFile(inputStream, tmpFile);
        inputStream.close();
        
		WxMediaImgUploadResult uploadResult = wxMpService.getMaterialService().mediaImgUpload(tmpFile);
		httpUrl.disconnect();
		
		result.put("success", true);
		result.put("url", uploadResult.getUrl());
		
		return result;
	}
	
	/**
	 * 将图文内容中的图片url上传为临时素材。
	 * 注意：
	 * 该接口不影响素材分辨率尺寸，对于构建小程序卡片不符合 1080*864的要求
	 */
	@RequestMapping(value ="/upload-tmp-media", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject uploadAttachmentByUrl(@RequestBody JSONObject json) throws WxErrorException, IOException {
		JSONObject result = new JSONObject();
		result.put("success", false);
		String imgUrl = json.getString("imgUrl");//获取图片地址
		if(imgUrl == null) {
			result.put("msg", "imgUrl is mandatory.");
			return result;
		}
		
		HttpURLConnection httpUrl = (HttpURLConnection) new URL(imgUrl).openConnection();
        httpUrl.connect();
        InputStream inputStream = httpUrl.getInputStream();
        String fileType = HttpURLConnection.guessContentTypeFromStream( inputStream );//猜测文件类型
        if(fileType==null)fileType=".png";
        File tmpFile = Files.createTempFile("tmp", fileType).toFile();
        FileUtils.copyInputStreamToFile(inputStream, tmpFile);
        inputStream.close();
        
		WxMediaUploadResult uploadResult = wxMpService.getMaterialService().mediaUpload("image", tmpFile);
		httpUrl.disconnect();
		
		result.put("success", true);
		result.put("url", uploadResult.getUrl());
		
		return result;
	}

	/**
	 * 生成短连接，便于二维码识别
	 */
	@RequestMapping(value ="/short-url", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> shortUrl(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		String longUrl = params.get("longUrl");
		logger.debug("try to generate short url for long url.[url]"+longUrl);
		String url = wxMpService.shortUrl(longUrl);
		logger.debug("Got QRcode URL. [URL]",url);
		Map<String, Object> data = Maps.newHashMap();
		data.put("url", url);
		result.put("status",true);
		result.put("data",data);
		result.put("description","short url created successfully");
		return result;
	}
	
	/**
	 * SaaS 端：
	 * 用于商家用户从小程序扫码关注服务号时，场景值为商家小程序用户 TUser::openid
	 * 用户扫码扫码后前端能够得到服务号openid、unionid，并自动补充到 wwCustomer
	 * 二维码格式为：TUser::openid
	 * 其中openid为当前二维码识别标志，扫码完成后前端将根据该标志wwCustomer 并更新 sxOpenid、sxUnionId
	 * 
	 * @param openid 为商家小程序下用户 openid
	 */
	@RequestMapping("/tuser-qrcode")
	@ResponseBody
	public Map<String, Object> generateTenantUserQRCode(@RequestParam("openid")String openid) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to generate tenant user QRcode.[openid]"+openid);
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("TUser::"+openid,2592000);//有效期30天，注意场景值长度不能超过64
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		result.put("url", url);
		result.put("status",true);
		result.put("description","Tenant User QRCode created successfully");
		return result;
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
	 * userId：发起邀请的用户ID，为其openId
	 * shadowUserId：为建立的虚拟用户ID，非openId。可以为空
	 */
	@RequestMapping("/tempQRcode")
	@ResponseBody
	public Map<String, Object> generateUserQRCode(@RequestParam("userId")String userId,@RequestParam("shadowUserId")String shadowUserId) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to generate temp QRcode for user.[id]"+userId);
		//用户同意授权后，通过code获得access token，其中也包含openid
		String shadowParams = "";
		if(shadowUserId!=null && shadowUserId.trim().length()>0) {
			shadowParams = "::"+shadowUserId.trim();
		}
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("User::"+userId+shadowParams,2592000);//有效期30天，注意场景值长度不能超过64
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		Map<String, Object> data = Maps.newHashMap();
		data.put("id", userId);
		data.put("shadowId", shadowUserId);
		data.put("url", url);
		result.put("status",true);
		result.put("data",data);
		result.put("description","User Temp QRCode created successfully");
		return result;
	}	
	

	/**
	 * 生成绑定账户二维码
	 * 该二维码仅用于采集工具、选品工具。达人扫码后前端能够得到绑定的openid
	 * 二维码格式为：Bind::UUID,
	 * 其中UUID为当前二维码识别标志，扫码完成后前端将根据该标志查询扫码用户的openId
	 */
	@RequestMapping("/bind-qrcode")
	@ResponseBody
	public Map<String, Object> generateBindQRCode() throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		String uuid = Util.md5_short(Util.get32UUID());//使用短码，长了会导致二维码生成场景值错误
		logger.debug("try to generate temp QRcode for binding.[uuid]"+uuid);
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("Bind::"+uuid,2592000);//有效期30天，注意场景值长度不能超过64
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		result.put("ticket", uuid);//返回前端，后续将根据该id查询扫码用户的openId
		result.put("url", url);
		result.put("status",true);
		result.put("description","Binding QRCode created successfully");
		return result;
	}	
	
	/**
	 * 生成即时注册二维码
	 * 该二维码仅用于未关注流量主进入系统页面时。达人扫码后前端能够得到绑定的openid
	 * 二维码格式为：Inst::xxxxxx,
	 * 其中xxxxxx为6位短码，作为当前二维码识别标志，扫码完成后前端将根据该标志查询扫码用户的openId
	 */
	@RequestMapping("/inst-qrcode")
	@ResponseBody
	public Map<String, Object> generateInstQRCode(@RequestParam("code")String code) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(code==null || code.trim().length()==0)//支持前端传入短码
			code = Util.get6bitCodeRandom();//使用6位短码，长了会导致二维码生成场景值错误
		logger.debug("try to generate inst QRcode for binding.[code]"+code);
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("Inst::"+code,2592000);//有效期30天，注意场景值长度不能超过64
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		result.put("ticket", code);//返回前端，后续将根据该id查询扫码用户的openId
		result.put("url", url);
		result.put("status",true);
		result.put("description","Inst QRCode created successfully");
		return result;
	}
	
	/**
	 * 生成saas小程序加入定制师二维码：从租户小程序扫码关注，并自动关联到租户
	 * 该二维码仅用于从租户小程序进入，扫码后注册定制师，并默认关联到入口租户。支持从多个租户小程序扫码进入。
	 * 二维码格式为：SaaS::xxxxxx,
	 * 其中xxxxxx为用户在租户小程序的openid，能够根据该数据查询得到wwCustomer信息，扫码完成后关联到平台openid
	 */
	@RequestMapping("/saas-qrcode")
	@ResponseBody
	public Map<String, Object> generateSaasQRCode(@RequestParam("openid")String openid) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(openid==null || openid.trim().length()==0)//支持前端传入短码
			openid = Util.get6bitCodeRandom();//使用6位短码，长了会导致二维码生成场景值错误
		logger.debug("try to generate inst QRcode for binding.[code]"+openid);
		WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket("SaaS::"+openid,2592000);//有效期30天，注意场景值长度不能超过64
		String url = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
		logger.debug("Got QRcode URL. [URL]",url);
		result.put("url", url);
		result.put("status",true);
		result.put("description","SaaS QRCode created successfully");
		return result;
	}
	
	/**
	 * 根据UUID查询扫码用户的openid
	 * 用于选品工具、采集工具达人扫码绑定
	 */
	@RequestMapping("/bind-openid")
	@ResponseBody
	public Map<String, Object> getBindingBrokerOpenid(@RequestParam("uuid")String uuid) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to get binding openid by uuid.[uuid]"+uuid);
		Object obj = CacheSingletonUtil.getInstance().getCacheData(uuid);
		result.put("status",obj==null?false:true);
		result.put("openid",obj);
		result.put("description",obj==null?"Not ready yet.":"ready");
		if(obj!=null){//仅返回一次，查询得到后即删除
			CacheSingletonUtil.getInstance().removeCacheData(uuid);
		}
		return result;
	}	
	
	@RequestMapping(value = "/notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendTemplateMessage(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		
		logger.debug("try to send notification message.[params]",params);
		
   	 	//发送通知信息给上级达人
		/**
{{first.DATA}}
会员昵称：{{keyword1.DATA}}
注册时间：{{keyword2.DATA}}
{{remark.DATA}}
		 */
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("parentBorkerOpenId"))//注意：场景值存放的是上级达人的openid
      	      .templateId(ilifeConfig.getMsgIdBroker())
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html")//跳转到团队页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", "新成员信息补充"))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("name")))
  	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
  	    		.addData(new WxMpTemplateData("remark", "为让团队变的更强大，点击协助TA完成设置并开始推荐。\n\n姓名："+params.get("name")+"\n电话："+params.get("phone")));
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
	 * 平台收益：beneficiary = platform
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
		titles.put("platform", "平台收益");
		
		Map<String, String> statusTitles = Maps.newHashMap();
		statusTitles.put("locked", "锁定-未达标");
		statusTitles.put("cleared", "待结算");
		
		String  beneficiaryType = params.get("beneficiary")!=null?params.get("beneficiary"):"未知";
		if(titles.get(params.get("beneficiary"))!=null)
			beneficiaryType = titles.get(params.get("beneficiary"));
		
		String platform  = params.get("platform")!=null?params.get("platform"):"保密";
		
		String remark = "";
		//remark+=params.get("item")!=null?"商品名称："+params.get("item"):"";
		remark+="收益类别："+beneficiaryType;
		remark+="\n来源平台："+platform;
		//remark+=params.get("orderTime")!=null?"\n订单时间："+params.get("orderTime"):"";
		remark+=params.get("seller")!=null?"\n团队成员："+params.get("seller"):"";
		remark+=params.get("status")!=null?"\n结算状态："+statusTitles.get(params.get("status")):"";
		
		if(remark.trim().length()==0)remark = "贡献越大，收益越多哦~~";
		
		logger.debug("try to send order notification message.[params]",params);
		
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");

		
		/**
你好，你已分销商品成功。
商品信息：日本贝亲旋转尼龙奶瓶刷
商品单价：11.00元
商品佣金：2.00元
分销时间：2015年7月21日 18:36
感谢你的使用。
		 */
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("brokerOpenid"))
      	      .templateId(ilifeConfig.getMsgIdOrder())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", "恭喜恭喜，有新订单成交"))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("item")))//商品信息
  	    		.addData(new WxMpTemplateData("keyword2", decimalFmt.format(Double.parseDouble(""+params.get("amountOrder")))))//订单金额
  	    		.addData(new WxMpTemplateData("keyword3", decimalFmt.format(Double.parseDouble(""+params.get("amountProfit")))))//收益金额
  	    		.addData(new WxMpTemplateData("keyword4", params.get("orderTime")))//订单成交时间
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
		
		Map<String, String> types = Maps.newHashMap();
		types.put("daily", "日报");
		types.put("weekly", "周报");
		types.put("montyly", "月报");
		types.put("yearly", "年报");
		
		String remark = "";
		remark += "\n浏览数："+params.get("views");
		remark += "\n分享数："+params.get("shares");
		remark += "\n意向数："+params.get("buys");
		remark += "\n订单数："+params.get("orders");
		remark += "\n团队人数："+params.get("members");
		remark += params.get("msg")!=null&&params.get("msg").toString().trim().length()>0?("\n"+params.get("msg")):"";
		
		logger.info("start send performance notification message.[params]",params);
		/**
{{title}}
结算类型：日报
结算时间：2016-05-30 12:00:00
结算金额：20元
感谢你的使用，请查看你的钱包
		 */
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("brokerOpenid"))
      	      .templateId(ilifeConfig.getMsgIdReport())
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/money.html")//订单汇总通知需要跳转到绩效界面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("brokerName")+"，"+titles.get(params.get("brokerName"))))
  	    		.addData(new WxMpTemplateData("keyword1", types.get(params.get("taskType")).toString()))
  	    		.addData(new WxMpTemplateData("keyword2", params.get("date")))
  	    		.addData(new WxMpTemplateData("keyword3", params.get("amount")))
  	    		.addData(new WxMpTemplateData("remark", remark));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	

	
	/**
	 * 发送导购数据同步消息：
新的数据同步任务完成。
会员昵称：淘宝数据同步
注册时间：2017-4-21 18:36
XXXX
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/data-sync-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendCpsSyncMsg(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send data sync msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		
		logger.info("start send data sync message.[params]",params);
		
		//20230803: 模板被拦截，临时停发消息
		String msgId = "";
//        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
//      	      .toUser(params.get("openid"))
//      	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
//      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/index.html")
//      	      .build();
//
//  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
//  	    		.addData(new WxMpTemplateData("keyword1", params.get("task")))
//  	    		.addData(new WxMpTemplateData("keyword2", params.get("time")))
//  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
//  	    msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 在购买置顶广告位、阅豆并完成支付后，发送付款成功消息：
{{first.DATA}}
商品名称：{{keyword1.DATA}}
付款金额：{{keyword2.DATA}}
订单状态：{{keyword3.DATA}}
下单时间：{{keyword4.DATA}}
备注：{{keyword5.DATA}}
{{remark.DATA}}

您好，您有新的订单付款成功！
商品名称：迪士尼水杯
付款金额：100
订单状态：订单付款
下单时间：2014年7月21日 18:36
备注：麻烦打包好一点，送礼用！
感谢你的使用。我们将尽快给您安排发货！
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/payment-success-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendWxPaymentNotify(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send payment success message.[params]",params);
		
		//默认直接发送给平台管理员：
		//对于租户管理员：需要手动传递openid
		String openid = ilifeConfig.getDefaultSystemBrokerOpenid();
		if( params.get("openid")!=null && params.get("openid").trim().length()>0) {
			openid = params.get("openid");
		}
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(openid)
      	      .templateId(ilifeConfig.getMsgIdPayment())//sf910GwObDADwsqbDkAWUA4nQ2j9Tso7QEo5bqbjF34
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/index.html")
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("product")))
  	    		.addData(new WxMpTemplateData("keyword2", params.get("amount")))
  	    		.addData(new WxMpTemplateData("keyword3", params.get("status")))
  	    		.addData(new WxMpTemplateData("keyword4", params.get("time")))
  	    		.addData(new WxMpTemplateData("keyword5", params.get("ext")))
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 发送清单推送消息
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/board-list-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> boardListBroadcast(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send data sync msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		
		logger.info("start send data sync message.[params]",params);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/boards.html?filter=all")
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/share.html?origin=board-all&filter=all")//需要通过微信中转，否则从模板消息进入无法获取达人信息和清单
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("task")))
  	    		.addData(new WxMpTemplateData("keyword2", params.get("time")))
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 发送Broker seed通知：在成功生成淘口令后发送。
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/broker-seed-success-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendBrokerSeedSuccessNotification(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send data sync msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		
		logger.info("start send broker seed notify message.[params]",params);
		//发送一条文字消息，包含转换后的淘口令
		double profitOrder = 0;
		try {
			profitOrder = Double.parseDouble(params.get("profitOrder").toString());
		}catch(Exception ex) {
			//do nothing
		}
		
		/**
		//不推送淘口令，口令能够在详情页查看
		StringBuffer sb = new StringBuffer();
		sb.append("亲，你的专属淘口令也准备好了哦，");
		sb.append(params.get("token") + params.get("title"));
		sb.append(profitOrder>0?"，店返￥"+params.get("profitOrder"):"");
		sb.append(",赶快分享吧~~");
		WxMpKefuMessage msg = WxMpKefuMessage
		  .TEXT()
		  .toUser(params.get("openid").toString())
		  .content(sb.toString())
		  .build();
		wxMpService.getKefuService().sendKefuMessage(msg);
		
		//**/
		
		//发送第二条文字消息：淘口令：大兄嘚，就别发那么多消息了，微信连续发20条就会停止发送了
		/**
		msg = WxMpKefuMessage
		  .TEXT()
		  .toUser(params.get("openid").toString())
		  .content(params.get("token")+"，复制这段文字购买 "+params.get("title")+" ~~")
		  .build();
		wxMpService.getKefuService().sendKefuMessage(msg);
		//**/
		
		//推送一条模板消息，能够进入详情页生成海报
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/broker/boards.html?filter=all")
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/share.html?origin=info2&id="+params.get("itemKey"))//需要通过微信中转，否则从模板消息进入无法获取达人信息和清单
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", "商品已经上架"))
  	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
  	    		.addData(new WxMpTemplateData("remark", (profitOrder>0?"店返￥"+params.get("profitOrder"):"")+" 进入详情可生成海报哦~~"));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  

 		//推送一条模板消息给管理员，通知上架成功
         templateMessage = WxMpTemplateMessage.builder()
       	      //.toUser("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc")//指定发送
        	  .toUser(ilifeConfig.getDefaultTechGuyOpenid())//指定发送
       	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
       	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/share.html?origin=info2&id="+params.get("itemKey"))//需要通过微信中转，否则从模板消息进入无法获取达人信息和清单
       	      .build();

   	    templateMessage.addData(new WxMpTemplateData("first", "从微信新增商品："+params.get("title")))
   	    		.addData(new WxMpTemplateData("keyword1", "达人商品上架成功"))
   	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
   	    		.addData(new WxMpTemplateData("remark", "店返："+profitOrder));
   	     msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	

	/**
	 * 发送Broker seed通知：超过3分钟，但仍未生成淘口令，则发送失败消息
	 * 
	 * 输入参数是一个Map。
	 */
	@RequestMapping(value = "/broker-seed-fail-notify", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendBrokerSeedFailureNotification(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send data sync msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}

		//发送一条文字消息告知：你发的啥玩意我找不到：取消发送。找都找不到了~也别发消息了~
		/**
		WxMpKefuMessage msg = WxMpKefuMessage
		  .TEXT()
		  .toUser(params.get("openid").toString())
		  .content("亲，这个淘口令对应的可能是下面的商品，也可以直接输入文字搜索试试看哦~~")
		  .build();
		wxMpService.getKefuService().sendKefuMessage(msg);
		//**/
		//推送一条消息给客服，需要关注该商品：不行啊，客服消息连续20条就会堵死的，改发送模板消息吧
		logger.info("start send operator seed fail notify message.[params]",params);
		/**
		WxMpKefuMessage msg = WxMpKefuMessage
		  .TEXT()
		  .toUser("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc")//指定发送
		  .content("商品未能成功上架，请查看。【淘口令】\n"+params.get("text")==null?"无":params.get("text"))
		  .build();
		wxMpService.getKefuService().sendKefuMessage(msg);
		//**/
		//推送一条模板消息给管理员，通知上架失败
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(ilifeConfig.getDefaultTechGuyOpenid())//指定发送
      	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/share.html?origin=info2&id="+params.get("itemKey"))//由于失败，没有itemKey
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("broker")+" 尝试从微信查询商品"))
  	    		.addData(new WxMpTemplateData("keyword1", "达人商品上架失败"))
  	    		.addData(new WxMpTemplateData("keyword2", dateFormatLong.format(new Date())))
  	    		.addData(new WxMpTemplateData("remark", params.get("text")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
		
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	
	/**
	 * 通过公众号向所有关注用户发送模板消息。
	 * 注意：需要谨慎使用，可能会导致模板消息被封 
	 * 输入参数：
        {
            type:item/list/board,//支持单个商品、搜索链接、board，当前仅支持前两种。根据关键字搜索得到后返回
            format:text/image/url,//预留：能够选择模板类型，当前仅提供固定模板消息
            id:xxx,//ID：仅对于item或board生效，如果传递则直接获取对应内容
            title:xxx,//标题：作为通知模板标题
            remark:xxx,//详情：作为模板消息备注
            keywords:"",//搜索关键字，用于搜索：不执行搜索
        }
      * 消息模板：
			{{first.DATA}}
			项目名称：{{keyword1.DATA}}
			工单类型：{{keyword2.DATA}}
			工作内容：{{keyword3.DATA}}
			日期时间：{{keyword4.DATA}}
			{{remark.DATA}}
	 */
	@RequestMapping(value = "/notify-mp-company", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageCompany(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to send occasion notification message.[action]"+json.toJSONString(),json);
		//根据具体action类型组装消息并发送：公众号只有模板消息，根据关键词搜索后得到推送
		String first = json.getString("title");
		String remark = json.getString("remark");
		String url = ilifeUrlPrefix+"/index.html";
		String keyword1="",keyword2="",keyword3="",keyword4 = "";
		boolean valid = false;
		if("item".equals(json.getString("type"))) {//是单条发送
			if(json.getString("id")!=null&&json.getString("id").trim().length()>0) {//是指定条目
				//查询arangodb得到条目内容并组装消息
				Map<String,Object> props = helper.getItemById(json.getString("id"));
				if(props != null) {//如果为空不做任何处理
					keyword1 = ""+props.get("title");
					keyword2 = "来源："+props.get("source");
					keyword3 = "分享到适合的客户或关心的人";
					keyword4 = "越快越好，手慢无";
					url = ilifeUrlPrefix+"/info2.html?id="+json.getString("id");
					valid = true;
				}
			}else {//使用关键字搜索，取第一条组装消息
				JSONObject items = helper.searchItemByKeyword(json.getString("keyword")==null?"":json.getString("keyword"));
				if(items.getJSONObject("hits").getJSONArray("hits").size()>0) {
					JSONObject item = items.getJSONObject("hits").getJSONArray("hits").getJSONObject(0);
					keyword1 = item.getString("title");
					keyword2 = "来源："+item.getString("source");
					keyword3 = "分享到适合的客户或关心的人";
					keyword4 = "越快越好，手慢无";
					url = ilifeUrlPrefix+"/info2.html?id="+json.getString("id");
					valid = true;
				}
			}
		}else if("board".equals(json.getString("type"))) {//发送清单
			//查询得到清单，并组织消息
			if(json.getString("id")!=null&&json.getString("id").trim().length()>0) {//是指定条目
				JSONObject board = helper.getBoardById(json.getString("id"));
				if(board != null ) {
					keyword1 = board.getString("title");
					keyword2 = "精选清单";
					keyword3 = "分享到适合的客户或关心的人";
					keyword4 = "长期有效，还可以定制自己的清单哦~~";
					url = ilifeUrlPrefix+"/board2-waterfall.html?id="+json.getString("id");
					valid = true;
				}
			}else {//否则根据关键字生成新的清单：以关键字前10条为item添加到列表
				//首先查出10条符合的结果
				JSONObject items = helper.searchItemByKeyword(json.getString("keyword")==null?"":json.getString("keyword"),10);
				if(items.getJSONObject("hits").getJSONArray("hits").size()>0) {
					//新建一个board
					JSONObject board = helper.createNewBoard(json.getString("title"),json.getString("keyword")).getJSONObject("data");
					if( board!=null && board.getString("id")!=null) {//仅在board创建成功后再开始
						//获取10条结果
						JSONArray itemArray = items.getJSONObject("hits").getJSONArray("hits");
						for(int k=0;k<10&&k<itemArray.size();k++) {
							JSONObject item = itemArray.getJSONObject(k);
							//新建boardItem
							helper.addBoardItem(board, item.getString("_id"), item.getString("title"), item.getString("summary"));
						}
						keyword1 = board.getString("title");
						keyword2 = "新建精选清单";
						keyword3 = "根据内容分享到适合的客户或关心的人";
						keyword4 = "已经为你准备好，关于 "+json.getString("keyword")+" 的清单，可以直接分享，也可以继续定制哦~~";
						url = ilifeUrlPrefix+"/board2-waterfall.html?id="+board.getString("id");
						valid = true;
					}
				}
			}
		}else if(json.getString("keyword")!=null) {//只要有keyword就直接返回列表
			keyword1 = "新商品上架";
			keyword2 = "来源于天猫、淘宝、京东、拼多多等多个";
			keyword3 = "进入查看并分享到适合的客户或关心的人";
			keyword4 = "越快越好，手慢无";
			url = ilifeUrlPrefix+"/index.html?keyword="+json.getString("keyword");
			valid = true;
		}
		
		//仅对查询信息的结果发送消息
		if(valid) {
			//推送一条模板消息给管理员，通知上架失败
	        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
	      	      .toUser(ilifeConfig.getDefaultTechGuyOpenid())//TODO：当前测试阶段，仅指定发送
	      	      .templateId(ilifeConfig.getMsgIdGuide())
	      	      .url(url)
	      	      .build();

	  	    templateMessage.addData(new WxMpTemplateData("first", first))
	  	    		.addData(new WxMpTemplateData("keyword1", keyword1))
	  	    		.addData(new WxMpTemplateData("keyword2", keyword2))
	  	    		.addData(new WxMpTemplateData("keyword3", keyword3))
	  	    		.addData(new WxMpTemplateData("keyword4", dateFormatLong.format(new Date())))
	  	    		.addData(new WxMpTemplateData("remark", remark));
	  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
			
			result.put("status", true);
			result.put("msgId", msgId);
		}else {
			result.put("status", false);
			result.put("msg", "无法按照指令发送模板消息，请检查参数是否正确。");
		}

		return result;
	}
	
	@RequestMapping(value = "/notify-mp-broker", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageBroker(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to send occasion notification message.[action]",json);
		result.put("status", false);
		result.put("msg", "TBC");
		return result;
	}
	
	@RequestMapping(value = "/notify-mp-customer", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageCustomer(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to send occasion notification message.[action]",json);
		result.put("status", false);
		result.put("msg", "TBC");
		return result;
	}
	
	/**
	 * 通过公众号向流量主发送模板消息。提醒指定时间段的时候，每天或每周等
	 * 注意：需要谨慎使用，可能会导致模板消息被封 
	 * 输入参数：
        {
			openid:xxx,
			title:xxx,
			timestamp:yyyy-MM-dd HH:mm:ss
			points:xxx,
			remark:xxx
        }
      * 消息模板：
			{{first.DATA}}
			截止时间：{{keyword1.DATA}}
			总资产：{{keyword2.DATA}}
			{{remark.DATA}}
	 */
	@RequestMapping(value = "/notify-mp-publisher", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessagePublisher(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send publisher notify message.[params]",json);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(json.getString("openid"))
      	      .templateId(ilifeConfig.getMsgIdPublisher())//sf910GwObDADwsqbDkAWUA4nQ2j9Tso7QEo5bqbjF34
      	      .url("https://www.biglistoflittlethings.com/ilife-web-wx/publisher/articles.html")
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", json.getString("title")))
  	    		.addData(new WxMpTemplateData("keyword1", json.getString("timestamp")))
  	    		.addData(new WxMpTemplateData("keyword2", json.getString("points")))
  	    		.addData(new WxMpTemplateData("remark", json.getString("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 通过公众号向流量主发送模板消息。提醒指定时间段的时候，每天或每周等
	 * 注意：需要谨慎使用，可能会导致模板消息被封 
	 * 输入参数：
        {
			openid:xxx,
			title:xxx,
			timestamp:yyyy-MM-dd HH:mm:ss
			points:xxx,
			remark:xxx,
			color:xxx
        }
      * 消息模板：
			{{first.DATA}} title
			用户名：{{keyword1.DATA}} nickname
			统计时间：{{keyword2.DATA}} timestamp
			统计数据：{{keyword3.DATA}} points 阅豆
			{{remark.DATA}} remark
	 */
	@RequestMapping(value = "/notify-mp-publisher-rank", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessagePublisherRank(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send publisher notify message.[params]",json);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(json.getString("openid"))
      	      .templateId("QKSw7Gb7_V5FGhNW0KOw0bv-fBd5V38JLIX-PKfcdKk")
      	      .url("https://www.biglistoflittlethings.com/ilife-web-wx/publisher/articles.html")
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", json.getString("title")))
  	    		.addData(new WxMpTemplateData("keyword1", json.getString("nickname")))
  	    		.addData(new WxMpTemplateData("keyword2", json.getString("timestamp")))
  	    		.addData(new WxMpTemplateData("keyword3", json.getString("points")))
  	    		.addData(new WxMpTemplateData("remark", json.getString("remark"),json.getString("color")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	
	
	/**
	 * 发送开白请求，响应开白回复
	 * 
	 * 	{{first.DATA}} title 接收到新的开白申请
		原文标题：{{keyword1.DATA}}  request:文章标题或公众号名称 
		原文时间：{{keyword2.DATA}}  requestTime：yyyy-MM-dd HH:mm:ss
		{{remark.DATA}} remark：备注。公众号：xxx，开白类型：xxx 请即时处理
	 * 
	 * @param json
	 * @return
	 * @throws WxErrorException
	 * @throws IOException
	 */
	@RequestMapping(value = "/notify-mp-forward", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageForward(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send forward notify message.[params]",json);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(json.getString("openid"))
      	      .templateId("pAsLmwxm8zkKBHRh9fOTjjwrQLGcM1d71Hp6oG4DWtU")//注意哦，是hard code哦
      	      .url(json.getString("redirectUrl"))
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", json.getString("title")))
  	    		.addData(new WxMpTemplateData("keyword1", json.getString("request")))
  	    		.addData(new WxMpTemplateData("keyword2", json.getString("requestTime")))
  	    		.addData(new WxMpTemplateData("remark", json.getString("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}
	
	/**
	 * 发送账号申请审核通知
	 * 
	 * 	{{first.DATA}}
		申请结果：{{keyword1.DATA}}
		审核信息：{{keyword2.DATA}}
		{{remark.DATA}}
	 * 
	 * @param json
	 * @return
	 * @throws WxErrorException
	 * @throws IOException
	 */
	@RequestMapping(value = "/notify-mp-badge", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageBadge(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send forward notify message.[params]",json);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(json.getString("openid"))
      	      .templateId("x3aTDJqolbFPO8zqzHglmLZgaO1yW_9sbo42Wa5B7_4")//注意哦，是hard code哦
      	      .url(json.getString("redirectUrl"))
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", json.getString("title")))
  	    		.addData(new WxMpTemplateData("keyword1", json.getString("respond")))
  	    		.addData(new WxMpTemplateData("keyword2", json.getString("respondMsg")))
  	    		.addData(new WxMpTemplateData("remark", json.getString("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	
	
	/**
	 * 达人注册后，发送上级达人通知信息。通过jsonObject传参，包括：
	 * @param name 达人昵称，或姓名
	 * @param openid 上级达人的openid
	 * @param title 通知标题
	 * @param url 跳转地址。可以为空，使用默认值
	 * @param remark 备注 。可以为空，使用默认值
	 * @return
	 * @throws WxErrorException 
	 * buildParentBrokerNotifyMsg(,userWxInfo.getNickname(),ilifeConfig.getDefaultParentBrokerOpenid(),"")
	 */
	@RequestMapping(value = "/notify-parent-broker", method = RequestMethod.POST)
	@ResponseBody
	public  Map<String, Object> sendParentBrokerNotifyMsg(@RequestBody JSONObject json) throws WxErrorException {
		Map<String, Object> result = Maps.newHashMap();
		logger.debug("try to send occasion notification message.[parent broker openid]"+json.getString("openid")+"[broker]"+json.getString("name"));
		result.put("status", false);
		
		String remark = "请进入团队列表查看。";
		if(json.getString("remark")!=null && json.getString("remark").trim().length()>0) {
			remark = json.getString("remark");
		}
		
		String url = "http://www.biglistoflittlethings.com/ilife-web-wx/broker/team.html";
		if(json.getString("url")!=null && json.getString("url").trim().length()>0) {
			url = json.getString("url");
		}
		
      WxMpTemplateMessage msg = WxMpTemplateMessage.builder()
    	      .toUser(json.getString("openid"))
    	      .templateId(ilifeConfig.getMsgIdBroker())//oWmOZm04KAQ2kRfCcU-udGJ0ViDVhqoXZmTe3HCWxlk
    	      .url(url)
    	      .build();
      msg.addData(new WxMpTemplateData("first", json.getString("title")))
    	    		.addData(new WxMpTemplateData("keyword1", json.getString("name")))
    	    		.addData(new WxMpTemplateData("keyword2", dateFormat.format(new Date())))
    	    		.addData(new WxMpTemplateData("remark", remark));
      String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(msg); 
      result.put("status", true);
      result.put("msg", "parent broker notify msg sent successfully.");
      return result;
	}
	
	/**
	 * 在租户初次开通服务后推送管理员账户信息：
        {
			openid:xxx,
			title:xxx,
			company: xxx,
			package:xxx,
			username:xxx,
			password:xxx,
			expireOn:yyyy-MM-dd HH:mm:ss
        }
      * 消息模板：
			{{first.DATA}} title
			开通企业：{{keyword1.DATA}} company
			开通套餐：{{keyword2.DATA}} package
			开通账号：{{keyword3.DATA}} username
			账号密码：{{keyword4.DATA}} password
			套餐期限：{{keyword5.DATA}} expireOn
			{{remark.DATA}}
	 */
	@RequestMapping(value = "/notify-mp-tenant-ready", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendMpTemplateMessageTenantReady(@RequestBody JSONObject json) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		logger.info("start send tenant notify message.[params]",json);
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(json.getString("openid"))
      	      .templateId("Fchqo70UJQmcwH-PT-QGtc45_0uP6fwuRVK7pa7Y0J0")
      	      .url("https://air.biglistoflittlethings.com")
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", json.getString("title")))
  	    		.addData(new WxMpTemplateData("keyword1", json.getString("company")))
  	    		.addData(new WxMpTemplateData("keyword2", json.getString("package")))
  	    		.addData(new WxMpTemplateData("keyword3", json.getString("username")))
  	    		.addData(new WxMpTemplateData("keyword4", json.getString("password")))
  	    		.addData(new WxMpTemplateData("keyword5", json.getString("expireOn")))
  	    		.addData(new WxMpTemplateData("remark", json.getString("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	


	/**
	 * 师家认证状态变化时时推送通知，包括新增、状态修改。格式为：
		你申请的师家认证通过审核。title。状态对应：pending 待审核、rejected 已拒绝、approved 通过审核
		申请结果：通过审核 status。与状态对应：pending 待审核、rejected 已拒绝、approved 通过审核
		审核信息：健康管理-健管师。content。根据 bizType 及certificateType 组装，如认证类型：定制旅行-定制师
		可前往师家端发起商家签约并开始接受服务派单~ remark 状态为 approved 时内容为 「可前往师家端发起商家签约并开始接受服务派单～」，否则为「请进入师家端补充修改～」
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "恭喜，你申请的师家认证通过审核。",
	 status: "通过审核",
	 content: "健康管理-健管师",
	 remark: "可前往师家端发起商家签约并开始接受服务派单~"
	 }
	 */
	@RequestMapping(value = "/notify-broker-register", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyBrokerRegister(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplBrokerRegister())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("status")))//审核结果
  	    		.addData(new WxMpTemplateData("keyword2", params.get("content")))//认证类型
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	
	

	/**
	 * 师家签约时推送通知，包括新增、状态修改。格式为：
		你的签约申请通过审核。title。状态对应：pending 待审核、rejected 已拒绝、approved 通过审核
		申请结果：通过审核 status。与状态对应：pending 待审核、rejected 已拒绝、approved 通过审核
		审核信息：签约商家 美途假日。content。提供签约商家名称，优先简称
		在有新的服务派单时，将接收到通知，并能够接受或拒绝订单~ remark 状态为 approved 时内容为 「在有新的服务派单时，将接收到通知，并能够接受或拒绝订单～」，否则为「请等待审核结果或联系商家完成签约审核～」
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "恭喜，你的签约申请通过审核。",
	 status: "通过审核",
	 content: "签约商家 美途假日",
	 remark: "在有新的服务派单时，将接收到通知，并能够接受或拒绝订单~"
	 }
	 */
	@RequestMapping(value = "/notify-broker-contract", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyBrokerContract(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplBrokerContract())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("status")))//审核结果
  	    		.addData(new WxMpTemplateData("keyword2", params.get("content")))//认证类型
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}		


	/**
	 * 生成服务派单或状态变化时推送通知，包括新增、状态修改。格式为：
		新订单通知 title 
		商家名称：张亮麻辣烫 merchant 派单商家名称，优先为简称
		服务名称：取物 content 服务订单名称
		订单金额：199.80 price 服务订单金额
		预约时间：2023-04-14 scheduleDate 服务订单预约时间
		订单状态：已接单 status 服务订单状态
	 * 
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "你有新的服务订单通知。",
	 merchant: "美途假日",
	 content: "取物",
	 price: 199.80,
	 scheduleDate: "2025-08-09 14:00",
	 status: "已接单",
	 remark: "请前往师家端-订单管理查看"
	 }
	 */
	@RequestMapping(value = "/notify-service-order", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyServiceOrder(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplServiceOrder())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("thing31", params.get("merchant")))//派单商家
  	    		.addData(new WxMpTemplateData("thing9", params.get("content")))//服务内容
  	    		.addData(new WxMpTemplateData("amount3", params.get("price")))//订单金额
  	    		.addData(new WxMpTemplateData("time14", params.get("scheduleDate")))//预约时间
  	    		.addData(new WxMpTemplateData("phrase13", params.get("status")))//订单状态
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}		


	/**
	 * 产生CPS 订单时推送通知，包括师家分销、用户分销。格式为：
		你好，你已分销商品成功。title
		商品信息：日本贝亲旋转尼龙奶瓶刷 content
		商品单价：11.00元 price
		商品佣金：2.00元 commission
		分销时间：2015年7月21日 18:36 orderTime
		感谢你的使用。 remark
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "恭喜，你有新的分销订单。",
	 content: "日本贝亲旋转尼龙奶瓶刷",
	 price: 11.00,
	 commission: 2.00,
	 orderTime: "2025-08-02 11:22:33",
	 remark: "订单完成后将自动结算，请留意结算通知"
	 }
	 */
	@RequestMapping(value = "/notify-cps-order", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyCpsOrder(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplCpsOrder())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("content")))//商品信息
  	    		.addData(new WxMpTemplateData("keyword2", decimalFmt.format(Double.parseDouble(""+params.get("price")))))//订单金额
  	    		.addData(new WxMpTemplateData("keyword3", decimalFmt.format(Double.parseDouble(""+params.get("commission")))))//收益金额
  	    		.addData(new WxMpTemplateData("keyword4", params.get("orderTime")))//订单成交时间
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	
	


	/**
	 * CPS 订单结算成功通知，格式为：
		您有一笔新的结算通知 title
		结算内容：坚持练习写毛笔字活动奖赏金 content
		结算金额：5.64元 commission
		结算时间：2018年07月01日 settleTime
		赚钱事小，培养好习惯才是最终目标。继续新挑战吧！ remark
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "恭喜，您有一笔新的结算佣金到账。",
	 content: "坚持练习写毛笔字活动奖赏金",
	 commission: 5.64,
	 settleTime: "2025-08-02 11:22:33",
	 remark: "订单佣金已经结算且转入微信账户，请留意查收。"
	 }
	 */
	@RequestMapping(value = "/notify-cps-settle", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyCpsSettle(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplSettle())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("keyword1", params.get("content")))//商品信息
  	    		.addData(new WxMpTemplateData("keyword2", decimalFmt.format(Double.parseDouble(""+params.get("commission")))))//收益金额
  	    		.addData(new WxMpTemplateData("keyword3", params.get("settleTime")))//订单成交时间
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	


	/**
	 * 在生成订单时向下单用户推送通知，包括新增、状态修改。格式为：
		新订单通知 title 
		服务名称：取物 content  对应订单内容名称
		服务时间：2022年10月11日 10:00 scheduleDate 预约时间。无预约时间采用订单更新时间
		商家名称：张亮麻辣烫 merchant 商家名称，优先传递简称
		订单状态：已接单 status 
		订单编号：2022112900000022 orderId 传递6 位订单尾号
	 * 
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "你有新的订单通知。",
	 content: "取物",
	 scheduleDate: "2025-08-09 14:00",
	 merchant: "美途假日",
	 status: "已接单",
	 orderId: "xxxx", 
	 remark: "请前往小程序-我的订单界面查看"
	 }
	 */
	@RequestMapping(value = "/notify-user-order", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyUserOrder(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplOrder())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("thing9", params.get("content")))//订单内容
  	    		.addData(new WxMpTemplateData("time32", params.get("scheduleDate")))//预约时间
  	    		.addData(new WxMpTemplateData("thing31", params.get("merchant")))//收单商家
  	    		.addData(new WxMpTemplateData("phrase13", params.get("status")))//订单状态
  	    		.addData(new WxMpTemplateData("character_string2", params.get("orderId")))//订单尾号
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}		

	/**
	 * 在订单日程开始前推送通知，仅推送一次。格式为：
		新订单通知 title 
		服务名称：取物 content  对应订单内容名称
		服务时间：2022年10月11日 10:00 scheduleDate 预约时间。无预约时间采用订单更新时间
		商家名称：张亮麻辣烫 merchant 商家名称，优先传递简称
		订单状态：已接单 status 
		订单编号：2022112900000022 orderId 传递6 位订单尾号
	 * 
	 * 
	 * 目标用户：openid 在服务号下的 openid
	 * 
	 * 输入参数是一个Map。如：
	 {
	 openid: "xxxxx",
	 title: "你有订单日程即将开始。",
	 content: "取物",
	 scheduleDate: "2025-08-09 14:00",
	 merchant: "美途假日",
	 status: "待开始",
	 orderId: "xxxx", 
	 remark: "请前往小程序-日程界面查看"
	 }
	 */
	@RequestMapping(value = "/notify-user-schedule", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> sendNotifyUserSchedule(@RequestBody Map<String,String> params) throws WxErrorException, IOException {
		Map<String, Object> result = Maps.newHashMap();
		if(params.get("openid")==null || params.get("openid").toString().trim().length()==0) {//如果没有openid则直接跳过
			logger.error("cannot send clearing notification msg without openid.[params]",params);
	  	     result.put("status", false);
	  	     return result;
		}
		logger.info("start send notification msg.[params]",params);
		DecimalFormat decimalFmt = new DecimalFormat("###################.###########");
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgTplOrder())
      	      //.url("http://www.biglistoflittlethings.com/ilife-web-wx/cps/index.html")//订单通知不跳转到详情页面
      	      .build();

  	    templateMessage.addData(new WxMpTemplateData("first", params.get("title")))
  	    		.addData(new WxMpTemplateData("thing9", params.get("content")))//订单内容
  	    		.addData(new WxMpTemplateData("time32", params.get("scheduleDate")))//预约时间
  	    		.addData(new WxMpTemplateData("thing31", params.get("merchant")))//收单商家
  	    		.addData(new WxMpTemplateData("phrase13", params.get("status")))//订单状态
  	    		.addData(new WxMpTemplateData("character_string2", params.get("orderId")))//订单尾号
  	    		.addData(new WxMpTemplateData("remark", params.get("remark")));
  	     String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);  
  	     
  	     result.put("status", true);
  	     result.put("msgId", msgId);
  	     return result;
	}	
}
