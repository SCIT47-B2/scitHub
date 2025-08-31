package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MenuItem;

@Controller
@Slf4j
public class ClassroomController {
    // 클래스룸 페이지 요청
    @GetMapping({"/classroom", "/classroom/home"})
    public String classroomPage(Model model) {

        List<MenuItem> menuItems = List.of(
            new MenuItem("홈", "/classroom/home"),
            new MenuItem("강의실", "/classroom/room")
        );

        model.addAttribute("menuItems", menuItems);

        return "classroom/home";
    }
}
