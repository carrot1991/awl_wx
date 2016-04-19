package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * 
 * @Description:一局游戏
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class Game extends BaseModel {

	public int playerNum;

	public int roundIndex;

	public String roomNum;// 游戏时的房间号，可能重合

	@Enumerated(EnumType.STRING)
	public GameStatus status;// 游戏状态

	public enum GameStatus {
		未开始("0"), 进行中("1"), 已结束("2");

		public String value;

		private GameStatus(String value) {
			this.value = value;
		}
	}

}
