package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Vote extends BaseModel {

	@ManyToOne
	public Player player;

	@ManyToOne
	public RoundVote roundVote;

	public boolean isApprove;

	public static Vote get(Player player, RoundVote roundVote) {
		return Vote.find("isDeleted = 0 and roundVote = ? and player = ? ", roundVote, player).first();
	}

	public static Vote add(Player player, RoundVote roundVote, boolean isApprove) {
		Vote vote = Vote.get(player, roundVote);
		if (vote == null) {
			vote = new Vote();
			vote.player = player;
			vote.roundVote = roundVote;
			vote.isApprove = isApprove;
			vote = vote.save();
		}
		return vote;
	}

}
