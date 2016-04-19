package utils.wx;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class MenuDefine {

	public static String createMenu() {
		JSONObject json = new JSONObject();
		JSONArray buttons = new JSONArray();

		JSONObject button1 = new JSONObject();
		button1.put("name", "成员");
		button1.put("type", "click");
		button1.put("key", "member");
		buttons.add(button1);

		JSONObject button2 = new JSONObject();
		button2.put("name", "身份");
		button2.put("type", "click");
		button2.put("key", "role");
		buttons.add(button2);

		JSONObject button3 = new JSONObject();
		button3.put("name", "投票");

		JSONArray button3Chilren = new JSONArray();

		JSONObject button4 = new JSONObject();
		button4.put("name", "赞成");
		button4.put("click", "approve");
		button3Chilren.add(button4);

		JSONObject button5 = new JSONObject();
		button5.put("name", "拒绝");
		button5.put("click", "oppose");
		button3Chilren.add(button5);

		button3.put("sub_button", button3Chilren);
		buttons.add(button3);

		JSONObject button6 = new JSONObject();
		button3.put("name", "行动");

		JSONArray button6Chilren = new JSONArray();

		JSONObject button7 = new JSONObject();
		button7.put("name", "执行");
		button7.put("click", "execute");
		button6Chilren.add(button7);

		JSONObject button8 = new JSONObject();
		button8.put("name", "破坏");
		button8.put("click", "damage");
		button6Chilren.add(button8);

		button6.put("sub_button", button6Chilren);
		buttons.add(button6);
		json.put("button", buttons);

		return json.toJSONString();
	}
}
