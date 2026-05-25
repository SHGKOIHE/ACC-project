package com.foodgroup.common.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record MemberPrincipal(Long memberId, String nickname) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return String.valueOf(memberId); }
}
