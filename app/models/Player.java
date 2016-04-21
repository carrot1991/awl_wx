package models;

import javax.persistence.Entity;

/**
 * 
 * @Description:玩家
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class Player extends BaseModel {

	public String name;

	public String openId;

	public static Player getByOpenId(String openId) {
		return Player.find("openId = ?", openId).first();
	}

}
