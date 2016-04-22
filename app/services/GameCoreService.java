package services;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import models.Game;
import models.Game.GameStatus;
import models.Player;
import models.PlayersInGame;
import models.PlayersInGame.Role;
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
	public static final String IDENTITY_STR = "身份";
	public static final String MEMBER_STR = "成员";

	/**
	 * 
	 * @Description:游戏核心逻辑代码
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
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

				textMessage.setContent("正在测试，" + player.name + "，您输入的是" + content);

				// 获取当前游戏房间的房间号和Game数据对象
				String currentGameRoomNO = (String) Cache.get(CACHE_KEY_PLAYER + fromUserName);
				Game currentGame = currentGameRoomNO != null ? (Game) Cache.get(CACHE_KEY_GAME + currentGameRoomNO)
						: null;

				// 退出房间
				if (EXIT_STR.equals(content)) {
					if (currentGameRoomNO == null) {
						textMessage.setContent("你又没在房间，退个奶子啊！");
						return MessageUtil.textMessageToXml(textMessage);
					}

					if (Game.exit(currentGame.id) != null) {
						// db操作成功后 清除cache数据
						Cache.safeDelete(GameCoreService.CACHE_KEY_GAME + currentGameRoomNO);
						PlayersInGame.listByGame(currentGame)
								.forEach(row -> Cache.safeDelete(GameCoreService.CACHE_KEY_PLAYER + row.player.openId));
						textMessage.setContent("已退出房间" + currentGame.roomNO + "，请先通知其他玩家");
						return MessageUtil.textMessageToXml(textMessage);
					} else {
						textMessage.setContent("退出房间失败，请重试！");
						return MessageUtil.textMessageToXml(textMessage);
					}

				}

				// 创建房间
				if (StringUtils.isNumeric(content) && Integer.parseInt(content) >= 5
						&& Integer.parseInt(content) <= 10) {
					if (currentGameRoomNO == null) {
						int playerNum = Integer.parseInt(content);
						String roomNO = getRoomNO();
						while (Cache.get(CACHE_KEY_GAME + roomNO) != null) {
							roomNO = getRoomNO();
						}

						Game game = Game.init(playerNum, roomNO);
						if (game == null) {
							textMessage.setContent("房间创建失败，请重试");
							return MessageUtil.textMessageToXml(textMessage);
						}

						Cache.add(CACHE_KEY_GAME + game.roomNO, game);
						PlayersInGame pig = PlayersInGame.joinInGame(player, game);
						if (pig == null) {
							textMessage.setContent("房间" + roomNO + "创建成功，但是你加入房间失败");
							return MessageUtil.textMessageToXml(textMessage);
						} else {
							textMessage.setContent(
									"游戏房间号：" + roomNO + "\n游戏人数：" + playerNum + "\n你是：" + pig.playerIndex + "号");
							return MessageUtil.textMessageToXml(textMessage);
						}
					} else {
						textMessage.setContent("您已经在房间" + currentGameRoomNO + "内了");
						return MessageUtil.textMessageToXml(textMessage);
					}
				}

				// 加入房间
				if (StringUtils.isNumeric(content) && content.length() == 4) {
					if (StringUtils.isNotEmpty(currentGameRoomNO)) {
						textMessage.setContent("你已经在房间" + currentGameRoomNO + "内了");
						return MessageUtil.textMessageToXml(textMessage);
					}

					Game game = (Game) Cache.get(CACHE_KEY_GAME + content);
					if (game == null) {
						textMessage.setContent("找不到房间" + content);
						return MessageUtil.textMessageToXml(textMessage);
					}

					if (game.status == GameStatus.进行中) {
						textMessage.setContent("房间已满，无法加入");
						return MessageUtil.textMessageToXml(textMessage);
					}

					if (game.status == GameStatus.已终止 || game.status == GameStatus.已结束) {
						textMessage.setContent("游戏已结束，无法加入");
						return MessageUtil.textMessageToXml(textMessage);
					}

					int playersNum = PlayersInGame.countByGame(currentGame);
					if (playersNum < game.playerNum) {
						PlayersInGame pig = PlayersInGame.joinInGame(player, game);
						if (pig == null) {
							textMessage.setContent("加入房间失败，请重试");
							return MessageUtil.textMessageToXml(textMessage);
						} else {
							playersNum = PlayersInGame.countByGame(currentGame);

							// 人满开车！！！！！
							if (playersNum == currentGame.playerNum) {
								// 获取玩家列表
								List<PlayersInGame> pigs = PlayersInGame.listByGame(currentGame);
								// 乱序玩家列表
								Collections.shuffle(pigs);

								// 分配角色
								for (int i = 0; i < pigs.size(); i++) {
									PlayersInGame item = pigs.get(i);

									if (i == 0) {
										item.updateRole(Role.梅林);
									} else if (i == 1) {
										item.updateRole(Role.派西维尔);
									} else if (i == 2) {
										item.updateRole(Role.莫甘娜);
									} else if (i == 3) {
										item.updateRole(Role.刺客);
									} else if (i == 4 || i == 5 || i == 7 || i == 8) {
										item.updateRole(Role.亚瑟的忠臣);
									} else if ((i == 6 && (pigs.size() == 7 || pigs.size() == 10)) || i == 10) {
										item.updateRole(Role.奥伯伦);
									} else if (i == 6 && pigs.size() == 8) {
										item.updateRole(Role.莫德雷德的爪牙);
									} else if (i == 6 && (pigs.size() == 9 || pigs.size() == 10)) {
										item.updateRole(Role.莫德雷德);
									}

									if (item.id == pig.id)
										pig = item;
								}

								textMessage.setContent("游戏房间号：" + content + "你是最后一个加入房间的人，游戏开始啦！！！通知其他成员输入'身份'获取身份信息\n"
										+ identityInfo(pigs, pig));
								return MessageUtil.textMessageToXml(textMessage);

							}

							textMessage.setContent(
									"游戏房间号：" + content + "\n游戏人数：" + game.playerNum + "\n你是：" + pig.playerIndex + "号");
							return MessageUtil.textMessageToXml(textMessage);
						}
					}

				}

				// 查看房间成员
				if (MEMBER_STR.equals(content)) {
					// 未加入房间
					if (StringUtils.isEmpty(currentGameRoomNO)) {
						textMessage.setContent("你都没加入游戏，看个奶子成员！");
						return MessageUtil.textMessageToXml(textMessage);
					}

					// 获取玩家列表
					List<PlayersInGame> pigs = PlayersInGame.listByGame(currentGame);
					textMessage.setContent(memberInfo(pigs));
					return MessageUtil.textMessageToXml(textMessage);
				}

				// 查看个人身份
				if (IDENTITY_STR.equals(content)) {
					// 未加入房间
					if (StringUtils.isEmpty(currentGameRoomNO)) {
						textMessage.setContent("你都没加入游戏，看个奶子身份！");
						return MessageUtil.textMessageToXml(textMessage);
					}

					GameStatus status = currentGame.status;
					// 已加入房间,但是人还没满
					if (status == GameStatus.未开始) {
						int playersNum = PlayersInGame.countByGame(currentGame);
						textMessage.setContent(
								"游戏还没开始，" + currentGame.playerNum + "缺" + (currentGame.playerNum - playersNum));
						return MessageUtil.textMessageToXml(textMessage);
					}

					// 获取玩家列表
					List<PlayersInGame> pigs = PlayersInGame.listByGame(currentGame);
					// 获取个人身份
					PlayersInGame pig = pigs.stream().filter(row -> row.player.openId.equals(fromUserName)).findFirst()
							.get();
					textMessage.setContent(identityInfo(pigs, pig));
					return MessageUtil.textMessageToXml(textMessage);
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

	/**
	 * 
	 * @Description:随机生成四位房间房间号
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
	 */
	private static String getRoomNO() {
		String code = "";
		Random random = new Random();
		for (int i = 0; i < 4; i++) {
			code += random.nextInt(10);
		}
		return code;
	}

	/**
	 * 
	 * @Description: 房间成员信息
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
	 */
	private static String memberInfo(List<PlayersInGame> pigs) {
		StringBuffer memberInfo = new StringBuffer("本房间内成员名单如下：\n");

		pigs.forEach(row -> memberInfo.append(row.playerIndex + "号是" + row.player.name + "\n"));
		memberInfo.append("\n" + configureInfo(pigs));
		return memberInfo.toString();
	}

	/**
	 * 
	 * @Description:配置信息
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
	 */
	private static String configureInfo(List<PlayersInGame> pigs) {
		String configure = "\n房间的配置   ";
		if (pigs.size() == 5)
			configure = "5人：梅林、派西维尔、忠臣*1  vs 莫甘娜、刺客";
		else if (pigs.size() == 6)
			configure = "6人：梅林、派西维尔、忠臣*2  vs 莫甘娜、刺客";
		else if (pigs.size() == 7)
			configure = "7人：梅林、派西维尔、忠臣*2  vs 莫甘娜、奥伯伦、刺客";
		else if (pigs.size() == 8)
			configure = "8人：梅林、派西维尔、忠臣*3  vs 莫甘娜、刺客、爪牙";
		else if (pigs.size() == 9)
			configure = "9人：梅林、派西维尔、忠臣*4  vs 莫德雷德、莫甘娜、刺客";
		else if (pigs.size() == 10)
			configure = "10人：梅林、派西维尔、忠臣*4  vs 莫德雷德、莫甘娜、奥伯伦、刺客";
		return configure;
	}

	/**
	 * 
	 * @Description:身份信息
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
	 */
	private static String identityInfo(List<PlayersInGame> pigs, PlayersInGame pig) {
		StringBuffer identityInfo = new StringBuffer();

		Role role = pig.role;
		String camp = role.isGoodMan() ? "抵抗组织" : "贱碟";
		identityInfo.append("你的身份是" + camp + "的" + role + "\n");

		if (role == Role.梅林) {
			identityInfo.append("你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> !row.role.isGoodMan())
					.filter(row -> row.role != Role.莫德雷德).collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "，"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("，"));
			identityInfo.append("都不是什么好鸟，不过你看不到莫德雷德这贱人");
		} else if (role == Role.亚瑟的忠臣) {
			identityInfo.append("你只是个不明真相的好人");
		} else if (role == Role.派西维尔) {
			identityInfo.append("你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> row.role == Role.梅林 || row.role == Role.莫甘娜)
					.collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "和"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("和"));
			identityInfo.append("其中一人是抵抗组织的梅林，一人是贱碟的莫甘娜");
		} else if (role == Role.莫德雷德) {
			identityInfo.append("梅林看不到你，你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> !row.role.isGoodMan())
					.filter(row -> row.role != Role.莫德雷德).filter(row -> row.role != Role.奥伯伦)
					.collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "，"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("，"));
			identityInfo.append("跟你是一伙的");
			if (pigs.size() == 7 || pigs.size() == 10) {
				identityInfo.append("，奥伯伦也是跟你一伙的，不过你看不到他。");
			}
		} else if (role == Role.莫甘娜) {
			identityInfo.append("你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> !row.role.isGoodMan())
					.filter(row -> row.role != Role.奥伯伦).collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "，"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("，"));
			identityInfo.append("跟你是一伙的");
			if (pigs.size() == 7 || pigs.size() == 10) {
				identityInfo.append("，奥伯伦也是跟你一伙的，不过你看不到他");
			}
			identityInfo.append("，派西维尔这傻叉分不清你跟梅林。");
		} else if (role == Role.奥伯伦) {
			identityInfo.append("傻叉，你的同伙看不到你，你也看不到他们。");
		} else if (role == Role.莫德雷德的爪牙) {
			identityInfo.append("你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> !row.role.isGoodMan())
					.filter(row -> row.role != Role.奥伯伦).collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "，"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("，"));
			identityInfo.append("跟你是一伙的");
			if (pigs.size() == 7 || pigs.size() == 10) {
				identityInfo.append("，奥伯伦也是跟你一伙的，不过你看不到他。");
			}
		} else if (role == Role.刺客) {
			identityInfo.append("你知道");
			List<PlayersInGame> showPigs = pigs.stream().filter(row -> !row.role.isGoodMan())
					.filter(row -> row.role != Role.奥伯伦).collect(Collectors.toList());
			showPigs.forEach(row -> identityInfo.append(row.player.name + "，"));
			identityInfo.deleteCharAt(identityInfo.lastIndexOf("，"));
			identityInfo.append("跟你是一伙的");
			if (pigs.size() == 7 || pigs.size() == 10) {
				identityInfo.append("，奥伯伦也是跟你一伙的，不过你看不到他");
			}
			identityInfo.append("，好人阵型3次任务成功后，你可以挑选一名可能是梅林的玩家刺杀，如果选中，你们也算赢。");
		}

		identityInfo.append("\n" + configureInfo(pigs));
		return identityInfo.toString();
	}

}
