package net.dsa.scitHub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.ClassroomDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.entity.classroom.Classroom;
import net.dsa.scitHub.service.ReservationService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ClassroomController {

    private final ReservationService rs;

    // 클래스룸 페이지 요청
    @GetMapping({ "/classroom", "/classroom/home" })
    public String classroomPage(Model model) {

        List<MenuItem> menuItems = List.of(
                new MenuItem("ホーム", "/classroom/home")
        // new MenuItem("講義室", "/classroom/room")
        );

        Map<Integer, ClassroomDTO> classroomMap = rs.getAllClassrooms();

        model.addAttribute("menuItems", menuItems);
        model.addAttribute("classroomMap", classroomMap);

        return "classroom/home";
    }
}
