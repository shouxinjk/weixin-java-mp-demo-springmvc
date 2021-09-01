package com.github.binarywang.demo.wx.mp.handler;

import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.ilife.util.SxHelper;
import com.thoughtworks.xstream.XStream;

import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author Binary Wang
 */
@Component
public class LocationHandler extends AbstractHandler {
  @Autowired
  private SxHelper helper;
  
  @Override
  public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                  Map<String, Object> context, WxMpService wxMpService,
                                  WxSessionManager sessionManager) {
    if (wxMessage.getMsgType().equals(XmlMsgType.LOCATION)) {
    	//TODO 接收处理用户发送的地理位置消息
        //上报地理位置事件
        this.logger.info("\n上报地理位置 。。。 ");
        this.logger.info("\n纬度 : " + wxMessage.getLatitude());
        this.logger.info("\n经度 : " + wxMessage.getLongitude());
        this.logger.info("\n精度 : " + String.valueOf(wxMessage.getPrecision()));
        //根据地理位置搜索附近的商品
        String xml = null;
        try {
        	xml = helper.searchByLocation(wxMessage.getLatitude(),wxMessage.getLongitude());
        }catch(Exception ex) {
        	logger.error("Error occured while search items.[location]lat:"+wxMessage.getLatitude()+" lon:"+wxMessage.getLongitude(),ex);
        }
        if(xml == null || xml.trim().length() == 0) {//如果没找到合适的则提示更换位置发送
        	return new TextBuilder().build("发送的位置5公里内没找到合适的内容，重新试试看呢~", wxMessage, null);
        }
        XStream xstream = new XStream();
        xstream.alias("item", WxMpXmlOutNewsMessage.Item.class);
    	WxMpXmlOutNewsMessage.Item item = (WxMpXmlOutNewsMessage.Item)xstream.fromXML(xml);
    	return WxMpXmlOutMessage.NEWS().addArticle(item).fromUser(wxMessage.getToUser())
    	        .toUser(wxMessage.getFromUser()).build();
    }

    //理论上不会执行到这里，但前面加了个if，这里还是要做一下善后
    try {
        return new TextBuilder().build("处理不了这个信息哦，亲~", wxMessage, null);
      } catch (Exception e) {
        this.logger.error("位置消息接收处理失败", e);
        return null;
      }
  }
  
}
  
  