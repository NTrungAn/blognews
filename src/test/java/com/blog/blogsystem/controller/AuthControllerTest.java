package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.LoginRequest;
import com.blog.blogsystem.dto.request.RefreshTokenRequest;
import com.blog.blogsystem.dto.request.RegisterRequest;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data", containsString("Username đã tồn tại")));
    }

    // Refresh token thành công khi gửi refresh token hợp lệ.
    @Test
    public void testRefreshToken_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(jwtUtils.generateRefreshToken("refresh_user_" + uniqueSuffix()));

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Refresh token không hợp lệ hoặc đã hết hạn!"));
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

    private void createUser(String username, String email, String rawPassword, RoleType roleType) {
        Role role = roleRepository.findByRoleName(roleType)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleType));

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Test User")
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
