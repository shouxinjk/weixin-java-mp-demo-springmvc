package com.github.binarywang.demo.wx.mp.handler;

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
	public void searchByKeyword() {
		JSONObject result = helper.searchByKeyword("火锅");
		JSONArray hits = result.getJSONObject("hits").getJSONArray("hits");
		assert hits.size()>0;
	}
	
	@Test
	public void searchByLocation() {
		JSONObject result = helper.searchByLocation("27.9881","86.9250");
		JSONArray hits = result.getJSONObject("hits").getJSONArray("hits");
		assert hits.size()>0;
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
}