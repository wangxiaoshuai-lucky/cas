package cn.wzy.sys.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@RestController
public class UserController {


	@Autowired
	private RestTemplate template;

	@Resource
	private HttpServletRequest request;

	@GetMapping("/hello")
	public String hello() {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return "用户未登录";
		}
		String token = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals("x-token")) {
				token = cookie.getValue();
			}
		}
		ResponseEntity<String> username = template.getForEntity("http://localhost:8080/uc/authForToken?token=" + token, String.class);
		String str = username.getBody();
		if (str == null || str.equals("用户未登录")) {
			return "用户未登录";
		}
		return "hello:" + str;
	}


}
