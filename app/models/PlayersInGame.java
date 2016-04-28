package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

import play.cache.Cache;
import services.GameCoreService;

@Entity
public class PlayersInGame extends BaseModel {

	@ManyToOne
	public Game game;

	@ManyToOne
	public Player player;

	public int playerIndex;

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

	public static PlayersInGame joinInGame(Player player, Game game) {
		Long count = PlayersInGame.count("game = ?", game);
		if (count >= game.playerNum)
			return null;

		PlayersInGame pig = new PlayersInGame();
		pig.game = game;
		pig.player = player;
		pig.playerIndex = count.intValue() + 1;
		pig = pig.save();

		if (pig != null)
			Cache.add(GameCoreService.CACHE_KEY_PLAYER + player.openId, game.roomNO);

		return pig;
	}

	public static List<PlayersInGame> listByGame(Game game) {
		return PlayersInGame.find("game = ? order by playerIndex asc", game).fetch();
	}

	public static int countByGame(Game game) {
		Long count = PlayersInGame.count("game = ?", game);
		return count.intValue();
	}

	public static PlayersInGame get(Game game, Player player) {
		return PlayersInGame.find("game = ? and player = ?", game, player).first();
	}

	public void updateRole(Role role) {
		this.role = role;
		this.save();
	}

}
