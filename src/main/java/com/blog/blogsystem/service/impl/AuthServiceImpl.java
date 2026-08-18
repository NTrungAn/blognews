package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.response.AuthResponse;
import com.blog.blogsystem.dto.response.RefreshTokenResponse;
import com.blog.blogsystem.entity.RefreshToken;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.exception.InvalidTokenException;
import com.blog.blogsystem.repository.RefreshTokenRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.security.JwtUtils;
import com.blog.blogsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.max-sessions:5}")
    private int maxSessions;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest, String deviceInfo) {
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

        // Lưu refresh token (hash) vào DB + quản lý số lượng session
        // Login mới → familyId = null → tự tạo UUID mới
        saveRefreshToken(user, refreshToken, deviceInfo, null);

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

        return new AuthResponse(accessToken, refreshToken, "Bearer",
                jwtUtils.getAccessTokenExpiration() / 1000, userDto);
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
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request, String deviceInfo) {
        String requestRefreshToken = request.getRefreshToken();

        // 1. Validate JWT signature + kiểm tra claim type == "refresh"
        if (!jwtUtils.validateRefreshToken(requestRefreshToken)) {
            throw new InvalidTokenException("Refresh token không hợp lệ hoặc đã hết hạn!");
        }

        // 2. Hash token và tìm trong DB (BẤT KỂ trạng thái revoke — để phát hiện reuse)
        String tokenHash = sha256(requestRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token không tồn tại trong hệ thống!"));

        // ═══════════════════════════════════════════════════════════
        // 3. TOKEN REUSE DETECTION (BCP 212)
        // Nếu token đã bị revoke mà vẫn được sử dụng → tấn công replay!
        // Revoke toàn bộ token family để bảo vệ user.
        // ═══════════════════════════════════════════════════════════
        if (storedToken.getRevoked()) {
            log.warn("⚠️ TOKEN REUSE DETECTED! familyId={}, user={}, tokenHash={}",
                    storedToken.getFamilyId(), storedToken.getUser().getUsername(), tokenHash);
            refreshTokenRepository.revokeAllByFamilyId(storedToken.getFamilyId());
            throw new InvalidTokenException(
                    "Phát hiện sử dụng lại token đã thu hồi! Tất cả phiên đăng nhập liên quan đã bị vô hiệu hóa. Vui lòng đăng nhập lại.");
        }

        // 4. Kiểm tra hết hạn (double-check với DB, dù JWT đã check)
        if (storedToken.isExpired()) {
            storedToken.revoke();
            refreshTokenRepository.save(storedToken);
            throw new InvalidTokenException("Refresh token đã hết hạn!");
        }

        // 5. REVOKE token cũ (Token Rotation)
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        // 6. Lấy username và tạo cặp token mới
        String username = jwtUtils.getUsernameFromRefreshToken(requestRefreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        // 7. Lưu token mới vào DB — KẾ THỪA familyId từ token cũ
        User user = storedToken.getUser();
        saveRefreshToken(user, newRefreshToken, deviceInfo, storedToken.getFamilyId());

        return new RefreshTokenResponse(newAccessToken, newRefreshToken, "Bearer",
                jwtUtils.getAccessTokenExpiration() / 1000);
    }

    @Override
    @Transactional
    public void logout() {
        // Lấy user hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                // Revoke ALL refresh tokens của user trong DB
                refreshTokenRepository.revokeAllByUser(user);
            });
        }
        // Clear SecurityContext
        SecurityContextHolder.clearContext();
    }

    // ─────────────── Private Helpers ───────────────

    /**
     * Lưu refresh token (hash) vào DB.
     * Nếu số lượng token active vượt quá maxSessions, xóa token cũ nhất.
     *
     * @param familyId nếu null → tạo familyId mới (login); nếu có giá trị → kế thừa (refresh)
     */
    private void saveRefreshToken(User user, String rawToken, String deviceInfo, UUID familyId) {
        // Kiểm tra và xóa token cũ nhất nếu vượt quá giới hạn
        long activeCount = refreshTokenRepository.countActiveByUser(user);
        if (activeCount >= maxSessions) {
            List<RefreshToken> activeTokens = refreshTokenRepository
                    .findAllByUserAndRevokedFalseOrderByCreatedAtDesc(user);
            // Revoke các token cũ nhất để chỉ giữ lại (maxSessions - 1) token
            for (int i = maxSessions - 1; i < activeTokens.size(); i++) {
                activeTokens.get(i).revoke();
                refreshTokenRepository.save(activeTokens.get(i));
            }
        }

        // Tính thời gian hết hạn từ cấu hình
        long expirationMs = jwtUtils.getRefreshTokenExpiration();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        // Nếu không có familyId (login mới) → tạo UUID mới
        UUID resolvedFamilyId = (familyId != null) ? familyId : UUID.randomUUID();

        // Lưu token mới
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .deviceInfo(deviceInfo)
                .familyId(resolvedFamilyId)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Hash chuỗi bằng SHA-256 và trả về hex string.
     * Lưu hash thay vì token gốc để bảo mật ngay cả khi DB bị truy cập trái phép.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
