package utils.wx;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import play.Logger;

public class WxUtils {
	public static String time_token = "";
	public static final String GET_OPENID_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
	public static final String GET_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
	public static final String SET_MENU_URL = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=%s";
	public static final String DEL_MENU_URL = "https://api.weixin.qq.com/cgi-bin/menu/delete?access_token=%s";
	// public static final String wxAppSecret =
	// Play.configuration.getProperty("wxAppSecret");
	// public static final String wxAppId =
	// Play.configuration.getProperty("wxAppId");

	public static final String wxAppSecret = "956a98e4761a46c7cd3b0ec4574badee ";
	public static final String wxAppId = "wx4a8a04d8658cfae9";

	// 通过code获取openid
	public static String GetOpenIdByCode(String code) {
		try {
			if (code == null || code.equals("")) {
				return null;
			}
			String url_token = String.format(GET_OPENID_URL, wxAppId, wxAppSecret, code);
			String result = RequestByGet(url_token);
			if (result.contains("errcode")) {
				return null;
			} else {
				JSONObject jsonObject = JSONObject.parseObject(result);
				return jsonObject.getString("openid");
			}
		} catch (JSONException e) {
			return null;
		}

	}

	// 获取通行证
	public static String GetAccessToken() {
		try {
			String url_token = String.format(GET_TOKEN_URL, wxAppId, wxAppSecret);
			url_token = RequestByGet(url_token);
			if (url_token.contains("access_token")) {
				JSONObject jsonObject = JSONObject.parseObject(url_token);
				url_token = jsonObject.getString("access_token");
				Set(url_token);
			}
			return url_token;
		} catch (JSONException e) {
			return "";
		}
	}

	// 设置最新菜单
	public static String SetMenu() {
		String POST_URL = String.format(SET_MENU_URL, GetAccessToken());
		MenuDefine menuDefine = new MenuDefine();
		String jsonString = menuDefine.createMenu();
		String res = RequestByPost(POST_URL, jsonString);
		return res;
	}

	// 删除菜单
	public static String DelMenu() {
		String url_Menu_Delete = String.format(DEL_MENU_URL, GetAccessToken());
		return RequestByGet(url_Menu_Delete);
	}

	public static String RequestByGet(String url) {
		String line = "";
		String temp = "";
		try {
			URL getUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
			connection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while ((temp = reader.readLine()) != null) {
				line = line + temp;
			}
			reader.close();
			return line;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "fail";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "fail";
		}
	}

	public static String RequestByPost(String url, String content) {
		String line = "";
		if (StringUtils.isBlank(content))
			return "fail";
		try {
			// Post请求的url，与get不同的是不需要带参数
			URL postUrl = new URL(url);
			// 打开连接
			HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
			// 打开读写属性，默认均为false
			connection.setDoOutput(true);
			connection.setDoInput(true);
			// 设置请求方式，默认为GET
			connection.setRequestMethod("POST");
			// 配置连接的Content-type，配置为application/x-
			// www-form-urlencoded的意思是正文是urlencoded编码过的form参数，下面我们可以看到我们对正文内容使用URLEncoder.encode进行编码
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("charset", "UTF-8");
			// 连接，从postUrl.openConnection()至此的配置必须要在 connect之前完成，
			// 要注意的是connection.getOutputStream()会隐含的进行调用 connect()，所以这里可以省略
			// connection.connect();
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			// DataOutputStream.writeBytes将字符串中的16位的 unicode字符以8位的字符形式写道流里面
			out.write(content.getBytes("utf-8"));
			out.flush();
			out.close(); // flush and close
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String temp = "";
			while ((temp = reader.readLine()) != null) {
				line = line + temp;
			}
			reader.close();
			return line;
		} catch (Exception e) {
			Logger.error(e.getMessage());
			return "fail";
		}
	}

	/**
	 * 将InputStream转换成某种字符编码的String
	 * 
	 * @param in
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	final static int BUFFER_SIZE = 4096;

	public static String InputStreamTOString(InputStream in, String encoding) throws Exception {

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] data = new byte[BUFFER_SIZE];
		int count = -1;
		while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
			outStream.write(data, 0, count);

		data = null;
		return new String(outStream.toByteArray(), encoding);
	}

	/**
	 * 设置access_Token
	 * 
	 * @param access_Token
	 */
	public static void Set(String access_Token) {
		Calendar cal = Calendar.getInstance();
		String time = cal.get(Calendar.YEAR) + ":" + (cal.get(Calendar.MONTH) + 1) + ":"
				+ cal.get(Calendar.DAY_OF_MONTH) + ":" + cal.get(Calendar.HOUR) + "=" + access_Token;
		time_token = time;
	}

	/**
	 * 比较access_Token是否有效
	 * 
	 * @return
	 */
	public static String Compare() {
		if (time_token == "") {
			return "no";
		} else {
			Calendar cal = Calendar.getInstance();
			String[] strs = time_token.split("=");
			String[] times = strs[0].split(":");
			int y = cal.get(Calendar.YEAR) - Integer.parseInt(times[0]);
			int m = cal.get(Calendar.MONTH) + 1 - Integer.parseInt(times[1]);
			int d = cal.get(Calendar.DAY_OF_MONTH) - Integer.parseInt(times[2]);
			int h = cal.get(Calendar.HOUR) - Integer.parseInt(times[3]);

			if (y > 0 || m > 0 || d > 0 || h > 1) {
				return "no";
			} else {
				return strs[1];
			}

		}
	}

	public static void main(String[] args) {
		System.out.println(SetMenu());
	}

}
