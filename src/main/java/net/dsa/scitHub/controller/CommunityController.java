package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.community.PostDTO;
import net.dsa.scitHub.service.CommunityService;

import org.springframework.web.bind.annotation.PostMapping;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService cs;
    
    // 커뮤니티 페이지 요청
    @GetMapping({"/community", "/community/home"})
    public String communityPage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("게시판 홈", "/community/home"),
            new MenuItem("Q&A", "/community/qna"),
            new MenuItem("강의평", "/community/courseReview")
        );
        model.addAttribute("menuItems", menuItems);
        return "community/home"; // templates/community/home.html
    }

    @GetMapping("/community/writePost")
    public String writePostPage() {
        return "community/writeForm";
    }

    @PostMapping("/community/write")
    public String writePost(PostDTO postDTO) {
        cs.makeNewPost(postDTO);
        return "community/home";
    }
    
}
