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
	public void searchByRest() {
		JSONObject result = helper.searchByKeyword("火锅");
		JSONArray hits = result.getJSONObject("hits").getJSONArray("hits");
		assert hits.size()>0;
	}
	
	@Test
	public void matchTaobaoToken() {
		assert helper.isTaobaoToken("付致这行话₳hk5N1SAU2g6₳转移至👉τаo宝аρρ👈，【美国进口Forever New芳新金装 呵护 精致衣物去渍液473ml】");
		assert helper.isTaobaoToken("fu致这行话₤JGvr1SxMkdx₤转移至👉τáǒЬáǒ👈，【魏你好 法国娇兰帝皇修护复原蜜50ml面部精华液精华油 紧致保湿】");
		assert helper.isTaobaoToken("娇兰黄金复原蜜50ml 预售20天发货\n" + 
				"黄金复原蜜 滴滴弹润亮\n" + 
				"\n" + 
				"价格：💰719\n" + 
				"\n" + 
				"$ZhaJ1SxZgei$长按拷贝");
	}
	
	@Test
	public void insertBrokerSeedByText() {
		helper.insertBrokerSeedByText("openid","付致这行话₳hk5N1SAU2g6₳转移至👉τаo宝аρρ👈，【美国进口Forever New芳新金装 呵护 精致衣物去渍液473ml】");
		helper.insertBrokerSeedByText("openid","fu致这行话₤JGvr1SxMkdx₤转移至👉τáǒЬáǒ👈，【魏你好 法国娇兰帝皇修护复原蜜50ml面部精华液精华油 紧致保湿】");
		helper.insertBrokerSeedByText("openid","娇兰黄金复原蜜50ml 预售20天发货\n" + 
				"黄金复原蜜 滴滴弹润亮\n" + 
				"\n" + 
				"价格：💰719\n" + 
				"\n" + 
				"$ZhaJ1SxZgei$长按拷贝");
	}
}