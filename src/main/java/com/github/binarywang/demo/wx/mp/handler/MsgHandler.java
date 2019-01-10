package com.github.binarywang.demo.wx.mp.handler;

import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.config.ElasticSearchConfig;
import com.github.binarywang.demo.wx.mp.service.WeixinService;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import me.chanjar.weixin.mp.builder.outxml.NewsBuilder;

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
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Binary Wang
 */
@Component
public class MsgHandler extends AbstractHandler {
  @Autowired
  private ElasticSearchConfig esConfig;	
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) {

    WeixinService weixinService = (WeixinService) wxMpService;

    if (!wxMessage.getMsgType().equals(WxConsts.XmlMsgType.EVENT)) {
      //TODO 可以选择将消息保存到本地
    }

    //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
    if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
      && weixinService.hasKefuOnline()) {
      return WxMpXmlOutMessage
        .TRANSFER_CUSTOMER_SERVICE().fromUser(wxMessage.getToUser())
        .toUser(wxMessage.getFromUser()).build();
    }
    
    //根据关键词搜索符合内容
    String keyword = wxMessage.getContent();
    String xml = defaultItem();
    try {
    		xml = search(keyword);
    }catch(Exception ex) {
    		logger.error("Error occured while search items.[keyword]"+keyword,ex);
    }
    XStream xstream = new XStream();
    xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
	WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
	return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
	        .toUser(wxMessage.getFromUser()).build();
  }
  
  /**
   * 
$itemTpl = " <item>
		 <Title><![CDATA[%s]]></Title> 
		 <Description><![CDATA[%s]]></Description>
		 <PicUrl><![CDATA[%s]]></PicUrl>
		 <Url><![CDATA[%s]]></Url>
		 </item>";
   */
  private String item(String title,String description,String picUrl,String url) {
	  StringBuffer sb = new StringBuffer();
	  sb.append("<item>");
	  sb.append("<title>"+title+"</title>");
	  sb.append("<description>"+description+"</description>");
	  sb.append("<picUrl>"+picUrl+"</picUrl>");
	  sb.append("<url>"+url+"</url>");
	  sb.append("</item>");
	  return sb.toString();
  }
  
  private String search(String keyword) throws UnknownHostException {
	  TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
		        .addTransportAddress(new TransportAddress(InetAddress.getByName("host2"), 9300));
	  Map<String, Object> template_params = new HashMap<>();
	  //查询总共符合的数量
	  template_params.put("keyword", keyword);
	  SearchResponse sr = new SearchTemplateRequestBuilder(client)
		        .setScript(esConfig.getQueryCount())//查询符合条件结果总数
		        .setScriptType(ScriptType.INLINE)    
		        .setScriptParams(template_params)    
		        .setRequest(new SearchRequest())     
		        .get()                               
		        .getResponse(); 
	  long total = sr.getHits().getTotalHits();
	  //查询指定条目
	  long which = System.currentTimeMillis() % total;//随机获取一条
	  template_params.put("which", which);//更新起止条数
	  sr = new SearchTemplateRequestBuilder(client)
		        .setScript(esConfig.getQuery())//查询一条
		        .setScriptType(ScriptType.INLINE)    
		        .setScriptParams(template_params)    
		        .setRequest(new SearchRequest())     
		        .get()                               
		        .getResponse(); 
	  //获取返回结果
	  String result = defaultItem();
	  if(sr.getHits().totalHits>0) {
		  SearchHit hit = sr.getHits().getAt(0);
		  String title = hit.field("title").getValue();
		  String description = hit.field("summary").getValue().toString();
		  String picUrl = ((String[])hit.field("images").getValue())[0];
		  String url = "http://www.biglistoflittlethings.com/list-web-wx/info2.html?id="+hit.field("_key").getValue().toString();
		  result = item(title,description,picUrl,url);
	  }
	  client.close();	 
	  return result;
  }
  
  private String defaultItem() {
  	final int i = (int)( System.currentTimeMillis() % 12 );//取一个随机数用于随机显示LOGO图片
  	String iStr = (""+(100+i)).substring(1);//格式化：结果为00，01，，，，11，12
	String title = "小确幸，大生活";
	String description = "Life is all about having a good time.";
	String picUrl = "http://www.shouxinjk.net/list/images/logo"+iStr+".jpeg";
	String url = "http://www.biglistoflittlethings.com/list-web-wx";
	return item(title,description,picUrl,url);
  }

}
