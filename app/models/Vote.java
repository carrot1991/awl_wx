package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Vote extends BaseModel {

	@ManyToOne
	public Player player;

	@ManyToOne
	public Round round;

	public boolean isApprove;

	public static Vote get(Player player, Round round) {
		return Vote.find("isDeleted = 0 and round = ? and player = ? ", round, player).first();
	}

	public static Vote add(Player player, Round round, boolean isApprove) {
		Vote vote = Vote.get(player, round);
		if (vote == null) {
			vote = new Vote();
			vote.player = player;
			vote.round = round;
			vote.isApprove = isApprove;
			vote = vote.save();
		}
		return vote;
	}

}
