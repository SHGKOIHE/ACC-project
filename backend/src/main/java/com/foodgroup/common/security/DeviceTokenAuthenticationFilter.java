package com.foodgroup.common.security;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.auth.service.DeviceTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class DeviceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";

    private final DeviceTokenService deviceTokenService;
    private final MemberPort memberPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(DEVICE_TOKEN_HEADER);

        if (StringUtils.hasText(token)) {
            deviceTokenService.resolveMemberId(token).flatMap(memberPort::findById).ifPresent(member -> {
                MemberPrincipal principal = new MemberPrincipal(member.getId(), member.getNickname());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }
}
