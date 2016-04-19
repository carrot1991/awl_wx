package services;

import java.util.Date;
import java.util.Map;

import play.Logger;
import play.mvc.Http.Request;
import utils.wx.MessageUtil;
import utils.wx.TextMessage;

/**
 * 核心服务类
 * 
 */
public class GameCoreService {

	/**
	 * 处理微信发来的请求
	 * 
	 * @param request
	 * @return
	 */
	public static String process(Request request) {
		String respMessage = null;
		try {
			// xml请求解析
			Map<String, String> requestMap = MessageUtil.parseXml(request);

			// 发送方帐号（open_id）
			String fromUserName = requestMap.get("FromUserName");
			// 公众帐号
			String toUserName = requestMap.get("ToUserName");
			// 消息类型
			String msgType = requestMap.get("MsgType");

			// 回复文本消息
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(new Date().getTime());
			textMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);
			textMessage.setFuncFlag(0);

			// 文本消息
			if (msgType.equals(MessageUtil.REQ_MESSAGE_TYPE_TEXT)) {

			}

			respMessage = MessageUtil.textMessageToXml(textMessage);

		} catch (Exception e) {
			Logger.error("processRequest error:%s", e);
			e.printStackTrace();
		}

		Logger.warn("respMessage: %s", respMessage);
		return respMessage;
	}
}
