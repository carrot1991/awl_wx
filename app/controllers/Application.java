package controllers;

import play.Logger;
import play.mvc.Http.Request;
import services.GameCoreService;

public class Application extends BaseController {

	public static void index(String openId) {
		renderText("run");
	}

	public static void wx() {
		// 调用核心业务类接收消息、处理消息
		Logger.info("wx post");
		String respMessage = GameCoreService.process(Request.current());
		renderText(respMessage);
	}

}