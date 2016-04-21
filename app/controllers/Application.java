package controllers;

import java.io.PrintWriter;

import play.Logger;
import play.mvc.Http.Request;
import services.GameCoreService;
import utils.wx.SignUtil;
import utils.wx.WxUtils;

public class Application extends BaseController {

	public static void index(String openId) {
		renderText("run");
	}

	public static void wx() {
		// 调用核心业务类接收消息、处理消息
		String method = Request.current().method;
		if (method.equals("POST")) {
			String respMessage = GameCoreService.process(Request.current());
			renderText(respMessage);
		} else if (method.equals("GET")) {
			String signature = request.params.get("signature");
			// 时间戳
			String timestamp = request.params.get("timestamp");
			// 随机数
			String nonce = request.params.get("nonce");
			// 随机字符串
			String echostr = request.params.get("echostr");
			response.setContentTypeIfNotSet("text/html");
			if (SignUtil.checkSignature(WxUtils.wxToken, signature, timestamp, nonce)) {
				try {
					PrintWriter writer = new PrintWriter(response.out);
					writer.print(echostr);
					writer.close();
					writer = null;
				} catch (Exception e) {
					Logger.error(e.getMessage());
				}
			}
		}
	}

}