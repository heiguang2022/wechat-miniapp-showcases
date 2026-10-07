package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.security.CurrentUser;
import com.yike.coffee.service.AuthService;
import com.yike.coffee.service.WechatLoginProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    private final WechatLoginProvider wechat;
    public AuthController(AuthService service, WechatLoginProvider wechat) { this.service = service; this.wechat = wechat; }

    public record DevLoginRequest(@NotBlank @Email String email) {}
    public record TokenRequest(@NotBlank String refreshToken) {}
    public record WechatRequest(@NotBlank String code) {}

    @PostMapping("/dev-login")
    ApiResponse<Map<String,Object>> dev(@Valid @RequestBody DevLoginRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.devLogin(body.email()), request);
    }
    @PostMapping("/wechat-login")
    ApiResponse<Map<String,Object>> wechat(@Valid @RequestBody WechatRequest body, HttpServletRequest request) {
        return ApiResponse.ok(wechat.login(body.code()), request);
    }
    @PostMapping("/refresh")
    ApiResponse<Map<String,Object>> refresh(@Valid @RequestBody TokenRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.refresh(body.refreshToken()), request);
    }
    @PostMapping("/logout")
    ApiResponse<Map<String,Boolean>> logout(@RequestBody(required=false) TokenRequest body, HttpServletRequest request) {
        service.logout(body == null ? null : body.refreshToken()); return ApiResponse.ok(Map.of("loggedOut", true), request);
    }
    @GetMapping("/me")
    ApiResponse<Object> me(HttpServletRequest request) { return ApiResponse.ok(CurrentUser.required(), request); }
}
