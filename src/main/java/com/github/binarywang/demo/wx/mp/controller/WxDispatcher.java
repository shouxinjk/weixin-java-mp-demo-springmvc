package com.github.binarywang.demo.wx.mp.controller;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

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
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.google.common.collect.Maps;
import com.ilife.util.SxHelper;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
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
	  private SxHelper helper;
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

  	    templateMessage.addData(new WxMpTemplateData("first", "有新成员加入团队"))
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
		statusTitles.put("locked", "锁定-未达标");
		statusTitles.put("cleared", "待结算");
		
		Map<String, String> platforms = Maps.newHashMap();
		platforms.put("taobao", "淘宝");
		platforms.put("tmall", "天猫");
		platforms.put("fliggy", "飞猪");
		platforms.put("lvmama", "驴妈妈");
		platforms.put("tongcheng", "同程旅行");
		platforms.put("ctrip", "携程");
		platforms.put("jd", "京东");
		platforms.put("dangdang", "当当网");
		platforms.put("dhc", "DHC");
		platforms.put("amazon", "亚马逊");
		
		String remark = "";
		//remark+=params.get("item")!=null?"商品名称："+params.get("item"):"";
		remark+=params.get("beneficiary")!=null?"收益类别："+titles.get(params.get("beneficiary")):"";
		remark+=params.get("platform")!=null?"\n来源平台："+platforms.get(params.get("platform")):"";
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
		
        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
      	      .toUser(params.get("openid"))
      	      .templateId(ilifeConfig.getMsgIdTask())//ey5yiuOvhnVN59Ui0_HdU_yF8NHZSkdcRab2tYmRAHI
      	      .url("http://www.biglistoflittlethings.com/ilife-web-wx/index.html")
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
       	      .toUser("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc")//指定发送
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
      	      .toUser("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc")//指定发送
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
				JSONObject items = helper.searchByKeyword(json.getString("keyword")==null?"":json.getString("keyword"));
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
				JSONObject items = helper.searchByKeyword(json.getString("keyword")==null?"":json.getString("keyword"),10);
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
	      	      .toUser("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc")//TODO：当前测试阶段，仅指定发送
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
	
}
