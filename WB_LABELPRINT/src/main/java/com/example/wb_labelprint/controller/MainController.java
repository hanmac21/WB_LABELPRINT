package com.example.wb_labelprint.controller;

import com.example.wb_labelprint.config.datasource.DbContextHolder;
import com.example.wb_labelprint.config.datasource.DbType;
import com.example.wb_labelprint.mapper.LoginMapper;
import com.example.wb_labelprint.mapper.kor.LoginKorMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private LoginKorMapper loginMapper;

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
        boolean auth = false;

        if(("woobo".equals(username) && "labelwoobo".equals(password)) || ("master".equals(username) && "woo#*".equals(password))) {
            auth = true;
            session.removeAttribute("custcode");        // 관리자는 필터 없이 전체 조회
        } else if ("PT".equals(country)) {
            try {
                DbContextHolder.set(DbType.valueOf(country));   // PT → korDataSource
                Map<String, Object> user = loginMapper.loginCheck(username);
                auth = (user != null) && password.equals(user.get("PW"));
                if (auth){
                    session.setAttribute("custcode", user.get("CUSTCODE"));
                }
            } finally {
                DbContextHolder.clear();   // 반드시 정리 (스레드 재사용 대비)
            }
        }

        if (!auth){
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
