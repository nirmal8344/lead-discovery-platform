package com.leaddiscovery.controller;

import com.leaddiscovery.dto.AuthResponse;
import com.leaddiscovery.dto.LoginRequest;
import com.leaddiscovery.dto.RegisterRequest;
import com.leaddiscovery.dto.UserSummaryDto;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import com.leaddiscovery.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummaryDto> register(@Valid @RequestBody RegisterRequest request) {
        UserSummaryDto createdUser = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getCurrentUser() {
        UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
        return ResponseEntity.ok(new UserSummaryDto(
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getRole(),
                null
        ));
    }
}
