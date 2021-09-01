package com.ilife.util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arangodb.entity.BaseDocument;
import com.github.binarywang.demo.wx.mp.config.iLifeConfig;
import com.google.common.collect.Maps;
@Service
public class SxHelper {
		private Logger logger = LoggerFactory.getLogger(getClass());
		  @Autowired
		  private iLifeConfig ilifeConfig;
		  
		@Value("#{extProps['es.url']}") String esUrl;
		@Value("#{extProps['es.query']}") String esQuery;
		@Value("#{extProps['es.queryByDistance']}") String esQueryByDistance;
		@Value("#{extProps['msg.item.url.prefix']}") String itemUrlPrefix;
		
	    ArangoDbClient arangoClient;
	    
		@Value("#{extProps['arangodb.host']}") String host;
		@Value("#{extProps['arangodb.port']}") String port;
		@Value("#{extProps['arangodb.username']}") String username;
		@Value("#{extProps['arangodb.password']}") String password;
		@Value("#{extProps['arangodb.database']}") String database;
	  
		private ArangoDbClient getArangoClient() {
			if(arangoClient == null) {
				arangoClient = new ArangoDbClient(host,port,username,password,database);
			}
			return arangoClient;
		}
		
		private void closeArangoClient() {
			arangoClient.close();
		}
		
	/**
	 * 向ElasticSearch发起搜索。并返回hits
	 * @param keyword
	 * @return
	 */
	  public JSONObject searchByKeyword(String keyword) {
		  Map<String,String> header = Maps.newHashMap();
          header.put("Content-Type","application/json");
          header.put("Authorization","Basic ZWxhc3RpYzpjaGFuZ2VtZQ==");

          String query = esQuery.replace("__keyword", keyword);
          logger.debug("try to search by query. " + query);
		  JSONObject data = JSONObject.parseObject(query);
		  
		  JSONObject result = HttpClientHelper.getInstance().post(esUrl, data,header);
		  logger.debug("got result. " + result);
		  return result;
	  }
	  
	  //根据位置发起搜索
	  public JSONObject searchByLocation(String lat,String lon) {
		  Map<String,String> header = Maps.newHashMap();
          header.put("Content-Type","application/json");
          header.put("Authorization","Basic ZWxhc3RpYzpjaGFuZ2VtZQ==");

          String query = esQueryByDistance.replace("__lat", lat).replace("__lon", lon);
          logger.debug("try to search by location.[query] " + query);
		  JSONObject data = JSONObject.parseObject(query);
		  
		  JSONObject result = HttpClientHelper.getInstance().post(esUrl, data,header);
		  logger.debug("got result. " + result);
		  return result;
	  }
	  
