package com.alm.controller;

import com.alm.service.SecurityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Controller
public class SecurityController {

    private final SecurityService securityService = new SecurityService();

    // 접속 시 비밀번호 유무에 따라 화면 이동
    @GetMapping("/")
    public String index() {
        // 비어있으면 설정 페이지, 있으면 로그인 페이지로 연결
        if (securityService.checkFirstLogin()) {
            return "redirect:/setup-password.html";
        } else {
            return "redirect:/login.html";
        }
    }

    // 로그인 검증 처리
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String inputPassword = request.get("password");

        // 일치 여부에 따라 결과 반환
        if (securityService.authenticate(inputPassword)) {
            return ResponseEntity.ok().body(Map.of("success", true));
        }
        return ResponseEntity.status(401).body(Map.of("success", false));
    }

    // 초기 비밀번호 저장 처리
    @PostMapping("/api/setup")
    @ResponseBody
    public ResponseEntity<?> setupPassword(@RequestBody Map<String, String> request) {
        String inputPassword = request.get("password");


        if (securityService.setupInitialPassword(inputPassword)) {
            return ResponseEntity.ok().body(Map.of("success", true));
        }
        return ResponseEntity.status(500).body(Map.of("success", false));
    }
}