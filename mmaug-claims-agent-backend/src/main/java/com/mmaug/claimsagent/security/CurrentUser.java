package com.mmaug.claimsagent.security;

import java.util.Set;

public record CurrentUser(
        String userId,
        String entraOid,
        String email,
        Set<Role> roles
) {
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}