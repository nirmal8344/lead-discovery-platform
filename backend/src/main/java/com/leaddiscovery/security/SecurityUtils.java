package com.leaddiscovery.security;

import com.leaddiscovery.entity.User;
import com.leaddiscovery.entity.enums.Role;
import com.leaddiscovery.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return Optional.empty();
        }
        return Optional.of((UserPrincipal) authentication.getPrincipal());
    }

    public UserPrincipal getRequiredCurrentUserPrincipal() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("User must be authenticated to perform this operation"));
    }

    public User getRequiredCurrentUser() {
        UserPrincipal principal = getRequiredCurrentUserPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in database"));
    }

    public boolean isAdmin() {
        return getCurrentUserPrincipal()
                .map(p -> p.getRole() == Role.ADMIN)
                .orElse(false);
    }
}
