package com.alm.controller;

import com.alm.service.SecurityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * [Controller 계층]
 * 보안(Security) 도메인의 HTTP 요청을 처리하는 컨트롤러.
 * 로그인, 비밀번호 초기 설정, 루트 경로 리다이렉션을 담당.
 *
 * [Java 문법 개념] @Controller vs @RestController:
 *   - @Controller: 뷰(View) 이름을 반환하거나 리다이렉션 처리 시 사용.
 *     반환값이 String이면 HTML 파일 이름 또는 "redirect:/경로"로 처리됨.
 *   - @RestController: 반환값을 JSON으로 직렬화하여 응답 body로 전송.
 *   - 이 클래스는 @Controller를 사용: index() 메서드에서 HTML 파일 리다이렉션이 필요하기 때문.
 *   - login(), setupPassword()는 JSON 응답이 필요하므로 @ResponseBody를 메서드에 개별 추가.
 *
 * [Java 문법 개념] @Controller의 리다이렉션:
 *   - "redirect:/경로": Spring MVC가 HTTP 302 Redirect 응답을 생성.
 *   - 브라우저가 302를 받으면 자동으로 해당 경로로 재요청(GET)을 보냄.
 */
@Controller
public class SecurityController {

    // SecurityService: 비밀번호 해시 비교, 최초 접속 여부 판단 등의 비즈니스 로직 담당
    private final SecurityService securityService = new SecurityService();

    /**
     * 루트 경로(/) 접속 시 상태에 따라 적절한 화면으로 리다이렉트.
     * GET /
     *
     * [처리 흐름]
     *   1. checkFirstLogin(): DB에 password_hash가 없으면 true (최초 접속)
     *   2. 최초 접속 → setup-password.html (비밀번호 설정 페이지)
     *   3. 재접속    → login.html (로그인 페이지)
     *
     * [Java 문법 개념] String 반환값 (Spring MVC View Resolution):
     *   - @Controller에서 String을 반환하면 Spring이 ViewResolver를 통해 뷰를 찾음.
     *   - "redirect:/" 접두사: ViewResolver를 거치지 않고 즉시 302 Redirect 응답 생성.
     *   - "/setup-password.html": static/ 폴더의 정적 HTML 파일 직접 제공.
     */
    @GetMapping("/")
    public String index() {
        if (securityService.checkFirstLogin()) {
            return "redirect:/setup-password.html"; // 최초 실행: 비밀번호 설정 화면으로
        } else {
            return "redirect:/login.html";           // 재접속: 로그인 화면으로
        }
    }

    /**
     * 로그인 요청 처리 및 인증 결과 반환.
     * POST /api/login
     * 요청 body 예: {"password": "myPassword123"}
     * 응답 예: {"success": true} 또는 {"success": false}
     *
     * [Java 문법 개념] @ResponseBody:
     *   - @Controller 클래스에서 특정 메서드만 JSON 응답으로 처리할 때 추가.
     *   - 이 메서드는 String(뷰 이름)이 아닌 ResponseEntity(JSON body)를 반환해야 하므로 필요.
     *
     * [Java 문법 개념] Map<String, String> 파라미터:
     *   - @RequestBody: HTTP 요청 body JSON → Java 객체 역직렬화.
     *   - 타입을 Map<String, String>으로 지정하면 {"password":"값"} → key="password", value="값".
     *
     * [HTTP 상태 코드]
     *   - 200 OK: 인증 성공
     *   - 401 Unauthorized: 인증 실패 (비밀번호 불일치)
     */
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String inputPassword = request.get("password"); // Map에서 "password" 키의 값 추출

        if (securityService.authenticate(inputPassword)) {
            // ResponseEntity.ok(): 200 OK + body 반환
            return ResponseEntity.ok().body(Map.of("success", true));
        }
        // ResponseEntity.status(int): 커스텀 상태 코드 지정. 401 = Unauthorized
        return ResponseEntity.status(401).body(Map.of("success", false));
    }

    /**
     * 최초 비밀번호 설정 요청 처리.
     * POST /api/setup
     * 요청 body 예: {"password": "newPassword"}
     *
     * [처리 흐름]
     *   1. 입력받은 비밀번호를 SecurityService로 전달
     *   2. SecurityService → SecurityUtil.encryptSHA256() → 해시값 생성
     *   3. SecurityRepository → user_security 테이블에 해시값 저장
     *   4. 성공/실패 여부를 JSON으로 반환
     *
     * [HTTP 상태 코드]
     *   - 200 OK: 저장 성공
     *   - 500 Internal Server Error: DB 저장 실패
     */
    @PostMapping("/api/setup")
    @ResponseBody
    public ResponseEntity<?> setupPassword(@RequestBody Map<String, String> request) {
        String inputPassword = request.get("password");

        if (securityService.setupInitialPassword(inputPassword)) {
            return ResponseEntity.ok().body(Map.of("success", true));
        }
        // ResponseEntity.status(500): 서버 내부 오류. 저장 실패를 의미.
        return ResponseEntity.status(500).body(Map.of("success", false));
    }
}
