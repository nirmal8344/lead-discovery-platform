package com.leaddiscovery.service;

import com.leaddiscovery.dto.AuthResponse;
import com.leaddiscovery.dto.LoginRequest;
import com.leaddiscovery.dto.RegisterRequest;
import com.leaddiscovery.dto.UserSummaryDto;
import com.leaddiscovery.entity.User;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.exception.UserAlreadyExistsException;
import com.leaddiscovery.repository.UserRepository;
import com.leaddiscovery.security.JwtTokenProvider;
import com.leaddiscovery.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, tokenProvider);
    }

    @Test
    @DisplayName("Should successfully register new user with hashed password and USER role")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("Alice Smith", "alice@example.com", "Password123!");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserSummaryDto response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Alice Smith", response.getName());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("Password123!");
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when registering with an existing email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Alice Smith", "alice@example.com", "Password123!");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully and return signed JWT and user info")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("alice@example.com", "Password123!");

        UserPrincipal principal = new UserPrincipal(1L, "Alice Smith", "alice@example.com", "hashed", Role.USER, Collections.emptyList());
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("mock.jwt.token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertEquals("Alice Smith", response.getUser().getName());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertEquals(Role.USER, response.getUser().getRole());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login authentication fails")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(tokenProvider, never()).generateToken(any());
    }
}
