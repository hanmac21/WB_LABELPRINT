package com.example.wb_labelprint.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    @GetMapping("/")
    public String root(){
        return "redirect:/main";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @PostMapping("/main")
    public String loginToMain(@RequestParam(required = false) String country, HttpSession session) {

        // country 가 있으면 세션에 저장 (없으면 기본 USA)
        if (country != null && !country.isBlank()) {
            session.setAttribute("country", country);
        } else {
            session.setAttribute("country", "USA");
        }

        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main() {
        return "main";
    }
}
