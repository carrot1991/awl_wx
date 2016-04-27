package services;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import models.Action;
import models.Game;
import models.Game.GameStatus;
import models.Player;
import models.PlayersInGame;
import models.PlayersInGame.Role;
import models.Round;
import models.RoundVote;
import models.Vote;

import org.apache.commons.lang.StringUtils;

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
	public static final String CACHE_KEY_GAMEROUND = "/game/round/";
	public static final String EXIT_STR = "退出";
	public static final String IDENTITY_TEXT = "身份";
	public static final String MEMBER_TEXT = "成员";
	public static final String TASK_TEXT = "任务";
	public static final String VOTE_TEXT = "投票";
	public static final String SUCC_TEXT = "1";
	public static final String FAIL_TEXT = "0";

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
				textMessage.setContent(gameResponse(fromUserName, requestMap.get("Content")));
				return MessageUtil.textMessageToXml(textMessage);
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
	 * @Description:抽离游戏核心逻辑代码，便于测试
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月27日
	 * @return String 返回类型
	 */
	public static String gameResponse(String openid, String postMessage) {
		try {
			// 获取发来的消息
			Player player = Player.getByOpenId(openid);
			String responseMessage = "";

			// 设置游戏昵称
			if (postMessage.startsWith("name")) {
				String name = postMessage.substring(4);
				if (player == null)
					player = new Player();
				player.name = name;
				player.openId = openid;
				player.save();
				return player.name + "，昵称设置成功!";
			}

			if (player == null)
				return "请先设置游戏昵称，请回复[name您的名字]取名（所有指令均无中括号） ";

			responseMessage = "正在测试，" + player.name + "，您输入的是" + postMessage;

			// 获取当前游戏房间的房间号和Game数据对象
			String currentGameRoomNO = (String) Cache.get(CACHE_KEY_PLAYER + openid);
			Game currentGame = currentGameRoomNO != null ? (Game) Cache.get(CACHE_KEY_GAME + currentGameRoomNO) : null;
			Round currentRound = currentGameRoomNO != null ? (Round) Cache.get(CACHE_KEY_GAMEROUND + currentGameRoomNO)
					: null;

			// 退出房间
			if (EXIT_STR.equals(postMessage)) {
				if (currentGameRoomNO == null)
					return "你又没在房间，退个奶子啊！";

				if (Game.exit(currentGame) != null) {
					PlayersInGame.listByGame(currentGame).forEach(
							row -> Cache.safeDelete(GameCoreService.CACHE_KEY_PLAYER + row.player.openId));
					return "已退出房间" + currentGame.roomNO + "，请先通知其他玩家";
				} else
					return "退出房间失败，请重试！";
			}

			// 创建房间
			if (StringUtils.isNumeric(postMessage) && Integer.parseInt(postMessage) >= 5
					&& Integer.parseInt(postMessage) <= 10) {
				if (currentGameRoomNO == null) {
					int playerNum = Integer.parseInt(postMessage);
					String roomNO = getRoomNO();
					while (Cache.get(CACHE_KEY_GAME + roomNO) != null) {
						roomNO = getRoomNO();
					}

					// 初始化Game
					Game game = Game.init(playerNum, roomNO);
					if (game == null)
						return "房间创建失败，请重试";

					// 初始化五个Round
					List<Round> rounds = Round.init(game);
					if (rounds == null || rounds.size() != 5) {
						Game.exit(game);
						return "房间创建失败，请重试";
					}

					// 本人加入游戏
					PlayersInGame pig = PlayersInGame.joinInGame(player, game);
					if (pig == null)
						return "房间" + roomNO + "创建成功，但是你加入房间失败";
					else
						return "游戏房间号：" + roomNO + "\n游戏人数：" + playerNum + "\n你是：" + pig.playerIndex + "号";

				} else
					return "您已经在房间" + currentGameRoomNO + "内了";

			}

			// 加入房间
			if (StringUtils.isNumeric(postMessage) && postMessage.length() == 4) {
				if (StringUtils.isNotEmpty(currentGameRoomNO))
					return "你已经在房间" + currentGameRoomNO + "内了";

				Game game = (Game) Cache.get(CACHE_KEY_GAME + postMessage);
				if (game == null)
					return "找不到房间" + postMessage;

				if (game.status == GameStatus.执行 || game.status == GameStatus.投票)
					return "房间已满，无法加入";

				if (game.status == GameStatus.已终止 || game.status == GameStatus.已结束)
					return "游戏已结束，无法加入";

				int playersNum = PlayersInGame.countByGame(currentGame);
				if (playersNum < game.playerNum) {
					PlayersInGame pig = PlayersInGame.joinInGame(player, game);
					if (pig == null) {
						return "加入房间失败，请重试";
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

							// 开局
							Game.start(currentGame);
							// 初始化第一局投票
							RoundVote.add(currentRound);
							return "游戏房间号：" + postMessage + "你是最后一个加入房间的人，游戏开始啦！！通知其他成员输入'身份'获取身份信息。请1号玩家挑选"
									+ currentRound.actionPlayerNum + "位玩家进行组队，然后全体投票。\n" + identityInfo(pigs, pig);
						} else
							return "游戏房间号：" + postMessage + "\n游戏人数：" + game.playerNum + "\n你是：" + pig.playerIndex
									+ "号";
					}
				}

			}

			// 投票 & 执行
			if (SUCC_TEXT.equals(postMessage) || FAIL_TEXT.equals(postMessage)) {

				// 未加入房间
				if (StringUtils.isEmpty(currentGameRoomNO))
					return "都没加入游戏,玩毛";

				GameStatus status = currentGame.status;

				// 已加入房间,但是人还没满
				if (status == GameStatus.未开始)
					return "游戏还没开始，" + currentGame.playerNum + "缺"
							+ (currentGame.playerNum - PlayersInGame.countByGame(currentGame));

				// 投票环节
				if (status == GameStatus.投票) {
					Vote vote = Vote.get(player, currentRound);
					if (vote != null)
						return "你已经投过票了，请回复[投票]查看结果。";

					vote = Vote.add(player, currentRound, SUCC_TEXT.equals(postMessage) ? true : false);
					if (vote == null)
						return "投票失败，我也不知道为什么，重试下。";

					RoundVote rv = RoundVote.update(currentRound, SUCC_TEXT.equals(postMessage) ? true : false);
					if (rv.isSuccess == null)
						return "投票成功!还有" + (currentGame.playerNum - rv.approveNum - rv.opposeNum) + "人还在墨迹。。。";
					else if (rv.isSuccess == true) {
						Game.startAction(currentGame);
						return "投票成功!本次组队成功，请选举出的人员执行任务！";
					} else {
						Long count = RoundVote.count(currentRound);
						if (count >= 3) {
							currentRound = Round.updateFailed(currentRound);
							currentRound = Round.nextRound(currentRound);
							currentGame = Game.nextRound(currentRound);

							if (currentGame.succNum != 3 && currentGame.failedNum != 3) {
								StringBuffer sb = new StringBuffer();

								sb.append("3次投票都失败，所以本回合失败了。");
								sb.append("第" + currentGame.roundIndex + "回合开始，请下位玩家挑选" + currentRound.actionPlayerNum
										+ "位玩家组队，然后全体投票。");
								return sb.toString();
							}
							// 游戏结束，进入结算环节
							else {
								List<Round> roundList = Round.listByGame(currentGame);
								boolean isSucc = currentGame.succNum == 3 ? true : false;
								StringBuffer sb = new StringBuffer();
								if (isSucc) {
									sb.append("抵抗组织三轮任务成功，但是间谍仍然可由刺客暗杀梅林获取胜利！每回合详情如下\n");
								} else {
									sb.append("本局游戏间谍获胜，愚蠢的抵抗组织。。。。。\n");
								}

								for (Round round : roundList) {
									sb.append("第" + round.roundIndex + "轮任务共" + round.actionPlayerNum + "执行任务\n");
									if (round.isSuccess != null) {
										sb.append("任务" + (round.isSuccess ? "成功" : "失败"));
										if (round.failedNum == 0)
											sb.append(",没有人破坏任务\n");
										else
											sb.append(round.failedNum + "人破坏任务\n");

										List<Action> actions = Action.listByRound(round);
										sb.append("成员为");
										actions.forEach(row -> sb.append(row.player.name + " "));
										sb.append("\n\n");
									} else {
										if (round.roundIndex == currentRound.roundIndex)
											sb.append("任务进行中\n\n");
										else
											sb.append("任务未进行\n\n");
									}
								}

								return sb.toString();
							}

						} else {
							RoundVote.add(currentRound);
							return "投票成功!本次组队失败，" + rv.opposeNum + "人反对本次组队。这是本回合第" + count
									+ "失败，如果3次投票失败，本回合自动失败。请下位玩家挑选" + currentRound.actionPlayerNum + "位玩家组队，然后全体投票。";
						}
					}

				}
				// 执行环节
				else if (status == GameStatus.执行) {
					// 重复执行判断
					Action action = Action.get(player, currentRound);
					if (action != null)
						return "你已经行动过了，请回复[任务]查看结果。";

					// 持久化执行记录
					action = Action.add(player, currentRound, SUCC_TEXT.equals(postMessage) ? true : false);
					if (action == null)
						return "执行失败，我也不知道为什么，重试下。";

					currentRound = Round.update(currentRound, SUCC_TEXT.equals(postMessage) ? true : false);
					int failedNum = currentRound.failedNum;
					Boolean isSuccess = currentRound.isSuccess;

					if (isSuccess == null)
						return "执行成功!还有"
								+ (currentRound.actionPlayerNum - currentRound.failedNum - currentRound.succNum)
								+ "人还在墨迹。。。";
					else {
						currentRound = Round.nextRound(currentRound);
						currentGame = Game.nextRound(currentRound);

						if (currentGame.succNum != 3 && currentGame.failedNum != 3) {
							StringBuffer sb = new StringBuffer();

							if (isSuccess == true) {
								sb.append("本回合成功!");
								if (currentRound.failedNum == 0)
									sb.append("没有人破坏任务。");
								else
									sb.append("不过有" + failedNum + "人破坏任务，可惜人数不够。");
								sb.append("第" + currentGame.roundIndex + "回合开始，请下位玩家挑选" + currentRound.actionPlayerNum
										+ "位玩家组队，然后全体投票。");
								return sb.toString();
							} else {
								sb.append("本回合失败了。" + failedNum + "人破坏任务。");
								sb.append("第" + currentGame.roundIndex + "回合开始，请下位玩家挑选" + currentRound.actionPlayerNum
										+ "位玩家组队，然后全体投票。");
								return sb.toString();
							}
						}
						// 游戏结束，进入结算环节
						else {
							List<Round> roundList = Round.listByGame(currentGame);
							boolean isSucc = currentGame.succNum == 3 ? true : false;
							StringBuffer sb = new StringBuffer();
							if (isSucc) {
								sb.append("抵抗组织三轮任务成功，但是间谍仍然可由刺客暗杀梅林获取胜利！每回合详情如下\n");
							} else {
								sb.append("本局游戏间谍获胜，愚蠢的抵抗组织。。。。。\n");
							}

							for (Round round : roundList) {
								sb.append("第" + round.roundIndex + "轮任务共" + round.actionPlayerNum + "执行任务\n");
								if (round.isSuccess != null) {
									sb.append("任务" + (round.isSuccess ? "成功" : "失败"));
									if (round.failedNum == 0)
										sb.append(",没有人破坏任务\n");
									else
										sb.append(round.failedNum + "人破坏任务\n");

									List<Action> actions = Action.listByRound(round);
									sb.append("成员为");
									actions.forEach(row -> sb.append(row.player.name + " "));
									sb.append("\n\n");
								} else {
									if (round.roundIndex == currentRound.roundIndex)
										sb.append("任务进行中\n\n");
									else
										sb.append("任务未进行\n\n");
								}
							}

							return sb.toString();
						}

					}

				}
			}

			// 查看投票结果
			if (VOTE_TEXT.equals(postMessage)) {
				// 未加入房间
				if (StringUtils.isEmpty(currentGameRoomNO))
					return "你都没加入游戏，看个奶子投票！";

				GameStatus status = currentGame.status;
				// 已加入房间,但是人还没满
				if (status == GameStatus.未开始) {
					int playersNum = PlayersInGame.countByGame(currentGame);
					return "游戏还没开始，" + currentGame.playerNum + "缺" + (currentGame.playerNum - playersNum);
				}

				StringBuffer sb = new StringBuffer();
				List<RoundVote> roundVotes = RoundVote.list(currentRound);

				if (status == GameStatus.投票)
					sb.append("当前是第" + currentGame.roundIndex + "回合的投票。\n");
				else if (status == GameStatus.执行)
					sb.append("第" + currentGame.roundIndex + "回合的投票已结束，正在执行中。\n");

				for (RoundVote rv : roundVotes) {
					if (rv.isSuccess == null) {
						sb.append("第" + rv.voteIndex + "轮投票正在进行中，已有" + rv.approveNum + "赞成，" + rv.opposeNum + "反对。\n");
					} else {
						sb.append("第" + rv.voteIndex + "轮投票" + (rv.isSuccess ? "成功。" : "失败。") + rv.approveNum + "人赞成，"
								+ rv.opposeNum + "人反对。\n");
					}
				}

				return sb.toString();
			}

			// 查看任务结果
			if (TASK_TEXT.equals(postMessage)) {
				// 未加入房间
				if (StringUtils.isEmpty(currentGameRoomNO))
					return "你都没加入游戏，看个奶子投票！";

				GameStatus status = currentGame.status;
				// 已加入房间,但是人还没满
				if (status == GameStatus.未开始) {
					int playersNum = PlayersInGame.countByGame(currentGame);
					return "游戏还没开始，" + currentGame.playerNum + "缺" + (currentGame.playerNum - playersNum);
				}

				List<Round> roundList = Round.listByGame(currentGame);
				StringBuffer sb = new StringBuffer();
				sb.append("现在是第" + currentGame.roundIndex + "轮任务，需" + currentRound.actionPlayerNum + "组队执行任务，");

				if (status == GameStatus.投票)
					sb.append("目前是投票阶段，请输入[投票]查看投票详情。\n");
				else if (status == GameStatus.执行)
					sb.append("目前投票已经结束，请组队的人员执行任务，已经有" + (currentRound.succNum + currentRound.failedNum) + "人执行了任务。\n");

				for (Round round : roundList) {
					sb.append("第" + round.roundIndex + "轮任务共" + round.actionPlayerNum + "执行任务\n");
					if (round.isSuccess != null) {
						sb.append("任务" + (round.isSuccess ? "成功" : "失败"));
						if (round.failedNum == 0)
							sb.append(",没有人破坏任务\n");
						else
							sb.append(round.failedNum + "人破坏任务\n");

						List<Action> actions = Action.listByRound(round);
						sb.append("成员为");
						actions.forEach(row -> sb.append(row.player.name + " "));
						sb.append("\n\n");
					} else {
						if (round.roundIndex == currentRound.roundIndex)
							sb.append("任务进行中\n\n");
						else
							sb.append("任务未进行\n\n");
					}
				}

				return sb.toString();
			}

			// 查看房间成员
			if (MEMBER_TEXT.equals(postMessage)) {
				// 未加入房间
				if (StringUtils.isEmpty(currentGameRoomNO)) {
					return "你都没加入游戏，看个奶子成员！";
				}

				// 获取玩家列表
				List<PlayersInGame> pigs = PlayersInGame.listByGame(currentGame);
				return memberInfo(pigs);
			}

			// 查看个人身份
			if (IDENTITY_TEXT.equals(postMessage)) {
				// 未加入房间
				if (StringUtils.isEmpty(currentGameRoomNO)) {
					return "你都没加入游戏，看个奶子身份！";
				}

				GameStatus status = currentGame.status;
				// 已加入房间,但是人还没满
				if (status == GameStatus.未开始) {
					int playersNum = PlayersInGame.countByGame(currentGame);
					return "游戏还没开始，" + currentGame.playerNum + "缺" + (currentGame.playerNum - playersNum);
				}

				// 获取玩家列表
				List<PlayersInGame> pigs = PlayersInGame.listByGame(currentGame);
				// 获取个人身份
				PlayersInGame pig = pigs.stream().filter(row -> row.player.openId.equals(openid)).findFirst().get();
				return identityInfo(pigs, pig);
			}

			return responseMessage;
		} catch (Exception e) {
			Logger.error("processRequest error:%s", e);
			return "服务器异常:" + e.getMessage();
		}
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
		memberInfo.append("\n" + configureInfo(pigs.get(0).game));
		return memberInfo.toString();
	}

	/**
	 * 
	 * @Description:配置信息
	 * @Author: huchao 295971796@qq.com
	 * @CreateDate: 2016年4月22日
	 * @return String 返回类型
	 */
	private static String configureInfo(Game game) {
		String configure = "\n房间的配置   ";
		if (game.playerNum == 5)
			configure += "5人：梅林、派西维尔、忠臣*1  vs 莫甘娜、刺客";
		else if (game.playerNum == 6)
			configure += "6人：梅林、派西维尔、忠臣*2  vs 莫甘娜、刺客";
		else if (game.playerNum == 7)
			configure += "7人：梅林、派西维尔、忠臣*2  vs 莫甘娜、奥伯伦、刺客";
		else if (game.playerNum == 8)
			configure += "8人：梅林、派西维尔、忠臣*3  vs 莫甘娜、刺客、爪牙";
		else if (game.playerNum == 9)
			configure += "9人：梅林、派西维尔、忠臣*4  vs 莫德雷德、莫甘娜、刺客";
		else if (game.playerNum == 10)
			configure += "10人：梅林、派西维尔、忠臣*4  vs 莫德雷德、莫甘娜、奥伯伦、刺客";
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

		identityInfo.append("\n" + configureInfo(pig.game));
		return identityInfo.toString();
	}

}
