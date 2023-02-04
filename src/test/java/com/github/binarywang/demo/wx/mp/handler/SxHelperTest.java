package com.github.binarywang.demo.wx.mp.handler;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ilife.util.SxHelper;


@RunWith(SpringJUnit4ClassRunner.class) 
@WebAppConfiguration
@ContextConfiguration(locations={"classpath:spring-service-bean.xml","classpath:applicationContext.xml","classpath:spring-servlet-common.xml"}) 

public class SxHelperTest {
	
	@Autowired
	SxHelper helper;

	@Test
	public void chatGPT() {
		String result = helper.requestChatGPT("抽象派艺术特点");
		System.err.println("got completion. "+result);
		assert result.length()>0;
	}
	
	@Test
	public void searchContent() throws UnknownHostException {
		String result = helper.searchContent("内衣");
		System.err.println("got result. total hits is "+result);
		assert result!=null&&result.trim().length()>0;
	}
	
	@Test
	public void searchItemByKeyword() {
		JSONObject result = helper.searchItemByKeyword("情趣内衣");
		JSONArray hits = result.getJSONObject("hits").getJSONArray("hits");
		System.err.println("got result. total hits is "+hits.size());
		assert hits.size()>0;
	}
	
	@Test
	public void searchByLocation() {
		JSONObject result = helper.searchByLocation("27.9881","86.9250");
		JSONArray hits = result.getJSONObject("hits").getJSONArray("hits");
		assert hits.size()>0;
	}
	
	@Test
	public void convertUrl() {
//		System.err.println( helper.convertUrl("http://item.jd.com/5089253.html?cpslink=0%26test=2"));
//		System.err.println( helper.convertUrl("https://item.jd.com/5089253.html?cpslink=0%26test=2"));
//		System.err.println( helper.convertUrl("http://cps.item.jd.com/5089253.html?cpslink=0%26test=2"));
		System.err.println( helper.convertUrl("https://t.vip.com/vEE6ZGoewRA?aq=1&desturl=https%3A%2F%2Fm.vip.com%2Fproduct-1711197624-6918838978304616600.html%3Fnmsns%3Dshop_iphone-7.82.1-link%26nst%3Dproduct%26nsbc%3D%26nct%3Dlink%26ncid%3Dc7e914df1f3d9df62dd417ec63ad0bc48b117b4a%26nabtid%3D13%26nuid%3D460248657%26nchl_param%3Dshare%3Ac7e914df1f3d9df62dd417ec63ad0bc48b117b4a%3A1668154847342%26mars_cid_a%3Dc7e914df1f3d9df62dd417ec63ad0bc48b117b4a%26chl_type%3Dshare&nmsns=shop_iphone-7.82.1-link&nst=product&nsbc=&nct=link&ncid=c7e914df1f3d9df62dd417ec63ad0bc48b117b4a&nabtid=13&nuid=460248657&nchl_param=share:c7e914df1f3d9df62dd417ec63ad0bc48b117b4a:1668154847342&mars_cid_a=c7e914df1f3d9df62dd417ec63ad0bc48b117b4a&chl_type=share"));
	}
	
	@Test
	public void matchUrl() {
		assert helper.getUrl("https://mobile.yangkeduo.com/comm_comment_report.html?_t_timestamp=internal_goods&_x_no_login_launch=1&r=donot").trim().length()>0;
		assert helper.getUrl("3👈微WsWEXoYLDiZ哈 https://m.tb.cn/h.fcPT0o5?sm=b18b7f  Versace/范思哲刺绣字母logo男士全棉短袖POLO衫21年秋冬新款").trim().length()>0;
		assert helper.getUrl("5👈 xixi:/哈axBdXo1mIOk信  ECCO爱步男士真皮休闲鞋2021年秋季新款耐磨低帮板鞋 柔酷X420734").trim().length()==0;
	}
	
	@Test
	public void matchTaobaoToken() {
		assert helper.parseTaobaoToken("付致这行话₳hk5N1SAU2g6₳转移至👉τаo宝аρρ👈，【美国进口Forever New芳新金装 呵护 精致衣物去渍液473ml】").length()>0;
		assert helper.parseTaobaoToken("fu致这行话₤JGvr1SxMkdx₤转移至👉τáǒЬáǒ👈，【魏你好 法国娇兰帝皇修护复原蜜50ml面部精华液精华油 紧致保湿】").length()>0;
		assert helper.parseTaobaoToken("娇兰黄金复原蜜50ml 预售20天发货\n" + 
				"黄金复原蜜 滴滴弹润亮\n" + 
				"\n" + 
				"价格：💰719\n" + 
				"\n" + 
				"$ZhaJ1SxZgei$长按拷贝").length()>0;
		assert helper.parseTaobaoToken("5👈 xixi:/哈axBdXo1mIOk信  ECCO爱步男士真皮休闲鞋2021年秋季新款耐磨低帮板鞋 柔酷X420734").length()>0;
	}
	
	@Test
	public void getKeywordFromTaobaoToken() {
		helper.getKeywordFromTaobaoToken("付致这行话₳hk5N1SAU2g6₳转移至👉τаo宝аρρ👈，【美国进口Forever New芳新金装 呵护 精致衣物去渍液473ml】");
		helper.getKeywordFromTaobaoToken("fu致这行话₤JGvr1SxMkdx₤转移至👉τáǒЬáǒ👈，【魏你好 法国娇兰帝皇修护复原蜜50ml面部精华液精华油 紧致保湿】");
		helper.getKeywordFromTaobaoToken("娇兰黄金复原蜜50ml 预售20天发货\n" + 
				"黄金复原蜜 滴滴弹润亮\n" + 
				"\n" + 
				"价格：💰719\n" + 
				"\n" + 
				"$ZhaJ1SxZgei$长按拷贝");
		helper.getKeywordFromTaobaoToken("5👈 xixi:/哈axBdXo1mIOk信  ECCO爱步男士真皮休闲鞋2021年秋季新款耐磨低帮板鞋 柔酷X420734");
	}
	
	@Test
	public void insertBrokerSeed() {
		helper.insertBrokerSeed("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc","taobaoToken","axBdXo1mIOk","5👈 xixi:/哈axBdXo1mIOk信  ECCO爱步男士真皮休闲鞋2021年秋季新款耐磨低帮板鞋 柔酷X420734");
		helper.insertBrokerSeed("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc","taobaoToken","wUd9Xo25bEl","0.0 xixi:/嘻wUd9Xo25bEl，  Brooks Brothers/布克兄弟男21夏新罗纹网眼条纹Logo款短袖Polo衫");
	}
	
	@Test
	public void createDefaultConnections() {
		helper.createDefaultConnections("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc");
	}
	
	@Test
	public void createDefaultPersonas() {
		helper.createDefaultPersonas("o8HmJ1EdIUR8iZRwaq1T7D_nPIYc");
	}
	
	@Test
	public void getWxArticleInfo() {
		try {
			JSONObject json = helper.getWxArticleInfo("https://mp.weixin.qq.com/s/LH4eF5au4MKZY5RYKETLGg");
			System.out.println(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}