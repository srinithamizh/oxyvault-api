package io.github.srinithamizh.oxyvault.jwt;

import io.github.srinithamizh.oxyvault.security.UserPrincipal;
import io.github.srinithamizh.oxyvault.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null
                && authorizationHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            authenticate(authorizationHeader.substring(BEARER_PREFIX.length()).trim(), request);
        }

        // An invalid token simply leaves the request unauthenticated. Protected endpoints then
        // answer 401 via the entry point, while public ones (login, register) keep working even
        // when the client still sends a stale token.
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.extractClaims(token);

            String username = claims.getSubject();
            if (username == null) {
                return;
            }

            // One database lookup per request: the same row supplies authorities and token version.
            UserPrincipal principal = userDetailsService.loadByUsername(username);

            if (!jwtService.isTokenVersionCurrent(claims, principal)) {
                log.debug("Rejected JWT for '{}': token version is no longer current", username);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
        }
    }
}
