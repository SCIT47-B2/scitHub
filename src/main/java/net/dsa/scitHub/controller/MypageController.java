package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.dsa.scitHub.dto.MenuItem;

@Controller
public class MypageController {
    // 마이페이지 요청
    @GetMapping("/mypage")
    public String myPage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("기본 정보", "/mypage/info"),
            new MenuItem("쪽지함", "/mypage/messages")
        );
        model.addAttribute("menuItems", menuItems);
        return "mypage/info"; // templates/mypage/info.html
    }
}
