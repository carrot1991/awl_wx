package services;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;

import models.Game;
import models.Player;
import models.PlayersInGame;
import play.Logger;
import play.cache.Cache;
import play.mvc.Http.Request;
import utils.wx.MessageUtil;
import utils.wx.TextMessage;

/**
 * 核心服务类
 * 
 */
public class GameCoreService {

	public static final String CACHE_KEY_PLAYER = "/player/";
	public static final String CACHE_KEY_GAME = "/game/";
	public static final String EXIT_STR = "退出";
	public static final String MEMBER_STR = "成员";

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
					return MessageUtil.textMessageToXml(textMessage);
				}

				textMessage.setContent("正在测试；" + player.name + ",您输入的是" + content);

				String currentGameRoomNo = (String) Cache.get(CACHE_KEY_PLAYER + fromUserName);
				Game currentGame = currentGameRoomNo != null ? (Game) Cache.get(CACHE_KEY_GAME + currentGameRoomNo)
						: null;

				if (EXIT_STR.equals(content)) {
					if (currentGameRoomNo == null) {
						textMessage.setContent("你又没在房间，退个奶子啊！");
						return MessageUtil.textMessageToXml(textMessage);
					} else {
						Cache.safeDelete(CACHE_KEY_GAME + currentGameRoomNo);
						PlayersInGame.listByGame(currentGame)
								.forEach(row -> Cache.safeDelete(CACHE_KEY_PLAYER + row.player.openId));
						currentGame.exit();
						textMessage.setContent("已退出房间" + currentGame.roomNO + ",请先通知其他玩家");
						return MessageUtil.textMessageToXml(textMessage);
					}
				}

				if (StringUtils.isNumeric(content) && Integer.parseInt(content) >= 5
						&& Integer.parseInt(content) <= 10) {
					if (currentGameRoomNo == null) {
						int playerNum = Integer.parseInt(content);
						String roomNO = getCode();
						while (Cache.get(CACHE_KEY_GAME + roomNO) != null) {
							roomNO = getCode();
						}

						Game game = Game.init(playerNum, roomNO);
						if (game == null) {
							textMessage.setContent("房间创建失败，请重试");
							return MessageUtil.textMessageToXml(textMessage);
						}

						Cache.add(CACHE_KEY_GAME + game.roomNO, game);
						PlayersInGame pig = PlayersInGame.joinInGame(player, game);
						if (pig == null) {
							textMessage.setContent("房间" + roomNO + "创建成功,但是你加入房间失败");
							return MessageUtil.textMessageToXml(textMessage);
						} else {
							textMessage.setContent(
									"游戏房间号：" + roomNO + "\n游戏人数：" + playerNum + "\n你是：" + pig.playerIndex + "号");
							return MessageUtil.textMessageToXml(textMessage);
						}

					} else {
						textMessage.setContent("您已经在房间 " + currentGameRoomNo + "内了");
						return MessageUtil.textMessageToXml(textMessage);
					}
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

	private static String getCode() {
		String code = "";
		Random random = new Random();
		for (int i = 0; i < 4; i++) {
			code += random.nextInt(10);
		}
		return code;
	}

}
