package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.dsa.scitHub.dto.MenuItem;

@Controller
public class ArchiveController {
    // 아카이브 페이지 요청
    @GetMapping({"/archive", "/archive/companyReview"})
    public String archivePage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("회사 리뷰", "/archive/companyReview"),
            new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);
        return "archive/companyReview"; // templates/archive/companyReview.html
    }
}
