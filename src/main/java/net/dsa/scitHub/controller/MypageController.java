package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@Controller
@RequiredArgsConstructor
@Slf4j
public class MypageController {

    private final UserService us;

	@Value("${board.uploadPath}")
	String uploadPath;	// 프로필파일 저장 경로

    // 마이페이지 요청
    @GetMapping({"/mypage", "/mypage/info"})
    public String myPage(@AuthenticationPrincipal UserDetails user, Model model) {
        // 사이드바 메뉴 아이템 설정
        List<MenuItem> menuItems = List.of(
            new MenuItem("기본 정보", "/mypage/info"),
            new MenuItem("쪽지함", "/mypage/messages")
        );
        model.addAttribute("menuItems", menuItems);

        // 회원 정보 불러오기
        try {
			MypageDTO dto = us.getMemberInfo(user.getUsername());
			model.addAttribute("user",dto);
			log.debug("회원 정보: {}", dto);
		} catch (Exception e) {
			log.debug("회원 정보 조회 실패..");
		}

        return "mypage/info"; // templates/mypage/info.html
    }

    // 쪽지함 페이지 요청
    @GetMapping("/mypage/messages")
    public String messagesPage(Model model) {
        // 사이드바 메뉴 아이템 설정
        List<MenuItem> menuItems = List.of(
            new MenuItem("기본 정보", "/mypage/info"),
            new MenuItem("쪽지함", "/mypage/messages")
        );
        model.addAttribute("menuItems", menuItems);
        return "mypage/messages";
    }

   /**
	 * 개인정보 수정 폼에서 전달된 값 처리
	 * @param user 로그인 사용자의 인증 정보
	 * @param memberDTO 수정폼에서 입력한 값	
	 * @return 메인화면으로
	 */
	@PostMapping("/mypage/info")
	public String info(@AuthenticationPrincipal UserDetails user, MypageDTO dto, @RequestParam(name = "avatar", required = false) MultipartFile upload) {
		
		log.debug("수정폼에서 전달된 값: {}", dto);
		dto.setUserName(user.getUsername());
		
		try {
			us.edit(dto, uploadPath, upload);
			log.debug("수정 성공");
		} catch (Exception e) {
			log.debug("수정 실패..");
		}
		
		return "redirect:/mypage/info";
	}
    
}
