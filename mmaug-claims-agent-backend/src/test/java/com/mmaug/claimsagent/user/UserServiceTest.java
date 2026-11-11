package com.mmaug.claimsagent.user;

import com.mmaug.claimsagent.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getOrCreateUser_whenNewStandardUser_assignsClaimsOfficer() {
        when(userRepository.findByEntraObjectId("new-oid")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = userService.getOrCreateUser("new-oid", "officer@example.com", "John Doe");

        assertThat(user).isNotNull();
        assertThat(user.getEntraObjectId()).isEqualTo("new-oid");
        assertThat(user.getRole()).isEqualTo(Role.CLAIMS_OFFICER);
        assertThat(user.getEmail()).isEqualTo("officer@example.com");
        assertThat(user.getDisplayName()).isEqualTo("John Doe");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void getOrCreateUser_whenAdminEmail_assignsAdminRole() {
        when(userRepository.findByEntraObjectId("admin-oid")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser user = userService.getOrCreateUser("admin-oid", "fumeh34@gmail.com", "Admin User");

        assertThat(user).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void getRolesForUser_whenAdmin_returnsAdminAndOfficerRoles() {
        AppUser adminUser = AppUser.builder()
                .entraObjectId("admin-oid")
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findByEntraObjectId("admin-oid")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Role> roles = userService.getRolesForUser("admin-oid", "fumeh34@gmail.com", "Admin");

        assertThat(roles).contains(Role.ADMIN, Role.CLAIMS_OFFICER, Role.AUDITOR);
    }

    @Test
    void getRolesForUser_whenClaimsOfficer_returnsOfficerRole() {
        AppUser officerUser = AppUser.builder()
                .entraObjectId("officer-oid")
                .role(Role.CLAIMS_OFFICER)
                .active(true)
                .build();
        when(userRepository.findByEntraObjectId("officer-oid")).thenReturn(Optional.of(officerUser));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Role> roles = userService.getRolesForUser("officer-oid", "officer@example.com", "Officer");

        assertThat(roles).containsExactly(Role.CLAIMS_OFFICER);
    }
}
