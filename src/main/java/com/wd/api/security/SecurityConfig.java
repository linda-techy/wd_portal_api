package com.wd.api.security;

import com.wd.api.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private CustomUserDetailsService customUserDetailsService;

        @Autowired
        private CustomAuthenticationEntryPoint authenticationEntryPoint;

        @Autowired
        private CustomAccessDeniedHandler accessDeniedHandler;

        @Value("${app.cors.allowed-origins:}")
        private String configuredAllowedOrigins;

        @Value("${spring.profiles.active:local}")
        private String activeProfile;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/tools/**").permitAll()

                                                // Public lead submission endpoints (contact form, client referral,
                                                // calculators)
                                                .requestMatchers("/leads/contact").permitAll()
                                                .requestMatchers("/leads/referral").permitAll()
                                                .requestMatchers("/leads/calculator/**").permitAll() // All calculator
                                                                                                     // types

                                                // Customer password reset (unauthenticated — accessed from email link)
                                                .requestMatchers("/customers/reset-password-page").permitAll()
                                                .requestMatchers("/customers/reset-password-confirm").permitAll()

                                                // Partnership endpoints (public)
                                                .requestMatchers("/api/partnerships/login").permitAll()
                                                .requestMatchers("/api/partnerships/apply").permitAll()
                                                .requestMatchers("/api/partnerships/logout").permitAll()

                                                // Partnership endpoints (protected - requires ROLE_PARTNER)
                                                .requestMatchers("/api/partnerships/**").hasRole("PARTNER")

                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                String envAllowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
                String effectiveOrigins = !isBlank(envAllowedOrigins) ? envAllowedOrigins : configuredAllowedOrigins;
                List<String> parsedOrigins = parseOrigins(effectiveOrigins);
                List<String> localhostOrigins = Arrays.asList(
                                "http://localhost:3001",
                                "http://127.0.0.1:3001",
                                "http://localhost:3000",
                                "http://127.0.0.1:3000");
                boolean isLocalProfile = isLocalLikeProfile(activeProfile);

                if (!parsedOrigins.isEmpty()) {
                        List<String> allowedOrigins = new ArrayList<>(parsedOrigins);
                        for (String localhostOrigin : localhostOrigins) {
                                if (!allowedOrigins.contains(localhostOrigin)) {
                                        allowedOrigins.add(localhostOrigin);
                                }
                        }
                        configuration.setAllowedOrigins(allowedOrigins);
                        logger.info("CORS: Using configured origins (+localhost dev origins) for profile '{}': {}",
                                        activeProfile, allowedOrigins);
                } else if (isLocalProfile) {
                        // Local/dev fallback only when no explicit origins are configured.
                        configuration.setAllowedOrigins(localhostOrigins);
                        logger.warn("CORS: No explicit origins configured; using localhost patterns for local/dev.");
                } else {
                        // Production-safe fallback if misconfigured.
                        List<String> fallbackOrigins = new ArrayList<>(Arrays.asList(
                                        "https://portal.walldotbuilders.com",
                                        "https://walldotbuilders.com",
                                        "https://www.walldotbuilders.com"));
                        fallbackOrigins.addAll(localhostOrigins);
                        configuration.setAllowedOrigins(fallbackOrigins);
                        logger.warn("CORS: No explicit origins configured; using production fallback origins: {}",
                                        fallbackOrigins);
                }

                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));
                configuration.setExposedHeaders(Arrays.asList(
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        private List<String> parseOrigins(String rawOrigins) {
                if (isBlank(rawOrigins)) {
                        return List.of();
                }
                return Arrays.stream(rawOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isEmpty())
                                .collect(Collectors.toList());
        }

        private boolean isLocalLikeProfile(String profile) {
                if (isBlank(profile)) {
                        return true;
                }
                String normalized = profile.trim().toLowerCase();
                return "local".equals(normalized) || "dev".equals(normalized) || "default".equals(normalized);
        }

        private boolean isBlank(String value) {
                return value == null || value.trim().isEmpty();
        }

        @Bean
        @SuppressWarnings("deprecation")
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(customUserDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}