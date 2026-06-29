package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.response.AuthResponse;
import com.blog.blogsystem.dto.response.RefreshTokenResponse;
import com.blog.blogsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        try {
            String message = authService.register(signUpRequest);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            // Tạm thời bắt lỗi tại đây. Sau này có thể dùng @ControllerAdvice để xử lý lỗi toàn cục
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            RefreshTokenResponse response = authService.refresh(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        authService.logout();
        return ResponseEntity.ok("Đăng xuất thành công");
    }
}
