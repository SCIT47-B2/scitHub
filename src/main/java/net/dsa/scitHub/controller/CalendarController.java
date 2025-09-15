package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.security.AuthenticatedUser;
import net.dsa.scitHub.service.EventService;


@Controller
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final EventService es;

    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("スケジュール", "/calendar/schedule"),
            new MenuItem("D-Day", "/calendar/dDay")
        );
    }

    // 캘린더 페이지 요청
    @GetMapping({"/calendar", "/calendar/schedule"})
    public String calendarPage(
            @AuthenticationPrincipal AuthenticatedUser user,
            Model model
        ) {
        // 비 로그인 상태 (userDetails가 null인지) 먼저 확인
        if(user == null) {
            // 비로그인시 landingPage로 리다이렉트
            return "redirect:/user/landingPage";
        }
        
        log.debug("유저 정보가 담겼는지 확인 {}",user.getId());
        log.debug("유저 정보가 담겼는지 확인 {}",user.getRoleName());

        Integer userId = es.convertUsernameToUserId(user.getId());

        model.addAttribute("currentUsername", user.getId());
        model.addAttribute("currentUserRole", user.getRoleName());
        model.addAttribute("currentUserId", userId);

        return "calendar/schedule"; // templates/calendar/schedule.html
    }

    // 디데이 페이지 요청
    @GetMapping("/calendar/dDay")
    public String dDayPage() {
        return "calendar/dDay"; // templates/calendar/dDay.html
    }
}
