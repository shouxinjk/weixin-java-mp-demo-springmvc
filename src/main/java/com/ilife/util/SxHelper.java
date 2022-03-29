package com.ilife.util;

import java.net.UnknownHostException;
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
		@Value("#{extProps['mp.msg.url.prefix']}") String ilifeUrlPrefix;
		
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

		
		  //发布微信文章
		  public String publishArticle(String openid,String nickname, String url) {
			  logger.debug("try to public new article.[openid]"+openid+"[url]"+url);
			  String remote = ilifeConfig.getSxApi()+"/wx/wxArticle/rest/article";
			  JSONObject broker = new JSONObject();
			  broker.put("openid", openid);//仅设置openid，在后端根据openid查询
			  broker.put("nickname", nickname);//设置nickname，在新建时使用
			  JSONObject article = new JSONObject();
			  String articleId = Util.md5(url);
			  article.put("broker", broker);
			  article.put("id", articleId);//指定ID，同一个URL仅发布一次
			  article.put("isNewRecord", true);//新建而不是更新
			  article.put("url", url);
			  article.put("title", nickname + " 发布");//固定的标题
			  article.put("status", "active");
			  article.put("channel", "auto");
			  JSONObject result = HttpClientHelper.getInstance().post(remote, article,null);
			  logger.debug("article created.[status]"+result.getBoolean("status"));
			  String msg = "";
			  if(result.getBooleanValue("status")) {//发布成功，返回成功卡片
				  msg = item("文章发布成功","点击进入查看",
							ilifeConfig.getFrontendPrefix()+"/list/images/logo"+(System.currentTimeMillis()%25)+".jpeg",
							url);//TODO:需要调整为文章列表页面地址
			  }else {//否则返回失败卡片
				  msg = item("阅豆不够，进文章列表获取吧","点击进入文章列表",
							ilifeConfig.getFrontendPrefix()+"/list/images/logo"+(System.currentTimeMillis()%25)+".jpeg",
							url);//TODO:需要调整为文章列表页面地址
			  }
			  return msg;
		  }
		
		  //新建Board
		  public JSONObject createNewBoard(String title, String keywords) {
			  logger.debug("try to create new board.[title]"+title+"[keywords]"+keywords);
			  String remote = ilifeConfig.getSxApi()+"/mod/board/rest/board";
			  JSONObject broker = new JSONObject();
			  broker.put("id", "system");//固定为系统达人
			  JSONObject board = new JSONObject();
			  String boardId = Util.get32UUID();
			  board.put("id", boardId);//指定ID，保存后需要使用该ID查询
			  board.put("isNewRecord", true);//新建而不是更新
			  board.put("title", "确幸君 的推荐清单");//固定的标题
			  board.put("keywords", keywords);//设置关键字
			  board.put("tags", keywords);//设置tags与关键字完全相同
			  board.put("description", "关于 "+keywords+" 的最新清单，我们已经为你准备好了");//设置关键字
			  board.put("broker", broker);
			  JSONObject result = HttpClientHelper.getInstance().post(remote, board,null);
			  logger.debug("board created.[id]"+result.getString("id"));
			  return result;
		  }
		  
		  //新建BoardItem
		  public JSONObject addBoardItem(String boardId,String itemId, String title,String description) {
			  logger.debug("try to add item to board.[boardId]"+boardId+"[itemId]"+itemId);
			  String remote = ilifeConfig.getSxApi()+"/mod/boardItem/rest/board-item";
			  JSONObject board = new JSONObject();
			  board.put("id", boardId);
			  JSONObject boardItem = new JSONObject();
			  boardItem.put("board", board);
			  boardItem.put("item", itemId);
			  boardItem.put("title", title);
			  boardItem.put("description", description);
			  boardItem.put("id", Util.md5(boardId+itemId));//以boardId+itemId为组合生成新的ID
			  boardItem.put("isNewRecord", true);//新建一个记录
			  JSONObject result = HttpClientHelper.getInstance().post(remote, boardItem,null);
			  logger.debug("board item added.",result);
			  return result;
		  }
		  //新建BoardItem
		  public JSONObject addBoardItem(JSONObject board,String itemId, String title,String description) {
			  logger.debug("try to add item to board.[boardId]"+board.getString("id")+"[itemId]"+itemId);
			  String remote = ilifeConfig.getSxApi()+"/mod/boardItem/rest/board-item";
			  JSONObject boardItem = new JSONObject();
			  boardItem.put("board", board);
			  boardItem.put("item", itemId);
			  boardItem.put("title", title);
			  boardItem.put("description", description);
			  boardItem.put("id", Util.md5(board.getString("id")+itemId));//以boardId+itemId为组合生成新的ID
			  boardItem.put("isNewRecord", true);//新建一个记录
			  JSONObject result = HttpClientHelper.getInstance().post(remote, boardItem,null);
			  logger.error("board item added.",result);
			  return result;
		  }
		  
		  //查询得到Board
		  public JSONObject getBoardById(String id) {
			  String remote = ilifeConfig.getSxApi()+"/mod/board/rest/"+id;
			  JSONObject result = HttpClientHelper.getInstance().get(remote, null,null);
			  logger.error("got result.",result);
			  return result;
		  }
		  
		  //查询得到Occasion
		  public JSONObject getOccasionById(String id) {
			  String remote = ilifeConfig.getSxApi()+"/mod/occasion/rest/"+id;
			  JSONObject result = HttpClientHelper.getInstance().get(remote, null,null);
			  logger.error("got result.",result);
			  return result;
		  }
		  
		  //查询得到Occasion关联的所有motivation
		  public JSONArray getOccasionRelatedNeeds(String occasionId) {
			  String remote = ilifeConfig.getSxApi()+"/mod/motivation/rest/byOccasion/"+occasionId;
			  JSONArray result = HttpClientHelper.getInstance().getList(remote, null,null);
			  logger.error("got result.",result);
			  return result;
		  }		
		
	/**
	 * 向ElasticSearch发起搜索。并返回hits
	 * 仅返回1条
	 * @param keyword
	 * @return
	 */
	  public JSONObject searchByKeyword(String keyword) {
		  return searchByKeyword(keyword,1);
	  }
	  
	  public JSONObject searchByKeyword(String keyword, int size) {
		  Map<String,String> header = Maps.newHashMap();
          header.put("Content-Type","application/json");
          header.put("Authorization","Basic ZWxhc3RpYzpjaGFuZ2VtZQ==");

          String query = esQuery.replace("__keyword", keyword);
          logger.debug("try to search by query. " + query);
		  JSONObject data = JSONObject.parseObject(query);
		  data.put("size", size);
		  
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
	  
	  public Map<String,Object> getItemById(String id) {
		  getArangoClient(); 
		  BaseDocument doc = arangoClient.find("my_stuff", id);
//			arangoClient.close();
		  if(doc!=null)
			  return doc.getProperties();
		  return null;
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
						ilifeUrlPrefix+"/info2.html?id="+hit.getString("_key"));
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
						ilifeUrlPrefix+"/info2.html?id="+hit.getString("_key"));
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
		String url = ilifeUrlPrefix+"/index.html?keyword="+keyword;
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
	  
	  //给新加入用户添加关心的人：将 sxType=seed的用户添加到关心的人，如果已经存在则不再重新建立
	  public void createDefaultConnections(String openId) {
		  logger.debug("try to create default connections for user.[openId]"+openId);
		  //查找sxType=seed的用户
		  Map<String,Object> params = Maps.newHashMap();
		  params.put("sxType", "seed");
		  getArangoClient(); 
		  String aql = "for doc in user_users filter doc.sxType==\"seed\" return doc";
		  List<BaseDocument> seeds = arangoClient.query(aql, null, BaseDocument.class);
		  if(seeds.size()==0) {
			  logger.debug("no seed user found.");
		  }
		 for(BaseDocument doc:seeds) {
			 logger.debug("create default user connections for user.[name]"+doc.getProperties().get("name"),doc);
			 //创建一个伪用户
			 String key = Util.md5_short(openId+doc.getKey());//以当前用户openId及种子用户Id计算MD5，取16位
			 doc.setKey(key);
			 doc.getProperties().remove("sxType");//一定要将sxType去掉
			 try {//如果创建失败则不再执行关系建立
				 arangoClient.insert("user_users", doc);
				 //建立和指定用户的关联
				 BaseDocument conn = new BaseDocument();
				 conn.setKey(Util.md5_short(openId+key));//同一个用户对伪用户仅能有一个连接
				 conn.addAttribute("_from", "user_users/"+openId);
				 conn.addAttribute("_to", "user_users/"+key);
				 conn.getProperties().put("name", doc.getProperties().get("relationship")==null?"我关心的TA":""+doc.getProperties().get("relationship"));
				 arangoClient.insert("connections", conn);
			 }catch(Exception ex) {
				 logger.error("error occured while creating default personas.",ex);
			 }
		 }
	  }
	  
	  //给新注册的达人添加默认画像：将 broker=system&sxType=seed的画像添加到达人，如果已经存在则不再重新建立
	  //注意：通过达人的openId建立关系
	  public void createDefaultPersonas(String openId) {
		  logger.debug("try to create default connections for user.[openId]"+openId);
		  //查找sxType=seed的画像
		  Map<String,Object> params = Maps.newHashMap();
		  params.put("sxType", "seed");
		  getArangoClient(); 
		  String aql = "for doc in persona_personas filter doc.sxType==\"seed\" and doc.broker==\"system\" return doc";
		  List<BaseDocument> seeds = arangoClient.query(aql, null, BaseDocument.class);
		  if(seeds.size()==0) {
			  logger.debug("no seed persona found.");
		  }
		 for(BaseDocument doc:seeds) {
			 logger.debug("create default persona connections for broker.[name]"+doc.getProperties().get("name"),doc);
			 //创建一个画像，实际是复制
			 String key = Util.md5_short(openId+doc.getKey());//以当前用户openId及种子用户Id计算MD5，取16位
			 doc.setKey(key);
			 doc.getProperties().put("broker", openId);
			 doc.getProperties().remove("sxType");//一定要将sxType去掉
			 try {//如果创建失败则不再执行关系建立
				 arangoClient.insert("persona_personas", doc);
				 //建立达人和画像的关联
				 BaseDocument conn = new BaseDocument();
				 conn.setKey(Util.md5_short(openId+key));//同一个用户对伪用户仅能有一个连接
				 conn.addAttribute("_from", "user_users/"+openId);
				 conn.addAttribute("_to", "persona_personas/"+key);
				 arangoClient.insert("user_persona", conn);
			 }catch(Exception ex) {
				 logger.error("error occured while creating default personas.",ex);
			 }
		 }		  
	  }
	  
	  
}
