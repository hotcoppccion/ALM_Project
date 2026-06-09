package com.alm.controller;

import com.alm.service.SecurityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * 보안(인증) 도메인 컨트롤러.
 * index() 가 HTML 리다이렉트를 반환하므로 @RestController 대신 @Controller 사용.
 * 최초 접속(password_hash 없음) → /setup-password.html, 이후 → /login.html.
 */
@Controller
public class SecurityController {

    private final SecurityService securityService = new SecurityService();

    /** GET / — 최초 접속 여부에 따라 리다이렉트. */
    @GetMapping("/")
    public String index() {
        if (securityService.checkFirstLogin()) {
            return "redirect:/setup-password.html";
        } else {
            return "redirect:/login.html";
        }
    }

    /** POST /api/login — 인증. 성공 200, 실패 401. */
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        if (securityService.authenticate(request.get("password")))
            return ResponseEntity.ok().body(Map.of("success", true));
        return ResponseEntity.status(401).body(Map.of("success", false));
    }

    /** POST /api/setup — 최초 비밀번호 설정. 성공 200, DB 저장 실패 500. */
    @PostMapping("/api/setup")
    @ResponseBody
    public ResponseEntity<?> setupPassword(@RequestBody Map<String, String> request) {
        if (securityService.setupInitialPassword(request.get("password")))
            return ResponseEntity.ok().body(Map.of("success", true));
        return ResponseEntity.status(500).body(Map.of("success", false));
    }

    /** GET /api/auth/status — 비밀번호 설정 여부 조회. 각 페이지 로드 시 인증 상태 확인. */
    @GetMapping("/api/auth/status")
    @ResponseBody
    public ResponseEntity<?> authStatus() {
        return ResponseEntity.ok(Map.of("hasPassword", !securityService.checkFirstLogin()));
    }
}
