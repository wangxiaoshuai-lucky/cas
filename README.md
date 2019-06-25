## 单点登录：
### 统一认证平台UC：
职责：登录和鉴权
* 登录:生成token信息，将信息存到UC域名下的cookies中
* 鉴权:验证token的正确性，子系统每个请求会首先到UC来验证token是否有效
* 返回token信息:子系统会使用JSONP跨域访问此接口获取token信息
### 各个业务子系统：
* JSONP跨域获取UC中的token信息并存到自己域名下的cookies中
* 业务接口会首先在UC中进行token的鉴权
### 例子
UC系统：
* localhost:8080/uc/login会将登录用户生成token并存到UC域的cookies中
* localhost:8080/uc/auth会根据请求cookies中的token信息返回用户的信息，包括会话信息(token)
* http://localhost:8080/uc/authForToken?token=**** 验证token是否有效，供子系统访问
子系统:
* 前端JSONP访问UC的鉴权接口：此时UC系统是登录过的，含有token信息，
所以业务系统跨域请求UC的时候会返回用户信息以及会话信息token，并将token存到业务系统域的cookies中
~~~
<script>
    function call(username, token) {
        console.log(username);
        console.log(token);
        document.cookie="x-token="+token;
    }
</script>
<script type="text/javascript" src="http://localhost:8080/uc/auth?callback=call"></script>
~~~
* 业务接口收到请求前访问UC检验token的有效性
~~~
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
~~~
### 鉴权细节
因为涉及到JSONP的跨域请求，请求成功会返回用户信息和会话token，容易造成json劫持，窃取用户信息  
解决方案：JSONP跨域访问检验调用方的referer，在白名单内的系统才能跨域成功
~~~
	private final String[] referers = new String[]{
		"http://127.0.0.1",
		"http://localhost",
	};
	
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
~~~