package net.dsa.scitHub.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

// 전역 모델 주입
@ControllerAdvice
@Slf4j
public class GlobalModel {
    @ModelAttribute("activeUri")
    public String activeUri(HttpServletRequest req) {
        log.debug("Active URI: {}", req.getRequestURI());

        return req.getRequestURI();     // 예: /scitHub/classroom/home
    }
    @ModelAttribute("ctx")
    public String ctx(HttpServletRequest req) {
        log.debug("Context Path: {}", req.getContextPath());

        return req.getContextPath();    // 예: /scitHub 또는 ""
    }
}