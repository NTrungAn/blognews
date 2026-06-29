package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.response.AuthResponse;
import com.blog.blogsystem.dto.response.RefreshTokenResponse;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.security.JwtUtils;
import com.blog.blogsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // Thực hiện xác thực
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Tạo JWT Token và Refresh Token
        String accessToken = jwtUtils.generateAccessToken(authentication.getName());
        String refreshToken = jwtUtils.generateRefreshToken(authentication.getName());

        // Lấy thông tin user
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy danh sách quyền hạn thực tế từ Database
        String role = user.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .findFirst()
                .orElse(RoleType.READER.name());

        AuthResponse.UserDto userDto = new AuthResponse.UserDto(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                role,
                null // avatarUrl chưa có trong entity User hiện tại
        );

        return new AuthResponse(accessToken, refreshToken, "Bearer", 15 * 60, userDto);
    }

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        // 1. Kiểm tra tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Lỗi: Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Lỗi: Email đã được sử dụng!");
        }

        // 2. Map DTO sang Entity và băm mật khẩu
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // 3. Gán quyền mặc định (READER)
        Role userRole = roleRepository.findByRoleName(RoleType.READER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền READER trong hệ thống."));
        user.setRoles(Collections.singleton(userRole));

        // 4. Lưu xuống Database
        userRepository.save(user);

        return "Đăng ký tài khoản thành công!";
    }

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        if (requestRefreshToken != null && jwtUtils.validateToken(requestRefreshToken)) {
            String username = jwtUtils.getUsernameFromJwt(requestRefreshToken);
            // Trong thực tế, nên lưu refresh token vào database để kiểm tra nó có bị revoke hay không
            // Ở đây tạm thời cấp luôn accessToken mới nếu refreshToken còn hạn
            String newAccessToken = jwtUtils.generateAccessToken(username);
            String newRefreshToken = jwtUtils.generateRefreshToken(username);
            return new RefreshTokenResponse(newAccessToken, newRefreshToken);
        }
        throw new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn!");
    }

    @Override
    public void logout() {
        // Với stateless JWT, việc logout chủ yếu ở phía client xóa token.
        // Phía server, nếu có lưu refresh token vào DB thì sẽ xóa nó ở đây.
        SecurityContextHolder.clearContext();
    }
}