	  //判断输入是否包含有URL
	  public String getUrl(String text) {
		  String pattern = "(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		 try {
		     Pattern r = Pattern.compile(pattern);
		     Matcher m = r.matcher(text);
		     if (m.find()) {
		         logger.debug("\n\nmatch: " + m.group());
		         return m.group();
		     }
		 }catch(Exception ex) {
		 	logger.error("Failed to match url.",ex);
		 }
		 return "";		  
	  }
	  
	  //根据输入URL判定是否是支持范围，并且转换为标准URL
	  //对于不支持的情况，返回空值
	  public String convertUrl(String url) {
		  //TODO 调用远端服务检查是否支持
		  String remote = ilifeConfig.getSxApi()+"/mod/linkTemplate/rest/convert";
		  Map<String,String> params = Maps.newHashMap();
		  params.put("url", url);
		  JSONObject result = HttpClientHelper.getInstance().get(remote, params,null);
		  logger.error("got result.",result);
		  if(result.getBooleanValue("success")) {
			  return result.getString("url");
		  }
		  return "";
	  }
	  
	  /**
	   * 判定是否包含有淘口令，有则返回口令，否则返回null
	   * @param text
	   * @return
	   */
	  public String parseTaobaoToken(String text) {
//	      String pattern = "([\\p{Sc}])\\w{8,12}([\\p{Sc}])";
		  String pattern = "[a-zA-Z0-9]{11}";
		 try {
		     Pattern r = Pattern.compile(pattern);
		     Matcher m = r.matcher(text);
		     if (m.find()) {
		    	 String token = m.group();
		         logger.debug("match: " + token);
		         return token;
		     }
		 }catch(Exception ex) {//不知道什么鬼，这里竟然会报NullPointerException
		 	logger.error("Failed to match taobao token.",ex);
		 }
		 return null;
	  }
	  
	  /**
	   * 从淘口令字符串解析得到商品详情。
	   * 规则为：如果淘口令内包含有【】则取其中间内容
	   * 否则将淘口令替换掉返回
	   * @param text
	   * @return
	   */
	  public String getKeywordFromTaobaoToken(String text) {
//	         String pattern = "([\\p{Sc}])\\w{8,12}([\\p{Sc}])";
	         String pattern = "[a-zA-Z0-9]{11}";
	         String keyword = text.replaceAll(pattern, "").replaceAll("\\n", " ");
	         if(keyword.indexOf("【")>-1 && keyword.indexOf("】")>-1 ) {
	        	 	pattern = "(【[^】]+】)";
		         try {
			         Pattern r = Pattern.compile(pattern);
			         Matcher m = r.matcher(text);
			         if (m.find()) {
			        	 	 keyword = m.group().replace("【", "").replace("】", "");
			             logger.debug("match: " + m.group());
			         }
		         }catch(Exception ex) {
		        	 	logger.error("Failed to match taobao token.",ex);
		         }
	         }
	         logger.debug("got keyword."+keyword);
	         return keyword;
	  }

	  /**
	   * 将包含有淘口令的字符串作为达人种子URL写入
	   * @param text
	   */
	  /**
	  public void insertBrokerSeedByText(String openid,String text) {
		  	getArangoClient(); 
		  //组织默认broker-seed文档
			BaseDocument doc = new BaseDocument();
			Map<String,Object> statusNode = new HashMap<String,Object>();
			statusNode.put("parse", false);
			statusNode.put("collect", false);
			statusNode.put("cps", false);
			statusNode.put("profit", false);
			statusNode.put("notify", false);
			Map<String,Object> timestampNode = new HashMap<String,Object>();
			timestampNode.put("create", new Date());	
			
			//doc.setKey(Util.md5(text));
			doc.getProperties().put("openid", openid);
			doc.getProperties().put("status", statusNode);
			doc.getProperties().put("timestamp", timestampNode);
			doc.getProperties().put("text", text);
			arangoClient.insert("broker_seeds", doc);
//			arangoClient.close();
	  }//**/
	  
	  /**
	   * 将待采集地址写入种子库
	   * @openid 发送信息达人的openId
	   * @type 种子类型，包括url / taobaoToken等
	   * @data 解析后的值，与type相对应
	   * @text 原始消息文本
	   */
	  public void insertBrokerSeed(String openid,String type, String data, String text) {
		  	getArangoClient(); 
		  //组织默认broker-seed文档
			BaseDocument doc = new BaseDocument();
			Map<String,Object> statusNode = new HashMap<String,Object>();
			statusNode.put("parse", false);
			statusNode.put("collect", false);
			statusNode.put("cps", false);
			statusNode.put("profit", false);
			statusNode.put("notify", false);
			Map<String,Object> timestampNode = new HashMap<String,Object>();
			timestampNode.put("create", new Date());	
			
			//doc.setKey(Util.md5(text));
			doc.getProperties().put("openid", openid);
			doc.getProperties().put("type", type);
			doc.getProperties().put("data", data);
			doc.getProperties().put("status", statusNode);
			doc.getProperties().put("timestamp", timestampNode);
			doc.getProperties().put("text", text);
			arangoClient.insert("broker_seeds", doc);
//			arangoClient.close();
	  }
	  
	  private Map<String,Object> createNode(String key, Object value){
			Map<String,Object> node = new HashMap<String,Object>();
			node.put(key,value);
			return node;
	  }
	  
	  /**
	   * 组装一个单条目XML消息
	$itemTpl = " <item>
			 <Title><![CDATA[%s]]></Title> 
			 <Description><![CDATA[%s]]></Description>
			 <PicUrl><![CDATA[%s]]></PicUrl>
			 <Url><![CDATA[%s]]></Url>
			 </item>";
	   */
	  private String item(String title,String description,String picUrl,String url) {
		  if(description == null || description.trim().length() == 0)
			  description = "Life is all about having a good time.";
		  StringBuffer sb = new StringBuffer();
		  sb.append("<item>");
		  sb.append("<title>"+title+"</title>");
		  sb.append("<description>"+description+"</description>");
		  sb.append("<picUrl>"+picUrl+"</picUrl>");
		  sb.append("<url>"+url+"</url>");
		  sb.append("</item>");
		  return sb.toString();
	  }
	  
	  /**
	   * 根据关键词发起搜索，并返回单个条目
	   * @param keyword
	   * @return
	   * @throws UnknownHostException
	   */
	  public String searchMatchedItem(String keyword) throws UnknownHostException {
		  JSONObject json = searchByKeyword(keyword);
		  //获取返回结果
		  String result = null;
		  JSONArray hits = json.getJSONObject("hits").getJSONArray("hits");
			if(hits.size()>0) {
				JSONObject hit = hits.getJSONObject(0).getJSONObject("_source");
				result = item(hit.getString("title"),
						hit.getString("summary"),
						hit.getJSONArray("images").getString(0),
						itemUrlPrefix+hit.getString("_key"));
			}
			logger.debug("Hit matched item and try to send msg."+result);
		  return result;
	  }
	  
	  //根据地理位置发起搜索
	  public String searchByLocation(double lat,double lon) throws UnknownHostException {
		  JSONObject json = searchByLocation(""+lat,""+lon);
		  //获取返回结果
		  String result = null;
		  JSONArray hits = json.getJSONObject("hits").getJSONArray("hits");
			if(hits.size()>0) {
				JSONObject hit = hits.getJSONObject(0).getJSONObject("_source");
				result = item(hit.getString("title"),
						hit.getString("summary"),
						hit.getJSONArray("images").getString(0),
						itemUrlPrefix+hit.getString("_key"));
			}
			logger.debug("Hit locate matched item and try to send msg."+result);
		  return result;
	  }
	  
	  public String queryDocByUrl(String url) {
		  String result = null;
		  getArangoClient();
		  String docKey = Util.md5(url);
		  BaseDocument doc = arangoClient.find("my_stuff", docKey);//等同于通过link.web地址查询
		  if(doc==null) {//如果查不到则尝试通过link.wap地址查询
			  Map<String,Object>  q = Maps.newHashMap();
			  Map<String,Object> wap = Maps.newHashMap();
			  wap.put("wap", url);
			  q.put("link", wap);
			  List<BaseDocument> docs = arangoClient.query(docKey, q, BaseDocument.class);
			  if(docs!=null && docs.size()>0) {
				  doc = docs.get(0);
			  }
		  }
		  
		  //组装返回消息
		  if(doc != null) {
			  Map<String,Object>  props = doc.getProperties();
			  String title = ""+props.get("title");
			  String summary = ""+ props.get("summary");
			  String logo = props.get("logo")==null?""+props.get("logo"):null;
			  if(logo==null) {//如果没有logo，则从image列表内选一张
				  List<String> images = (List<String>)props.get("images");
				  if(images!=null && images.size()>0)
					  logo = images.get(0);
				  else//如果还没有则随机给一个吧
					  logo = getDefaultImage();
			  }
			  String _key = doc.getKey();
			  result = item(title,summary,logo,_key);
		  }
		  return result;
	  }
	  
	  /**
	   * 装载默认返回条目
	   * @return
	   */
	  public String loadDefaultItem(String keyword) {
		if(keyword == null)keyword = "";
		String title = "小确幸，大生活";
		String description = "Life is all about having a good time.";
		String picUrl = getDefaultImage();
		String url = "http://www.biglistoflittlethings.com/ilife-web-wx/index.html?keyword="+keyword;
		return item(title,description,picUrl,url);
	  }
	  public String loadDefaultItem() {
		  return loadDefaultItem(null);
	  }
	  
	  /**
	   * 随机返回一张LOGO图片
	   * @return
	   */
	  private String getDefaultImage() {
		  final int i = (int)( System.currentTimeMillis() % 24 );//取一个随机数用于随机显示LOGO图片
		  String img = "http://www.biglistoflittlethings.com/list/images/logo"+i+".jpeg";
		  return img;
	  }
	  
}
