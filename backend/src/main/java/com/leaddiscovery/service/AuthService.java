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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public UserSummaryDto register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email '" + normalizedEmail + "' already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setName(request.getName().trim());
        user.setUsername(normalizedEmail); // Keep username synchronized with email
        user.setEmail(normalizedEmail);
        user.setPasswordHash(encodedPassword);
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        return UserSummaryDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(authentication);

        UserSummaryDto userSummary = new UserSummaryDto(
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getRole(),
                null
        );

        return new AuthResponse(jwt, tokenProvider.getJwtExpirationMs() / 1000, userSummary);
    }
}
