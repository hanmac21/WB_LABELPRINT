package com.example.wb_usa_labelprint.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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

    @GetMapping("/main")
    public String main(){
        return "main";
    }

    @PostMapping("/main")
    public String mainPost() {
        return "redirect:/main";
    }


}
