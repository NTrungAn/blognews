package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.response.AuthResponse;
import com.blog.blogsystem.dto.response.RefreshTokenResponse;

public interface AuthService {
    // Nghiệp vụ đăng nhập, trả về token và thông tin user
    AuthResponse login(LoginRequest loginRequest, String deviceInfo);

    // Nghiệp vụ đăng ký, ném ra exception nếu lỗi hoặc trả về thông báo thành công
    String register(RegisterRequest registerRequest);

    // Nghiệp vụ làm mới token (có Token Rotation + Reuse Detection)
    RefreshTokenResponse refresh(RefreshTokenRequest request, String deviceInfo);

    // Nghiệp vụ đăng xuất — revoke tất cả refresh token của user hiện tại
    void logout();
}
