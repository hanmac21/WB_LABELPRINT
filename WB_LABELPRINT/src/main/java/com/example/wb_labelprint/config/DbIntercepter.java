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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object country = session.getAttribute("country");
            if (country != null) {
                try {
                    DbContextHolder.set(DbType.valueOf(country.toString()));
                } catch (IllegalArgumentException e) {
                    DbContextHolder.set(DbType.USA);                    // 잘못된 값이면 USA
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion (HttpServletRequest request, HttpServletResponse response, Object Handler, Exception ex){
        DbContextHolder.clear();
    }
}
