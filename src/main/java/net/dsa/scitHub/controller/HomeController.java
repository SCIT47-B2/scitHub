package net.dsa.scitHub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {

    // templates/classroom/home.html로 이동하게 함
    @GetMapping({"", "/"})
    public String home() {
        return "redirect:classroom/home";
    }

    @GetMapping("/skeleton")
    public String skeleton() {
        return "skeleton";
    }

    @GetMapping({"/homeTest"})
    public String landingPage() {
        return "user/landingPage";
    }
    
}
