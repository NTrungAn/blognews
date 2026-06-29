package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.dto.response.AuthResponse;

public interface AuthService {
    // Nghiệp vụ đăng nhập, trả về token và thông tin user
    AuthResponse login(LoginRequest loginRequest);

    // Nghiệp vụ đăng ký, ném ra exception nếu lỗi hoặc trả về thông báo thành công
    String register(RegisterRequest registerRequest);

    // Nghiệp vụ làm mới token
    com.blog.blogsystem.dto.response.RefreshTokenResponse refresh(com.blog.blogsystem.dto.request.RefreshTokenRequest request);

    // Nghiệp vụ đăng xuất (hiện tại stateless nên chỉ trả về message)
    void logout();
}
