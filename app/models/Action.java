package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * 
 * @Description:执行环节
 * @Author: huchao 295971796@qq.com
 * @CreateDate: 2016年4月19日
 */
@Entity
public class Action extends BaseModel {

	@ManyToOne
	public Round round;

	@ManyToOne
	public Player player;

	public Boolean isSuccess; // 是否成功执行

}
