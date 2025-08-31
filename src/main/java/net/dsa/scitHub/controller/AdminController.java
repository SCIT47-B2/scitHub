package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.dsa.scitHub.dto.MenuItem;

@Controller
public class AdminController {
    // 관리자 페이지 요청
    @GetMapping({"/admin", "/admin/announcement"})
    public String adminPage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("공지사항", "/admin/announcement"),
            new MenuItem("문의", "/admin/support"),
            new MenuItem("신고", "/admin/report"),
            new MenuItem("회원관리", "/admin/managementUser")
        );
        model.addAttribute("menuItems", menuItems);
        return "admin/announcement"; // templates/admin/announcement.html
    }
}
