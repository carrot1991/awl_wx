package controllers;

import cn.bran.play.JapidController;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;
import play.mvc.Util;

public class BaseController extends JapidController {
	/** 分页默认每页查询数量 **/
	public static final int DEFAULT_LIST_SIZE = 10;
	/** 分页默认每页查询数量 **/
	public static final int PAGE_SIZE = 10;
	/** 记录当前登陆用户加密id **/
	public static final String CURRENT_PERSON_ID = "currentPersonId";
	/** 记录保持登陆用户加密id **/
	public static final String KEEP_PERSON_ID = "keepPersonId";

	/** 记录当前登陆用户加密id **/
	public static final String CURRENT_SYSPERSON_ID = "currentSysPersonId";
	/** 记录保持登陆用户加密id **/
	public static final String KEEP_SYSPERSON_ID = "keepSysPersonId";

	/** 独立APP访问标记 **/
	public static final String APP_TAG = "appTag";

	/**
	 * @Description:底部切换菜单枚举
	 * @Author: suzui
	 * @CreateDate: 2015.8.14
	 * @return
	 */
	public static enum NavType {
		index, search, home, cart
	}

	@Util
	protected static void clearSysPersonIdToBrowser(Request request) {
		Session.current().remove(request.controller + CURRENT_SYSPERSON_ID);
	}

	@Util
	protected static void setAppTag() {
		Response.current().setCookie(APP_TAG, Boolean.TRUE.toString());
	}
}