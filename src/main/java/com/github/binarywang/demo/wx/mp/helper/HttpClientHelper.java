package com.github.binarywang.demo.wx.mp.helper;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class HttpClientHelper {
	Logger logger = LoggerFactory.getLogger(getClass());
	
	public static void main(String[] args) {
		String url = "http://localhost:8080/iLife/a/mod/broker/rest/1001";
		JSONObject data = new JSONObject();
		data.put("hierarchy", "333");
		data.put("level", "万人斩");
		data.put("upgrade", "无");
		data.put("status", "pending");
		data.put("openid", "这是假的，哪来的openid");
		data.put("name", "测试账户");
		data.put("phone", "12345678");
		HttpClientHelper client = new HttpClientHelper();
		client.post(url, data,null);
	}
	
	public JSONObject post(String url, JSONObject data) {
		return post(url,data,null);
	}
	
	public JSONObject post(String url, JSONObject data,Map<String,String> header) {
		JSONObject result = new JSONObject();
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);
		
		post.setHeader("Content-type", "application/json; charset=utf-8");
		if(header!=null && header.size()>0) {
			for(Map.Entry<String, String> entry: header.entrySet()) {
				post.setHeader(entry.getKey(),entry.getValue());
			}
		}
		
		// 构建消息实体
		StringEntity entity = new StringEntity(data.toJSONString(), Charset.forName("UTF-8"));
		entity.setContentEncoding("UTF-8");
		
		// 发送json格式的数据请求
		entity.setContentType("application/json");
		post.setEntity(entity);
		
		try {
			HttpResponse response = httpClient.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			String content = EntityUtils.toString(response.getEntity(), "UTF-8");
			httpClient.close();
			logger.debug("got status code.",statusCode);
			logger.debug("got response content.",content);
			return JSONObject.parseObject(content);
		} catch (Exception e) {
			logger.error("Error occured whild post request to server.[url]"+url,data,e);
			result.put("error", e);
		}
		result.put("status", false);
		return result;
	}
}
