package com.wd.api.security;

import com.wd.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        
        // Validate token first
        if (!jwtService.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token type and subject
        String tokenType = jwtService.extractTokenType(jwt);
        String actualSubject = jwtService.extractActualSubject(jwt);
        
        // Handle different token types
        if ("PARTNER".equals(tokenType)) {
            // Partnership user authentication
            handlePartnerAuthentication(jwt, actualSubject, request);
        } else {
            // Portal user authentication (default)
            handlePortalAuthentication(jwt, actualSubject, request);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void handlePartnerAuthentication(String jwt, String phone, HttpServletRequest request) {
        // Create simple authentication for partnership users
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_PARTNER")
        );
        
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            phone, // Phone number as principal
            null,
            authorities
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    
    private void handlePortalAuthentication(String jwt, String email, HttpServletRequest request) {
        try {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
            
            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // If user not found in portal users, skip authentication
            // This allows the request to proceed to the controller where it will be rejected
        }
    }
} 