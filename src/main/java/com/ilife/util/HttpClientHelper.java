package com.ilife.util;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class HttpClientHelper {
	Logger logger = LoggerFactory.getLogger(getClass());
	CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public static void main(String[] args) {
		String url = "http://www.shouxinjk.net/ilife/a/mod/broker/rest/1001";
		JSONObject data = new JSONObject();
		data.put("hierarchy", "333");
		data.put("level", "万人斩");
		data.put("upgrade", "无");
		data.put("status", "pending");
		data.put("openid", "这是假的，哪来的openid");
		data.put("name", "测试账户");
		data.put("phone", "12345678");
		HttpClientHelper.getInstance().post(url, data,null);
		
		
		//设置data server Authorization
		String url2 = "https://data.shouxinjk.net/_db/sea/_api/document/connections?returnNew=true";
	    Map<String,String> header = new HashMap<String,String>();
	    header.put("Authorization","Basic aWxpZmU6aWxpZmU=");
	    
		JSONObject conn = new JSONObject();
		conn.put("_from", "user_users/o8HmJ1EdIUR8iZRwaq1T7D_nPIYc");//源是推荐者
		conn.put("_to","user_users/oQhcg5eNMytKMgzQx-xYC2DiUu7E");//端是新加入的用户：从结果中获取
		conn.put("name", "我关心的TA");//关系名称
		HttpClientHelper.getInstance().post(url2, conn,header);
	}
	
	private static HttpClientHelper helper = null;
	
	public static HttpClientHelper getInstance() {
		if(helper == null) {
			helper = new HttpClientHelper();
		}
		return helper;
	}
	
	public String postForChatGPT(String url, JSONObject data,Map<String,String> header) throws Exception {
        String result = "";
        CloseableHttpClient httpClient = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            // 全部信任 不做身份鉴定
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new String[] { "TLSv1", "TLSv1.2" }, null, NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", new PlainConnectionSocketFactory()).register("https", sslsf).build();
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(200);// max connection
            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(cm).setConnectionManagerShared(true).build();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type", "application/json; charset=utf-8");
    		if(header!=null && header.size()>0) {
    			for(Map.Entry<String, String> entry: header.entrySet()) {
    				httpPost.setHeader(entry.getKey(),entry.getValue());
    			}
    		}
    		// 构建消息实体
    		StringEntity entity = new StringEntity(data.toJSONString(), Charset.forName("UTF-8"));
    		entity.setContentEncoding("UTF-8");
    		
    		// 发送json格式的数据请求
    		entity.setContentType("application/json");
    		httpPost.setEntity(entity);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();// 设置请求和传输超时时间
            httpPost.setConfig(requestConfig);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity resEntity = httpResponse.getEntity();
            result = EntityUtils.toString(resEntity);
        } catch (Exception e) {
            throw e;
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return result;
    }
	
	public JSONObject post(String url, JSONObject data) {
		return post(url,data,null);
	}
	
	public JSONObject post(String url, JSONObject data,Map<String,String> header) {
		JSONObject result = new JSONObject();
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
			post.releaseConnection();
			//httpClient.close();
			logger.debug("got status code.",statusCode);
			logger.debug("got response content.",content);
			
			return JSONObject.parseObject(content);
		} catch (Exception e) {
			logger.error("Error occured while do post request to server.[url]"+url,data,e);
			result.put("error", e);
		}
		result.put("status", false);
		return result;
	}
	
	public JSONObject put(String url, JSONObject data,Map<String,String> header) {
		JSONObject result = new JSONObject();
		HttpPut post = new HttpPut(url);
		
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
			post.releaseConnection();
			//httpClient.close();
			logger.debug("got status code.",statusCode);
			logger.debug("got response content.",content);
			
			return JSONObject.parseObject(content);
		} catch (Exception e) {
			logger.error("Error occured while do put request to server.[url]"+url,data,e);
			result.put("error", e);
		}
		result.put("status", false);
		return result;
	}	
	
	public JSONObject get(String url, Map<String,String> params,Map<String,String> header) {
		JSONObject result = new JSONObject();
		
		//组装参数
		int i=0;
		if(params!=null && params.size()>0) {
			for(Map.Entry<String, String> entry: params.entrySet()) {
				if(i==0)
					url += "?"+entry.getKey()+"="+entry.getValue();
				else
					url += "&"+entry.getKey()+"="+entry.getValue();
				i++;
			}
		}		
		
		HttpGet get = new HttpGet(url);
		
		//设置请求头
		get.setHeader("Content-type", "application/json; charset=utf-8");
		if(header!=null && header.size()>0) {
			for(Map.Entry<String, String> entry: header.entrySet()) {
				get.setHeader(entry.getKey(),entry.getValue());
			}
		}
		
		try {
			HttpResponse response = httpClient.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			String content = EntityUtils.toString(response.getEntity(), "UTF-8");
			get.releaseConnection();
			//httpClient.close();
			logger.debug("got status code.",statusCode);
			logger.debug("got response content.",content);
			
			return JSONObject.parseObject(content);
		} catch (Exception e) {
			logger.error("Error occured while do get request to server.[url]"+url,params,e);
			result.put("error", e);
		}
		result.put("status", false);
		return result;
	}
	
	//获取列表对象
	public JSONArray getList(String url, Map<String,String> params,Map<String,String> header) {
		JSONArray result = new JSONArray();
		
		//组装参数
		int i=0;
		if(params!=null && params.size()>0) {
			for(Map.Entry<String, String> entry: params.entrySet()) {
				if(i==0)
					url += "?"+entry.getKey()+"="+entry.getValue();
				else
					url += "&"+entry.getKey()+"="+entry.getValue();
				i++;
			}
		}		
		
		HttpGet get = new HttpGet(url);
		
		//设置请求头
		get.setHeader("Content-type", "application/json; charset=utf-8");
		if(header!=null && header.size()>0) {
			for(Map.Entry<String, String> entry: header.entrySet()) {
				get.setHeader(entry.getKey(),entry.getValue());
			}
		}
		
		try {
			HttpResponse response = httpClient.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			String content = EntityUtils.toString(response.getEntity(), "UTF-8");
			get.releaseConnection();
			//httpClient.close();
			logger.debug("got status code.",statusCode);
			logger.debug("got response content.",content);
			
			return JSONObject.parseArray(content);
		} catch (Exception e) {
			logger.error("Error occured while do get request to server.[url]"+url,params,e);
		}
		return result;
	}
}
