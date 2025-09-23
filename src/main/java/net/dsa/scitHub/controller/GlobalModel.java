package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.dto.NotificationDTO;
import net.dsa.scitHub.service.NotificationService;
import net.dsa.scitHub.service.UserService;

/**
 * 전역 모델 주입
 * - 모든 뷰 렌더링 시 공통으로 필요한 값들을 내려줍니다.
 */
@ControllerAdvice(assignableTypes = {
    AdminController.class,
    ArchiveController.class,
    CalendarApiController.class,
    CalendarController.class,
    CkEditor5Controller.class,
    ClassroomController.class,
    CommunityController.class,
    HomeController.class,
    MessageController.class,
    MypageController.class,
    NotificationReadController.class,
    ReservationApiController.class,
    UserController.class
})
@RequiredArgsConstructor
@Slf4j
public class GlobalModel {

    private final UserService us;
    private final NotificationService ns;

    /**
     * 현재 로그인 사용자의 최신 정보 및 알림 데이터를 모든 모델에 주입
     */
    @ModelAttribute
    public void addCommonAttributes(Model model, @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return; // 로그인 안 된 페이지 대응
        }

        try {
            MypageDTO currentUserDTO = us.getMemberInfo(user.getUsername());
            log.debug("현재 로그인 사용자: {}", currentUserDTO);
            model.addAttribute("currentUser", currentUserDTO);

            if (currentUserDTO != null) {
                long unreadCount = ns.getUnreadNotificationCount(currentUserDTO.getUserId());
                List<NotificationDTO> notifications = ns.getRecentNotifications(currentUserDTO.getUserId(), 10);

                model.addAttribute("unreadCount", unreadCount);
                model.addAttribute("notifications", notifications);
                log.debug("알림 데이터 주입됨: unreadCount={}, notificationSize={}", unreadCount, notifications.size());
            }
        } catch (Exception e) {
            log.warn("글로벌 모델 주입 중 오류 발생", e);
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
