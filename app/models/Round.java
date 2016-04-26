package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import play.cache.Cache;
import services.GameCoreService;

@Entity
public class Round extends BaseModel {

	@ManyToOne
	public Game game;

	public int roundIndex;// 回合顺序 1,2,3,4,5

	public Boolean isSuccess;// 回合是否成功

	public int actionPlayerNum;// 需要组队行动的人数

	public int succNum;// 成功执行的人数

	public int failedNum;// 破坏任务的人数

	@Transient
	public List<Action> actions;

	public static List<Round> init(Game game) {
		List<Round> rounds = new ArrayList<Round>();
		for (int i = 1; i <= 5; i++) {
			Round round = new Round();
			round.game = game;
			round.roundIndex = i;
			round.actionPlayerNum = getActionPlayerNum(game.playerNum, i);
			round = round.save();
			rounds.add(round);
		}
		// 将第一回合放入缓存
		Cache.add(GameCoreService.CACHE_KEY_GAMEROUND + game.roomNO, rounds.get(0));
		return rounds;
	}

	public static Round fetchCurrentByGame(Game game) {
		return Round.find("isDeleted = 0 and game = ? and roundIndex = ? ", game, game.roundIndex).first();
	}

	public static Round fetchByGame(Game game, int roundIndex) {
		return Round.find("isDeleted = 0 and game = ? and roundIndex = ? ", game, roundIndex).first();
	}

	public static List<Round> listByGame(Game game) {
		return Round.find("isDeleted = 0 and game = ? order by roundIndex asc", game).fetch();
	}

	public static List<Round> listByGame(Game game, Boolean isSuccess) {
		return Round.find("isDeleted = 0 and game = ? and isSuccess = ?", game, isSuccess).fetch();
	}

	public static Long count(Game game, Boolean isSuccess) {
		return Round.count("isDeleted = 0 and game = ? and isSuccess = ?", game, isSuccess);
	}

	public static Round isSuccess(Round roundToUpdate) {
		Round round = Round.find("isDeleted = 0 and id = ?", roundToUpdate.id).first();
		if (round != null && round.actionPlayerNum == round.succNum + round.failedNum) {
			if (round.failedNum == 0
					|| (round.failedNum == 1 && round.actionPlayerNum == 4 && round.game.playerNum == 7)
					|| (round.failedNum == 1 && round.actionPlayerNum == 5))
				round.isSuccess = true;
			else
				round.isSuccess = false;
			round = round.save();
		}
		return round;
	}

	public static Round nextRound(Round currentRound) {
		if (currentRound.roundIndex == 5)
			return null;

		Round round = Round.fetchByGame(currentRound.game, currentRound.roundIndex + 1);
		if (round != null)
			Cache.add(GameCoreService.CACHE_KEY_GAMEROUND + currentRound.game.roomNO, round);
		return round;
	}

	public static Round update(Round roundToUpdate, boolean isSuccess) {
		Round round = Round.find("isDeleted = 0 and id = ?", roundToUpdate.id).first();
		if (isSuccess)
			round.succNum = round.succNum + 1;
		else
			round.failedNum = round.failedNum + 1;

		round = round.save();

		if (round.actionPlayerNum == round.succNum + round.failedNum) {
			round = Round.isSuccess(round);
		}
		// 将第一回合放入缓存
		Cache.add(GameCoreService.CACHE_KEY_GAMEROUND + round.game.roomNO, round);
		return roundToUpdate;
	}

	private static int getActionPlayerNum(int playerNum, int roundIndex) {
		int num = 0;
		switch (roundIndex) {
		case 1:
			if (playerNum == 5 || playerNum == 6 || playerNum == 7)
				num = 2;
			else
				num = 3;
			break;
		case 2:
			if (playerNum == 5 || playerNum == 6 || playerNum == 7)
				num = 3;
			else
				num = 4;
			break;
		case 3:
			if (playerNum == 5)
				num = 2;
			else if (playerNum == 6)
				num = 4;
			else if (playerNum == 7)
				num = 3;
			else
				num = 4;
			break;
		case 4:
			if (playerNum == 5 || playerNum == 6)
				num = 3;
			else if (playerNum == 7)
				num = 4;
			else
				num = 5;
			break;
		case 5:
			if (playerNum == 5)
				num = 3;
			else if (playerNum == 6 || playerNum == 7)
				num = 4;
			else
				num = 5;
			break;
		default:
			break;
		}

		return num;
	}

}
