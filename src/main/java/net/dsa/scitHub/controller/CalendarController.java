package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.dsa.scitHub.dto.MenuItem;

@Controller
public class CalendarController {
    // 캘린더 페이지 요청
    @GetMapping({"/calendar", "/calendar/schedule"})
    public String calendarPage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("일정", "/calendar/schedule"),
            new MenuItem("디데이", "/calendar/dDay")
        );
        model.addAttribute("menuItems", menuItems);
        return "calendar/schedule"; // templates/calendar/schedule.html
    }
}
