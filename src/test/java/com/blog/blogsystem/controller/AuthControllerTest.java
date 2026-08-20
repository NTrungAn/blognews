package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.entity.RefreshToken;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import com.blog.blogsystem.repository.RefreshTokenRepository;
import com.blog.blogsystem.repository.RoleRepository;
import com.blog.blogsystem.repository.UserRepository;
import com.blog.blogsystem.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Đăng nhập thành công với tài khoản hợp lệ và nhận đủ token + thông tin user.
    @Test
    public void testLogin_Success() throws Exception {
        String suffix = uniqueSuffix();
        String username = "login_user_" + suffix;
        String password = "Password@123";
        createUser(username, "login_" + suffix + "@example.com", password, RoleType.USER);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    // Sau khi login, refresh token phải được lưu vào database.
    @Test
    public void testLogin_SavesRefreshTokenToDatabase() throws Exception {
        String suffix = uniqueSuffix();
        String username = "login_db_" + suffix;
        String password = "Password@123";
        User user = createUser(username, "login_db_" + suffix + "@example.com", password, RoleType.USER);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Kiểm tra refresh token được lưu trong DB
        var tokens = refreshTokenRepository.findAllByUserAndRevokedFalseOrderByCreatedAtDesc(user);
        assertFalse(tokens.isEmpty(), "Refresh token phải được lưu vào database sau khi login");
        assertEquals(1, tokens.size());
        assertFalse(tokens.get(0).getRevoked());
    }

    // Đăng nhập thất bại khi username/password không đúng.
    @Test
    public void testLogin_Failure_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin_fake");
        loginRequest.setPassword("wrong_password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message", containsString("Sai thông tin đăng nhập")));
    }

    // Validate request login khi bỏ trống cả username và password.
    @Test
    public void testLogin_ValidationFail_BlankFields() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("");
        loginRequest.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.username").value("Username không được để trống"))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu không được để trống"));
    }

    // Đăng ký tài khoản mới thành công với dữ liệu hợp lệ.
    @Test
    public void testRegister_Success() throws Exception {
        String suffix = uniqueSuffix();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("register_user_" + suffix);
        registerRequest.setEmail("register_" + suffix + "@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setFullName("Register User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Đăng ký tài khoản thành công!"));
    }

    // Đăng ký thất bại khi username đã tồn tại trong hệ thống.
    @Test
    public void testRegister_Failure_DuplicateUsername() throws Exception {
        String suffix = uniqueSuffix();
        createUser("existing_" + suffix, "existing_" + suffix + "@example.com", "Password@123", RoleType.READER);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("existing_" + suffix);
        registerRequest.setEmail("new_" + suffix + "@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setFullName("Duplicate Username");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Username")));
    }

    // Refresh token thành công: user thực phải tồn tại trong DB và token phải được lưu.
    @Test
    public void testRefreshToken_Success() throws Exception {
        String suffix = uniqueSuffix();
        String username = "refresh_user_" + suffix;
        User user = createUser(username, "refresh_" + suffix + "@example.com", "Password@123", RoleType.READER);

        // Tạo refresh token và lưu vào DB (giả lập đã login)
        String refreshToken = jwtUtils.generateRefreshToken(username);
        saveRefreshTokenToDB(user, refreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    // Refresh token thất bại khi token không hợp lệ hoặc sai định dạng.
    @Test
    public void testRefreshToken_Failure_InvalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message", containsString("không hợp lệ")));
    }

    // Gửi access token vào endpoint refresh phải bị từ chối.
    @Test
    public void testRefreshToken_Failure_AccessTokenUsedAsRefresh() throws Exception {
        String suffix = uniqueSuffix();
        String username = "access_as_refresh_" + suffix;
        createUser(username, "aar_" + suffix + "@example.com", "Password@123", RoleType.READER);

        // Tạo ACCESS token (không phải refresh token)
        String accessToken = jwtUtils.generateAccessToken(username);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(accessToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // Token cũ phải bị revoke sau khi rotate (Token Rotation).
    @Test
    public void testRefreshToken_RotatesToken() throws Exception {
        String suffix = uniqueSuffix();
        String username = "rotate_" + suffix;
        User user = createUser(username, "rotate_" + suffix + "@example.com", "Password@123", RoleType.READER);

        // Tạo refresh token ban đầu và lưu vào DB
        String originalRefreshToken = jwtUtils.generateRefreshToken(username);
        RefreshToken savedToken = saveRefreshTokenToDB(user, originalRefreshToken);
        UUID savedTokenId = savedToken.getId();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(originalRefreshToken);

        // Gọi refresh
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Flush và clear persistence context để reload từ DB
        // Token cũ phải bị revoke, không dùng được nữa
        String oldHash = sha256(originalRefreshToken);
        var revokedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(oldHash);
        assertTrue(revokedToken.isEmpty(), "Token cũ phải bị revoke sau khi rotate");

        // Kiểm tra token cũ được đánh dấu revoked trong DB
        var tokenInDb = refreshTokenRepository.findById(savedTokenId);
        assertTrue(tokenInDb.isPresent());
        assertTrue(tokenInDb.get().getRevoked(), "Token cũ phải có revoked=true trong DB");
    }

    // Dùng token đã bị revoke phải bị từ chối.
    @Test
    public void testRefreshToken_Failure_RevokedToken() throws Exception {
        String suffix = uniqueSuffix();
        String username = "revoked_" + suffix;
        User user = createUser(username, "revoked_" + suffix + "@example.com", "Password@123", RoleType.READER);

        // Tạo refresh token, lưu vào DB, rồi revoke nó
        String refreshToken = jwtUtils.generateRefreshToken(username);
        RefreshToken stored = saveRefreshTokenToDB(user, refreshToken);
        stored.revoke();
        refreshTokenRepository.save(stored);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message", containsString("thu hồi")));
    }

    // Sau logout, tất cả refresh token phải bị revoke.
    @Test
    public void testLogout_RevokesAllTokens() throws Exception {
        String suffix = uniqueSuffix();
        String username = "logout_" + suffix;
        String password = "Password@123";
        User user = createUser(username, "logout_" + suffix + "@example.com", password, RoleType.USER);

        // Login để tạo token trong DB
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Lấy access token để gọi logout
        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("data").get("accessToken").asText();

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Tất cả refresh token phải bị revoke
        var activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalseOrderByCreatedAtDesc(user);
        assertTrue(activeTokens.isEmpty(), "Tất cả refresh token phải bị revoke sau logout");
    }

    // Logout luôn trả về thành công và xóa context xác thực phía server.
    @Test
    public void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Đăng xuất thành công"));
    }

    // ─────────────── Helper Methods ───────────────

    private User createUser(String username, String email, String rawPassword, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Test User")
                .roles(Set.of(role))
                .build();

        return userRepository.save(user);
    }

    private RefreshToken saveRefreshTokenToDB(User user, String rawToken) {
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .deviceInfo("Test-Agent")
                .familyId(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(rt);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
