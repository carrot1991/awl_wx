package services;

import java.util.Date;
import java.util.Map;

import models.Player;
import play.Logger;
import play.mvc.Http.Request;
import utils.wx.MessageUtil;
import utils.wx.TextMessage;

/**
 * 核心服务类
 * 
 */
public class GameCoreService {

	private static final String CACHE_KEY_PLAYER = "/player/";

	/**
	 * 处理微信发来的请求
	 * 
	 * @param request
	 * @return
	 */
	public static String process(Request request) {
		String respMessage = null;
		// 回复文本消息
		TextMessage textMessage = new TextMessage();
		try {
			// xml请求解析
			Map<String, String> requestMap = MessageUtil.parseXml(request);

			// 发送方帐号（open_id）
			String fromUserName = requestMap.get("FromUserName");
			// 公众帐号
			String toUserName = requestMap.get("ToUserName");
			// 消息类型
			String msgType = requestMap.get("MsgType");

			Logger.info("wx process fromUserName:%s,toUserName:%s,msgType:%s", fromUserName, toUserName, msgType);

			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(new Date().getTime());
			textMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_TEXT);
			textMessage.setFuncFlag(0);

			// 文本消息
			if (msgType.equals(MessageUtil.REQ_MESSAGE_TYPE_TEXT)) {
				// 获取发来的消息
				String content = requestMap.get("Content");
				Player player = Player.getByOpenId(fromUserName);

				// 设置游戏昵称
				if (content.startsWith("name")) {
					String name = content.substring(4);
					if (player == null)
						player = new Player();
					player.name = name;
					player.openId = fromUserName;
					player.save();
					textMessage.setContent(player.name + "，昵称设置成功!");
					return MessageUtil.textMessageToXml(textMessage);
				}

				if (player == null) {
					textMessage.setContent("请先设置游戏昵称，请回复[name您的名字]取名（所有指令均无中括号） ");
				} else {
					textMessage.setContent("您是" + player.name + ",您输入了文本消息：" + content);
				}

			}
		} catch (Exception e) {
			Logger.error("processRequest error:%s", e);
			textMessage.setContent("服务器异常:" + e.getMessage());
		}

		respMessage = MessageUtil.textMessageToXml(textMessage);
		Logger.info("respMessage: %s", respMessage);
		return respMessage;
	}
}
