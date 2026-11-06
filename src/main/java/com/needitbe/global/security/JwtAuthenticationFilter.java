package com.needitbe.global.security;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // Authorization 헤더가 없으면 다음 필터로 이동
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            Claims claims = jwtTokenProvider.parseClaims(token);

            if (!"access".equals(claims.get("type", String.class))) {
                throw new IllegalArgumentException(
                        "Access Token이 아닙니다."
                );
            }

            Long memberId = Long.valueOf(claims.getSubject());
            Member member = memberRepository.findById(memberId)
                    .orElse(null);

            if (member != null) {

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + member.getRole().name()
                        )
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                member.getId().toString(),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {
            // 잘못되거나 만료된 토큰은 인증하지 않음
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}