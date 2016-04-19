package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class Round extends BaseModel {

	@ManyToOne
	public Game game;

	public int index;// 回合顺序 1,2,3,4,5

	public Boolean isSuccess;// 回合是否成功 failedNum ==0时，isSuccess为true

	public Integer succNum;// 成功执行的人数

	public Integer failedNum;// 破坏任务的人数

}
