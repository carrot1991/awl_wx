package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

@Entity
public class PlayersInGame extends BaseModel {

	@ManyToOne
	public Game game;

	@ManyToOne
	public Player player;

	@Enumerated(EnumType.STRING)
	public Role role;

	public enum Role {
		梅林("1"), 派西维尔("2"), 亚瑟的忠臣("3"), 莫德雷德("4"), 莫甘娜("5"), 奥伯伦("6"), 刺客("7"), 莫德雷德的爪牙("8");

		public String value;

		private Role(String value) {
			this.value = value;
		}

		public boolean isGoodMan() {
			boolean res = false;
			if (this == Role.梅林 || this == Role.派西维尔 || this == Role.亚瑟的忠臣)
				res = true;
			return res;
		}
	}

}
