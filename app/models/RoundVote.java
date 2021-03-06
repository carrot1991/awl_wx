package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.StringUtils;

import play.db.jpa.JPA;

/**
 * 
 * @Description:回合的投票环节
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class RoundVote extends BaseModel {

	@ManyToOne
	public Round round;

	public int voteIndex;// 本回合的投票index

	public Boolean isSuccess;// 投票是否成功

	public int approveNum;// 赞成的人数

	public String approveName;// 赞成的玩家

	public int opposeNum;// 反对的人数

	public String opposeName; // 反对的玩家

	public static Long count(Round round) {
		return RoundVote.count("isDeleted = 0 and round = ? ", round);
	}

	public static List<RoundVote> list(Round round) {
		return RoundVote.find("isDeleted = 0 and round = ? ", round).fetch();
	}

	public static RoundVote getCurrent(Round round) {
		return RoundVote.find("isDeleted = 0 and round = ? order by voteIndex desc", round).first();
	}

	public static RoundVote add(Round round) {
		Long count = RoundVote.count("isDeleted = 0 and round = ? ", round);
		RoundVote rv = new RoundVote();
		rv.round = round;
		rv.voteIndex = count.intValue() + 1;
		rv = rv.save();
		return rv;
	}

	public static RoundVote update(Round round, Player player, boolean isApprove) {
		RoundVote rv = getCurrent(round);
		JPA.em().lock(rv, LockModeType.WRITE);
		if (isApprove) {
			rv.approveNum = rv.approveNum + 1;
			rv.approveName = StringUtils.isBlank(rv.approveName) ? player.name + " "
					: rv.approveName + player.name + " ";
		} else {
			rv.opposeNum = rv.opposeNum + 1;
			rv.opposeName = StringUtils.isBlank(rv.opposeName) ? player.name + " " : rv.opposeName + player.name + " ";
		}

		if (round.game.playerNum == rv.approveNum + rv.opposeNum) {
			if (rv.approveNum > rv.opposeNum)
				rv.isSuccess = true;
			else
				rv.isSuccess = false;
		}
		rv = rv.save();

		return rv;
	}

}
