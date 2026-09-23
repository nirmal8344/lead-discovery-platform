package com.leaddiscovery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.AuthResponse;
import com.leaddiscovery.dto.LoginRequest;
import com.leaddiscovery.dto.RegisterRequest;
import com.leaddiscovery.dto.UserSummaryDto;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.exception.GlobalExceptionHandler;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import com.leaddiscovery.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Mock
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AuthController controller = new AuthController(authService, securityUtils);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register should register new user")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "Password123!");
        UserSummaryDto responseDto = new UserSummaryDto(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(authService.register(any(RegisterRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 for invalid email/password")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/auth/login should authenticate and return JWT token")
    void testLoginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        UserSummaryDto userSummary = new UserSummaryDto(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());
        AuthResponse response = new AuthResponse("mock.jwt.token", 86400L, userSummary);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/auth/me should return authenticated user profile")
    void testGetMeEndpoint() throws Exception {
        UserPrincipal principal = new UserPrincipal(1L, "Alice", "alice@example.com", "pwd", Role.USER, Collections.emptyList());
        when(securityUtils.getRequiredCurrentUserPrincipal()).thenReturn(principal);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
