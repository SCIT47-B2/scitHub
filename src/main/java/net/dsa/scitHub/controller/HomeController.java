package net.dsa.scitHub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {
    @GetMapping({"", "/"})
    public String home() {
        return "home";
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
