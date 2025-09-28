package net.dsa.scitHub.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.ClassroomDTO;
import net.dsa.scitHub.dto.InstagramPostDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.service.InstagramService;
import net.dsa.scitHub.service.PostService;
import net.dsa.scitHub.service.ReservationService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ClassroomController {

    private final ReservationService rs;
    private final PostService ps;
    private final InstagramService is;

    @ModelAttribute("boardMap")
    public Map<String, String> boardMap() {
        Map<String, String> boardMap = new LinkedHashMap<String,String>();
        // key:value = 영어이름(DB에 있는):일본어이름
        boardMap.put("free", "自由掲示板");
        boardMap.put("it", "IT情報");
        boardMap.put("japanese", "日本語情報");
        boardMap.put("jpCulture", "日本文化&生活情報");
        boardMap.put("job", "就活情報&コツ");
        boardMap.put("hobby", "趣味&旅行&グルメ情報");
        boardMap.put("certificate", "資格情報");
        boardMap.put("graduated", "卒業生掲示板");
        boardMap.put("qna", "Q&A");

        log.debug("게시글 맵 정보 : {}", boardMap);
        return boardMap;
    }

    // 클래스룸 페이지 요청
    @GetMapping({ "/classroom", "/classroom/home" })
    public String classroomPage(Model model) {

        List<MenuItem> menuItems = List.of(
                new MenuItem("ホーム", "/classroom/home")
        // new MenuItem("講義室", "/classroom/room")
        );

        Map<Integer, ClassroomDTO> classroomMap = rs.getAllClassrooms();
        List<PostDTO> latestAnnouncements = ps.getLatestAnnouncements(3);
        List<InstagramPostDTO> instagramPosts = is.getRecentPosts();

        model.addAttribute("menuItems", menuItems);
        model.addAttribute("classroomMap", classroomMap);
        model.addAttribute("latestAnnouncements", latestAnnouncements);
        model.addAttribute("instagramPosts", instagramPosts);

        return "classroom/home";
    }
}
