package com.example.wb_labelprint.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Void> login(@RequestParam String username, @RequestParam String password, @RequestParam String country, HttpSession session) {
        if (!"woobo".equals(username) || !"a1234".equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        session.setAttribute("country", country);
        session.setAttribute("isLogin", true);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/main")
    public String main(HttpSession session) {
        Boolean isLogin = (Boolean) session.getAttribute("isLogin");

        if (isLogin == null || !isLogin) {
            return "redirect:/login";   // 로그인 안 했으면 로그인으로
        }
        return "forward:/main.html";
    }

    @GetMapping("/session/country")
    @ResponseBody
    public String currentCountry(HttpSession session) {
        Object country = session.getAttribute("country");
        return country != null ? country.toString() : "USA";
    }

    @PostMapping("/logout")
    @ResponseBody
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
