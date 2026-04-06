package com.chatroom.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {

        // Look for Authorization header in every request
        String authHeader = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            // Strip "Bearer " prefix to get the actual token
            String token = authHeader.substring(7);

            // Validate the token
            if (jwtUtil.isValid(token)) {

                // Extract userId from inside the token
                Long userId = jwtUtil.extractUserId(token);

                // Tell Spring Security this request is authenticated
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userId,   // principal — who is this user
                        null,     // credentials — not needed
                        List.of() // authorities — roles (none for now)
                    );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Always continue to next filter
        filterChain.doFilter(request, response);
    }
}
