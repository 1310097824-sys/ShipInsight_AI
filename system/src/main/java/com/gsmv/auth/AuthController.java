package com.gsmv.auth;

import com.gsmv.auth.dto.LoginRequest;
import com.gsmv.auth.dto.LoginResponse;
import com.gsmv.auth.dto.CaptchaResponse;
import com.gsmv.auth.dto.RegisterRequest;
import com.gsmv.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.generate());
    }
}
