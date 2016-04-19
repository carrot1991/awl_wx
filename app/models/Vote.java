package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Vote extends BaseModel {

	@ManyToOne
	public Player player;

	@ManyToOne
	public Round round;

	public Boolean isApprove;

}
