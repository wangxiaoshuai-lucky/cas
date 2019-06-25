package cn.wzy.uc.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class UserController {

	private static ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

	private final String[] referers = new String[]{
		"http://127.0.0.1",
		"http://localhost",
	};

	@Resource
	private HttpServletRequest request;

	@Resource
	private HttpServletResponse response;


	@GetMapping("/login")
	public String login(String username, String password) {
		if (username == null)
			return "登录失败";
		String token = generatorUID();
		response.addCookie(new Cookie("x-token", token));
		tokens.putIfAbsent(token, username);
		return "hello:" + username;
	}


	/**
	 * 返回token信息，用于跨域的token传递
	 * 检验跨域的白名单列表，防止json劫持
	 *
	 * @param callback
	 * @return
	 */
	@GetMapping("/auth")
	public String auth(String callback) {
		if (notAllowAuth()) {
			return "禁止该网点单点登录";
		}
		String token = getToken(request.getCookies());
		if (token == null) {
			return "用户未登录";
		}
		String username = tokens.get(token);
		if (username == null) {
			return "用户未登录";
		}
		return callback + "('" + username + "','" + token + "')";
	}

	/**
	 * 是否禁止该网点跨域
	 *
	 * @return false为不禁止
	 */
	private boolean notAllowAuth() {
		String refer = request.getHeader("referer");
		if (refer != null) {
			boolean failed = true;
			for (String ref : referers) {
				if (refer.startsWith(ref)) {
					failed = false;
				}
			}
			return failed;
		}
		return false;
	}

	/**
	 * 通过token鉴权
	 *
	 * @param token
	 * @return
	 */
	@GetMapping("/authForToken")
	public String authForToken(String token) {
		String username = tokens.get(token);
		if (username == null) {
			return "用户未登录";
		}
		return username;
	}


	/**
	 * 生成唯一token
	 *
	 * @return
	 */
	private String generatorUID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	/**
	 * 获取token字段信息
	 *
	 * @param cookies
	 * @return
	 */
	private String getToken(Cookie[] cookies) {
		if (cookies == null) {
			return null;
		} else {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("x-token")) {
					return cookie.getValue();
				}
			}
			return null;
		}
	}
}
