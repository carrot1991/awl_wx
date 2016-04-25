package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Round extends BaseModel {

	@ManyToOne
	public Game game;

	public int roundIndex;// 回合顺序 1,2,3,4,5

	public Boolean isSuccess;// 回合是否成功 failedNum ==0时，isSuccess为true

	public Integer succNum;// 成功执行的人数

	public Integer failedNum;// 破坏任务的人数

	public static List<Round> init(Game game) {
		List<Round> rounds = new ArrayList<Round>();
		for (int i = 1; i <= 5; i++) {
			Round round = new Round();
			round.game = game;
			round.roundIndex = i;
			round = round.save();
			rounds.add(round);
		}
		return rounds;
	}

	public static Round fetchByGame(Game game, int roundIndex) {
		return Round.find("isDeleted = 0 and game = ? and roundIndex = ? ", game, roundIndex).first();
	}

	public static List<Round> listByGame(Game game) {
		return Round.find("isDeleted = 0 and game = ? ", game).fetch();
	}

	public static Long count(Game game, boolean isSuccess) {
		return Round.count("isDeleted = 0 and game = ? and isSuccess = ?", game, isSuccess);
	}

}
