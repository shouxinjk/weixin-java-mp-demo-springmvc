package com.ilife.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
			  //点击后跳转到文章列表，能够同时获取 用户信息
			  String wxUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxe12f24bb8146b774&"
						+ "redirect_uri=https://www.biglistoflittlethings.com/ilife-web-wx/dispatch.html&response_type=code&scope=snsapi_userinfo&"
						+ "state=____STATE____#wechat_redirect";
			  
			  wxUrl = StringEscapeUtils.escapeHtml4(wxUrl);

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
			  article.put("title", "新文章 "+nickname);//固定的标题
			  String img = ilifeConfig.getFrontendPrefix()+"/list/images/logo"+(System.currentTimeMillis()%25)+".jpeg";
			  JSONObject wechatArticle = null;
			  try {
				  wechatArticle = getWxArticleInfo(url);
				  if(wechatArticle.getString("title")!=null && wechatArticle.getString("title").trim().length()>0)
					  article.put("title", wechatArticle.getString("title"));
				  if(wechatArticle.getString("coverImg")!=null) {
					  article.put("coverImg", wechatArticle.getString("coverImg"));
					  img = wechatArticle.getString("coverImg");
				  }
			  }catch(Exception ex) {
				  //do nothing
				  article.put("title", "新发布文章");
				  article.put("coverImg", "");
			  }
			  article.put("status", "active");
			  article.put("channel", "auto");
			  JSONObject result = HttpClientHelper.getInstance().post(remote, article,null);
			  logger.debug("article created.[status]"+result.getBoolean("status"));
			  String msg = "";
			  
			  if(result.getBooleanValue("status")) {//发布成功，提交到互阅列表后返回成功卡片
				  /**
				  //提交到grouping加入互阅
		    		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");//("yyyy-MM-dd HH:mm");//默认每天发一班
		    		
		    		//当前固定为整点发车，发报告时间为15分，截止时间为1小时
		    		Calendar cal = Calendar.getInstance();
		    		int currentHour = cal.get(Calendar.HOUR_OF_DAY);
		    		cal.set(Calendar.MINUTE, 0);//整点开始
		    		cal.set(Calendar.SECOND, 0);
		    		Date timeFrom = cal.getTime();
		    		cal.add(Calendar.HOUR, 24);//截止时间为一小时后；每天一班为24小时
		    		Date timeTo = cal.getTime();
		    		
		    		String seed = fmt.format(timeFrom);
		    		String code = Util.get6bitCodeRandom(seed);//需要固定seed，在生成报告时能够获取
		    		
				  JSONObject grouping = new JSONObject();
				  grouping.put("code", code);
				  grouping.put("subjectType", "article");//固定为文章类型
				  grouping.put("subjectId", result.getJSONObject("data").getString("id"));//文章ID
				  grouping.put("timeFrom", timeFrom.getTime());
				  grouping.put("timeTo", timeTo.getTime());
				  remote = ilifeConfig.getSxApi()+"/wx/wxGrouping/rest/grouping";
				  result = HttpClientHelper.getInstance().post(remote, grouping,null);
				  logger.debug("article groupping-ed.[status]"+result.getBoolean("success"));
				  if(result.getBoolean("success")) {
//				   msg = item(article.getString("title"),"文章发布成功，已加入今天的互阅列表，分享邀请更多人来阅读吧~~",
//								"https://www.biglistoflittlethings.com/static/logo/grouping/default.png",
//								wxUrl.replace("____STATE____", "publisher__articles-grouping___code="+code+"__timeFrom="+timeFrom.getTime()+"__timeTo="+timeTo.getTime()));//跳转到互阅文章列表页面地址
				  }else {
				   msg = item(article.getString("title"),"文章发布成功，点击进入查看",
							img,
							wxUrl.replace("____STATE____", "publisher__articles"));//跳转到文章列表页面地址
					  
				  }
				  //**/
				  //直接返回文章 列表
				  msg = item(article.getString("title"),"文章发布成功，点击进入查看",
						  "https://www.biglistoflittlethings.com/static/logo/grouping/default.png",
							wxUrl.replace("____STATE____", "publisher__articles"));//跳转到文章列表页面地址
			  }else {//否则返回失败卡片
				   msg = item(article.getString("title"),"文章发布失败，阅豆不够，请先阅读或关注获取吧~~",
							img,
							wxUrl.replace("____STATE____", "publisher__articles"));//跳转到文章列表页面地址
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
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  JSONObject result = HttpClientHelper.getInstance().post(remote, params);
		  logger.error("got result.",result);
		  if(result.getBooleanValue("success")) {
			  return result.getString("url");
		  }
		  return "";
	  }
	  
	  //判断是否支持自动入库
	  public JSONObject checkAutoEnhouseSupport(String url) {
		  String remote = ilifeConfig.getSxApi()+"/rest/cps/support";
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  return HttpClientHelper.getInstance().post(remote, params);
	  }
	  
	  //根据URL自动采集商品
	  public JSONObject autoEnhouse(String url,String text,String openid) {
		  //调用远端服务完成自动上架
		  String remote = ilifeConfig.getSxApi()+"/rest/cps/enhouse";
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  params.put("text", text);
		  params.put("openid", openid);
		  return HttpClientHelper.getInstance().post(remote, params);
	  }
	  
	  @Deprecated
	  //判断是否是拼多多链接，如果是则自动上架。
	  //如果不是CPS商品则直接返回错误
	  public JSONObject checkPddUrl(String url,String openid) {
		  //调用远端服务完成自动上架
		  String remote = ilifeConfig.getSxApi()+"/rest/cps/pdd";
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  params.put("openid", openid);
		  return HttpClientHelper.getInstance().post(remote, params);
	  }
	  
	  //判断是否是淘宝链接，如果是则自动上架。
	  //如果不是CPS商品则直接返回错误
	  @Deprecated
	  public JSONObject checkTaobaoUrl(String url,String openid) {
		  //调用远端服务完成自动上架
		  String remote = ilifeConfig.getSxApi()+"/rest/cps/taobao";
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  params.put("openid", openid);
		  return HttpClientHelper.getInstance().post(remote, params);
	  }
	  
	  //通知手动上架。
	  //如果不是CPS商品则直接返回错误
	  public JSONObject checkManualEnhouseUrl(String url,String openid) {
		  //调用远端服务完成自动上架
		  String remote = ilifeConfig.getSxApi()+"/rest/cps/manual";
		  JSONObject params = new JSONObject();
		  params.put("url", url);
		  params.put("openid", openid);
		  return HttpClientHelper.getInstance().post(remote, params);
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
		  insertBrokerSeed( openid, type,  data,  text, false);
	  }
	  //手动指定是否通知状态
	  public void insertBrokerSeed(String openid,String type, String data, String text, boolean notifyStatus) {
		  	getArangoClient(); 
		  //组织默认broker-seed文档
			BaseDocument doc = new BaseDocument();
			Map<String,Object> statusNode = new HashMap<String,Object>();
			statusNode.put("parse", false);
			statusNode.put("collect", false);
			statusNode.put("cps", false);
			statusNode.put("profit", false);
			statusNode.put("notify", notifyStatus);
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
	  public String item(String title,String description,String picUrl,String url) {
		  if(description == null || description.trim().length() == 0)
			  description = "选出好的，分享对的，让生活充满小确幸。";
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
			if(hits.size()>0) { //将从返回的结果内随机取值
				int idx = (int)Math.floor(Math.random()*100)%hits.size();
				JSONObject hit = hits.getJSONObject(idx).getJSONObject("_source");
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
				long idx = System.currentTimeMillis()%hits.size();
				JSONObject hit = hits.getJSONObject((int)idx).getJSONObject("_source");
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
			  String summary = "";
			  if(props.get("summary")!=null) {
				  summary = ""+props.get("summary");
			  }else {
				  Map<String,String> advices = (Map<String,String>)props.get("advices");
				  if(advices != null && advices.keySet().size()>=1) {
					  long index = System.currentTimeMillis()%advices.keySet().size();
					  long i=0;
					  for(String key: advices.keySet()) {
						  if(index == i) {
							  summary = advices.get(key);
							  break;
						  }
						  i++;
					  }
				  }else if(props.get("tagging")!=null && props.get("tagging").toString().trim().length()>0) {
					  summary = props.get("tagging").toString();
				  }else {
					  Map<String,String> distributor = (Map<String,String>)props.get("distributor");
					  summary = distributor.get("name");
				  }
			  }
			  String logo = props.get("logo")==null?""+props.get("logo"):null;
			  if(logo==null) {//如果没有logo，则从image列表内选一张
				  List<String> images = (List<String>)props.get("images");
				  if(images!=null && images.size()>0)
					  logo = images.get(0);
				  else//如果还没有则随机给一个吧
					  logo = getDefaultImage();
			  }
			  String _key = doc.getKey();
			  String targetUrl = "https://www.biglistoflittlethings.com/ilife-web-wx/info2.html?id="+doc.getKey();
			  result = item(title,summary,logo,targetUrl);
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
	  
	  /**
	   * 根据URL地址爬取微信文章 标题、logo、作者、时间、
	   * @param url
	   * @return JSONObject
	   * 	author:作者
	   * 	title：标题
	   * 	coverImg：封面图片
	   * 	publishOn：发布时间
	 * @throws IOException 
	   */
	  public JSONObject getWxArticleInfo(String url) throws IOException {
		  logger.debug("start request article. [url]"+url);
		  JSONObject data = new JSONObject();
		  
		  //请求页面
		  Document doc = Jsoup.connect(url).timeout(3000).get();
		  
		  //获取标题
			Elements titles = doc.getElementsByClass("rich_media_title");
			String title = titles.text();
			if(title != null && title.trim().length() > 0)//标题有时无法获取，前端会采用默认设置
				data.put("title", title);
			logger.debug("got title. [title]"+title);
		  //获取封面图片
			String picUrl = null;
			int flag;
			String htmlString=doc.toString();
			flag=htmlString.indexOf("cdn_url_1_1");//获取1：1图片 //("msg_cdn_url");
			while(htmlString.charAt(flag)!='\"'){
				flag++;
			}
			int beginIndex=++flag;
			while(htmlString.charAt(flag)!='\"')
				flag++;
			int endIndex=--flag;
			picUrl=htmlString.substring(beginIndex,endIndex);
			logger.debug("got coverImg. [coverImg]"+picUrl);
			data.put("coverImg", picUrl);

		  //获取作者
			Element authors = doc.getElementById("js_name");
			String author = authors.text();
			data.put("author", author);
			logger.debug("got author. [author]"+author);
		  //获取发布时间
			/**
			String time=null;
			Elements scripts = doc.select("script");
	        for (Element script : scripts) {
	            String html = script.html();
	            if (html.contains("document.getElementById(\"publish_time\")")) {
	                int fromIndex = html.indexOf("s=\"");
	                time=html.substring(fromIndex+3,fromIndex+13);
	                break;
	            }
	        }
	        data.put("publishTime", time);
	        logger.debug("got publishTime. [publishTime]"+time);
	        //**/
		  return data;
	  }
	  
	  //发送企业微信通知消息
	  //直接用卡片方式组织
	  public void sendWeworkMsg(String title,String description,String picUrl,String url) {
			//组装模板消息
			JSONObject json = new JSONObject();
			json.put("msgtype", "news");
			JSONObject jsonArticle = new JSONObject();
			jsonArticle.put("title" , title);
			jsonArticle.put("description" , description);
			jsonArticle.put("url" , url);
			jsonArticle.put("picurl" , picUrl);

			JSONArray jsonArticles = new JSONArray();
			jsonArticles.add(jsonArticle);
			JSONObject jsonNews = new JSONObject();
			jsonNews.put("articles", jsonArticles);
			json.put("news", jsonNews);
			
			logger.debug("try to send cp msg. ",json);
			
	   	    //准备发起HTTP请求：设置data server Authorization
		    Map<String,String> header = new HashMap<String,String>();
		    header.put("Authorization","Basic aWxpZmU6aWxpZmU=");
		    
			//发送到企业微信
			HttpClientHelper.getInstance().post(
					ilifeConfig.getWeworkApi()+"/notify-cp-company-broker", 
					json,header);
	  }
	  
	  
}
