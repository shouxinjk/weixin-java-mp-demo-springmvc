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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arangodb.entity.BaseDocument;
import com.google.common.collect.Maps;
@Service
public class SxHelper {
		private Logger logger = LoggerFactory.getLogger(getClass());
		
		@Value("#{extProps['es.url']}") String esUrl;
		@Value("#{extProps['es.query']}") String esQuery;
		@Value("#{extProps['msg.item.url.prefix']}") String itemUrlPrefix;
		
	    ArangoDbClient arangoClient;
	    
		@Value("#{extProps['arangodb.host']}") String host;
		@Value("#{extProps['arangodb.port']}") String port;
		@Value("#{extProps['arangodb.username']}") String username;
		@Value("#{extProps['arangodb.password']}") String password;
		@Value("#{extProps['arangodb.database']}") String database;
	  
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
	  
	  /**
	   * 判定是否包含有淘口令
	   * @param text
	   * @return
	   */
	  public boolean isTaobaoToken(String text) {
	         String pattern = "([\\p{Sc}])\\w{8,12}([\\p{Sc}])";
	         try {
		         Pattern r = Pattern.compile(pattern);
		         Matcher m = r.matcher(text);
		         if (m.find()) {
		             logger.debug("match: " + m.group());
		             return true;
		         }
	         }catch(Exception ex) {//不知道什么鬼，这里竟然会报NullPointerException
	        	 	logger.error("Failed to match taobao token.",ex);
	         }
	         return false;
	  }
	  
	  /**
	   * 从淘口令字符串解析得到商品详情。
	   * 规则为：如果淘口令内包含有【】则取其中间内容
	   * 否则将淘口令替换掉返回
	   * @param text
	   * @return
	   */
	  public String getKeywordFromTaobaoToken(String text) {
	         String pattern = "([\\p{Sc}])\\w{8,12}([\\p{Sc}])";
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
	  public void insertBrokerSeedByText(String openid,String text) {
		  arangoClient = new ArangoDbClient(host,port,username,password,database);
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
