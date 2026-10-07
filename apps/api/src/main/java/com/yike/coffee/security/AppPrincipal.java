package com.yike.coffee.security;

import java.security.Principal;

public record AppPrincipal(String userId, String email, String displayName, String role, String merchantId) implements Principal {
    @Override public String getName() { return userId; }
}
