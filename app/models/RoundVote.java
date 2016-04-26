package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

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

	public int opposeNum;// 反对的人数

	public static Long count(Round round) {
		return Round.count("isDeleted = 0 and round = ? ", round);
	}

	public static List<RoundVote> list(Round round) {
		return Round.find("isDeleted = 0 and round = ? ", round).fetch();
	}

	public static RoundVote add(Round round) {
		Long count = Round.count("isDeleted = 0 and round = ? ", round);
		RoundVote rv = new RoundVote();
		rv.round = round;
		rv.voteIndex = count.intValue() + 1;
		rv = rv.save();
		return rv;
	}

	public static RoundVote update(Round round, boolean isApprove) {
		RoundVote rv = RoundVote.find("isDeleted = 0 and round = ? order by voteIndex desc", round).first();
		if (isApprove)
			rv.approveNum = rv.approveNum + 1;
		else
			rv.opposeNum = rv.opposeNum + 1;

		if (round.game.playerNum == rv.approveNum + rv.opposeNum) {
			if (rv.approveNum > rv.opposeNum)
				rv.isSuccess = true;
			else
				rv.isSuccess = false;
		}
		return rv.save();
	}

}
