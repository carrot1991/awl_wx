package models;

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

	public boolean isSuccess;// 投票是否成功

	public int approveNum;// 赞成的人数

	public int opposeNum;// 反对的人数

	public static Long count(Round round) {
		return Round.count("isDeleted = 0 and round = ? ", round);
	}

}
