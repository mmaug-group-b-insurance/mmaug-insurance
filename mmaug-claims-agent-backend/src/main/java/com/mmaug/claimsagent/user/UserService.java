package com.mmaug.claimsagent.user;

import com.mmaug.claimsagent.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public AppUser getOrCreateUser(String entraObjectId, String email, String displayName) {
        if (entraObjectId == null || entraObjectId.isBlank()) {
            return null;
        }

        Optional<AppUser> existing = userRepository.findByEntraObjectId(entraObjectId);
        if (existing.isPresent()) {
            AppUser user = existing.get();
            boolean changed = false;
            if (email != null && !email.isBlank() && user.getEmail() == null) {
                user.setEmail(email);
                changed = true;
            }
            if (displayName != null && !displayName.isBlank() && user.getDisplayName() == null) {
                user.setDisplayName(displayName);
                changed = true;
            }
            user.setLastLoginAt(Instant.now());
            changed = true;
            return changed ? userRepository.save(user) : user;
        }

        // Auto-provision authenticated user in DB
        Role role = Role.CLAIMS_OFFICER;
        if (email != null && (email.toLowerCase().contains("admin") || email.equalsIgnoreCase("fumeh34@gmail.com"))) {
            role = Role.ADMIN;
        }

        AppUser newUser = AppUser.builder()
                .entraObjectId(entraObjectId)
                .email(email)
                .displayName(displayName != null && !displayName.isBlank() ? displayName : email)
                .role(role)
                .active(true)
                .lastLoginAt(Instant.now())
                .build();

        log.info("Auto-provisioned user in database: oid={}, email={}, role={}", entraObjectId, email, role);
        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Set<Role> getRolesForUser(String entraObjectId, String email, String displayName) {
        AppUser user = getOrCreateUser(entraObjectId, email, displayName);
        if (user == null || !user.isActive() || user.getRole() == null) {
            return Set.of();
        }
        if (user.getRole() == Role.ADMIN) {
            return Set.of(Role.ADMIN, Role.CLAIMS_OFFICER, Role.AUDITOR);
        }
        return Set.of(user.getRole());
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByEntraObjectId(String entraObjectId) {
        return userRepository.findByEntraObjectId(entraObjectId);
    }

    @Transactional(readOnly = true)
    public List<AppUser> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public AppUser updateRole(UUID userId, Role newRole) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setRole(newRole);
        return userRepository.save(user);
    }
}
