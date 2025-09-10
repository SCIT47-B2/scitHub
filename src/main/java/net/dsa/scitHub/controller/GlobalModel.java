package net.dsa.scitHub.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.service.UserService;

/**
 * 전역 모델 주입
 * - 모든 뷰 렌더링 시 공통으로 필요한 값들을 내려줍니다.
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalModel {

    private final UserService userService;

    /**
     * 현재 로그인 사용자의 최신 정보 주입
     * @param user UserDetails - 인증 사용자(미인증이면 null)
     * @return MypageDTO|null  - 로그인 상태면 DTO, 아니면 null
     */
    @ModelAttribute("currentUser")
    public MypageDTO currentUser(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return null; // 로그인 안 된 페이지 대응
        try {
            MypageDTO dto = userService.getMemberInfo(user.getUsername());
            log.debug("currentUser injected: {}", dto);
            return dto;
        } catch (Exception e) {
            log.warn("현재 사용자 조회 실패", e);
            return null;
        }
    }

    /**
     * 현재 요청 URI (메뉴 active 표시용)
     * @param req HttpServletRequest
     * @return String - 예: /scitHub/mypage/info
     */
    @ModelAttribute("activeUri")
    public String activeUri(HttpServletRequest req) {
        String uri = req.getRequestURI();
        log.debug("Active URI: {}", uri);
        return uri;
    }

    /**
     * 컨텍스트 경로
     * @param req HttpServletRequest
     * @return String - 예: /scitHub 또는 ""
     */
    @ModelAttribute("ctx")
    public String ctx(HttpServletRequest req) {
        String ctx = req.getContextPath();
        log.debug("Context Path: {}", ctx);
        return ctx;
    }
}
