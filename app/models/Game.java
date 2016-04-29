package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.LockModeType;

import play.cache.Cache;
import play.db.jpa.JPA;
import services.GameCoreService;

/**
 * 
 * @Description:一局游戏
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class Game extends BaseModel {

	public int playerNum;

	public int roundIndex;

	public String roomNO;// 游戏时的房间号，可能重合

	public int succNum;// 成功执行的人数

	public int failedNum;// 破坏任务的人数

	public String result;

	@Enumerated(EnumType.STRING)
	public GameStatus status;// 游戏状态

	public enum GameStatus {
		未开始("0"), 投票("1"), 执行("2"), 已结束("3"), 已终止("4");

		public String value;

		private GameStatus(String value) {
			this.value = value;
		}
	}

	public static Game init(int playerNum, String roomNO) {
		Game game = new Game();
		game.playerNum = playerNum;
		game.roomNO = roomNO;
		game.roundIndex = 0;
		game.status = GameStatus.未开始;
		game = game.save();
		if (game != null)
			Cache.add(GameCoreService.CACHE_KEY_GAME + roomNO, game);
		return game;
	}

	public static Game exit(Game gameToExit, String result) {
		Game game = Game.findById(gameToExit.id);
		game.status = GameStatus.已终止;
		game.result = result;
		game = game.save();
		// db操作成功后 清除cache数据
		Cache.safeDelete(GameCoreService.CACHE_KEY_GAME + game.roomNO);
		Cache.safeDelete(GameCoreService.CACHE_KEY_GAMEROUND + game.roomNO);
		Cache.safeDelete(GameCoreService.CACHE_KEY_GAMEPLAYERNUM + game.roomNO);
		return game;
	}

	public static Game start(Game gameToStart) {
		Game game = Game.findById(gameToStart.id);
		game.status = GameStatus.投票;
		game.roundIndex = 1;
		game = game.save();
		Cache.set(GameCoreService.CACHE_KEY_GAME + game.roomNO, game);
		return game;
	}

	public static Game startAction(Game gameToStart) {
		Game game = Game.findById(gameToStart.id);
		game.status = GameStatus.执行;
		game = game.save();
		Cache.set(GameCoreService.CACHE_KEY_GAME + game.roomNO, game);
		return game;
	}

	public static Game nextRound(Round currentRound) {
		Game game = Game.findById(currentRound.game.id);
		if (game != null) {
			JPA.em().lock(game, LockModeType.WRITE);
			game.roundIndex = currentRound.roundIndex + 1;
			game.status = GameStatus.投票;
			if (currentRound.isSuccess)
				game.succNum = game.succNum + 1;
			else
				game.failedNum = game.failedNum + 1;
			game = game.save();
			Cache.set(GameCoreService.CACHE_KEY_GAME + game.roomNO, game);
		}
		return game;
	}

}
