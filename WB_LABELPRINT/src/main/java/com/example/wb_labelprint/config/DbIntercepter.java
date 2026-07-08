package com.example.wb_labelprint.config;

import com.example.wb_labelprint.config.datasource.DbContextHolder;
import com.example.wb_labelprint.config.datasource.DbType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DbIntercepter implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && Boolean.TRUE.equals(session.getAttribute("isLogin"));

        if (!loggedIn) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html")) {
                response.sendRedirect("/login");
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return false;
        }

        Object country = session.getAttribute("country");
        if (country != null) {
            try {
                DbContextHolder.set(DbType.valueOf(country.toString()));
            } catch (IllegalArgumentException e) {
                DbContextHolder.set(DbType.USA);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion (HttpServletRequest request, HttpServletResponse response, Object Handler, Exception ex){
        DbContextHolder.clear();
    }
}
