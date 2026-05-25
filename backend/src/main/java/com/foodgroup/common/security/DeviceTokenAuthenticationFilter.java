package com.foodgroup.common.security;

import com.foodgroup.auth.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class DeviceTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";
    private static final String CACHE_PREFIX = "auth:device:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(DEVICE_TOKEN_HEADER);

        if (StringUtils.hasText(token)) {
            Long memberId = getMemberIdFromCache(token);
            if (memberId != null) {
                memberRepository.findById(memberId).ifPresent(member -> {
                    MemberPrincipal principal = new MemberPrincipal(member.getId(), member.getNickname());
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long getMemberIdFromCache(String token) {
        String cacheKey = CACHE_PREFIX + token;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return Long.parseLong(cached);
        }

        return memberRepository.findByDeviceToken(token).map(member -> {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(member.getId()), CACHE_TTL);
            return member.getId();
        }).orElse(null);
    }
}
