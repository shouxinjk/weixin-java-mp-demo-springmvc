package com.github.binarywang.demo.wx.mp.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.ilife.util.HttpClientHelper;
import com.ilife.util.SxHelper;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;


@RunWith(SpringJUnit4ClassRunner.class) 
@WebAppConfiguration
@ContextConfiguration(locations={"classpath:spring-service-bean.xml","classpath:applicationContext.xml","classpath:spring-servlet-common.xml"}) 

public class HttpClientHelperTest {

	@Test
	public void post() {
		JSONObject result = HttpClientHelper.getInstance().post("https://weather.com/weather/today/l/22.69,113.91?par=google",null,null);
		System.err.println("got completion. "+result);
	}
	
	@Test
	public void postForChatGPT() throws Exception {
		String keyword = "抽象派艺术特点";
		OpenAiService service = new OpenAiService("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",120);
		CompletionRequest completionRequest = CompletionRequest.builder()
		        .prompt(keyword)
		        .model("text-davinci-003")
		        .maxTokens(keyword.length()*2+1000)
		        .echo(true)
		        .build();
		List<CompletionChoice> choices = service.createCompletion(completionRequest).getChoices();
		assert choices!=null&&choices.size()>0;
		for(CompletionChoice choice:choices) {
			System.err.println(choice.getText());
		}
	}
	
	@Test
	public void postForChatGPT2() throws Exception {
		String url = "https://api.openai.com/v1/completions";
		String keyword="抽象派艺术特点";
		
		  Map<String,String> header = Maps.newHashMap();
          header.put("Content-Type","application/json");
          header.put("Authorization","Bearer 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");
          header.put("Host", "api.openai.com");
//          header.put("Accept", "text/event-stream");
////          header.put("User-Agent", userAgent);
//          header.put("X-Openai-Assistant-App-Id", "");
//          header.put("Connection", "close");
//          header.put("Accept-Language", "en-US,en;q=0.9");
//          header.put("Referer", "https://chat.openai.com/chat");
          
          String msgTpl = "{\"model\":\"text-davinci-003\",\"prompt\":\"__keyword\",\"max_tokens\":__maxtokens,\"temperature\":0}";
          String msg = msgTpl.replace("__keyword", keyword).replace("__maxtokens",""+(keyword.length()*2+1000));
          
          String result = HttpClientHelper.getInstance().postForChatGPT(url, JSONObject.parseObject(msg), header);
          assert result!=null && result.trim().length()>0;
          System.err.println("got prompts.\n"+result);
		
	}
	
}