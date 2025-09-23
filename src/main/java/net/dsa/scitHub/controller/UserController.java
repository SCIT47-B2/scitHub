package net.dsa.scitHub.controller;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.UserDTO;
import net.dsa.scitHub.service.UserService;

@Controller
@Slf4j
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final UserService us;


	/**
	 * 회원가입 페이지로 이동
	 * @return joinForm.html
	 */
	@PostMapping("signup")
	public String join(UserDTO user) {
		
		log.debug("전달된 회원정보: {}", user);
		
		try {
			us.join(user);
			log.debug("가입성공!");
		} catch (Exception e) {
			log.debug("가입실패..");
		}
		
		return "redirect:/user/landingPage?signup=success";
	}

	/**
	 * 로그인/회원가입 선택 페이지로 이동
	 * 시큐리티에서 인증하기 위해서 보내는 경로값과 일치해야 함.
	 * @return loginForm.html
	 */
	@GetMapping("landingPage")
	public String landingPage(@RequestParam(value = "error", required = false) String error,
							  @RequestParam(value = "signup", required = false) String signup,
							  HttpServletRequest request, Model model) {
		if (error != null) {
			HttpSession session = request.getSession(false);
			String errorMessage = "ログインに失敗しました。再度お試しください。"; // 기본 에러 메시지
			if (session != null) {
				AuthenticationException ex = (AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
				if (ex instanceof DisabledException) {
					errorMessage = "アカウントがブロックされています。管理者にお問い合わせください。";
				} else if (ex instanceof BadCredentialsException) {
					errorMessage = "ユーザーIDまたはパスワードが一致しません。";
				}
			}
			model.addAttribute("errorMessage", errorMessage);
		}
		if ("success".equals(signup)) {
			model.addAttribute("signupMessage", "会員登録が完了しました！ご登録のIDでログインしてください。");
		}
		return "user/landingPage";
	}

	/**
	 * 로그인 전용 페이지로 이동
	 * @return login.html
	 */
	@GetMapping("loginPage")
	public String loginPage() {
		return "user/login";
	}
    
	/** 아이디 중복확인(팝업: 결과만 표시) */
	@GetMapping("/idCheck")
	public String idCheck(@RequestParam("searchId") String searchId, Model model){
		
		log.debug("searchId: {}", searchId);
		// 해당 아이디로 가입 가능 여부
		boolean available = us.idCheck(searchId);

		log.debug("해당 아이디로 가입 가능 여부: {}", available);
		
		model.addAttribute("searchId", searchId);
		model.addAttribute("result", available); // true=사용가능
		return "user/idCheck"; // templates/user/idCheck.html
	}
}
