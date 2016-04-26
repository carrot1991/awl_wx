package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * 
 * @Description:执行环节
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class Action extends BaseModel {

	@ManyToOne
	public Round round;

	@ManyToOne
	public Player player;

	public boolean isSuccess; // 是否成功执行

	public static List<Action> listByRound(Round round) {
		return Action.find("round = ? and isDeleted = 0 ", round).fetch();
	}

	public static Action get(Player player, Round round) {
		return Action.find("isDeleted = 0 and round = ? and player = ? ", round, player).first();
	}

	public static Action add(Player player, Round round, boolean isSuccess) {
		Action action = Action.get(player, round);
		if (action == null) {
			action = new Action();
			action.player = player;
			action.round = round;
			action.isSuccess = isSuccess;
			action = action.save();
		}
		return action;
	}

}
