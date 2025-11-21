package com.wd.api.security;

import com.wd.api.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/tools/**").permitAll()
                
                // Public lead submission endpoints (contact form, client referral, calculators)
                .requestMatchers("/leads/contact").permitAll()
                .requestMatchers("/leads/referral").permitAll()
                .requestMatchers("/leads/calculator/**").permitAll() // All calculator types
                
                // Partnership endpoints (public)
                .requestMatchers("/api/partnerships/login").permitAll()
                .requestMatchers("/api/partnerships/apply").permitAll()
                .requestMatchers("/api/partnerships/logout").permitAll()
                
                // Partnership endpoints (protected - requires ROLE_PARTNER)
                .requestMatchers("/api/partnerships/**").hasRole("PARTNER")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Get allowed origins from environment or use defaults
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        boolean isDevelopment = "dev".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE")) 
            || System.getProperty("spring.profiles.active", "").equalsIgnoreCase("dev");
        
        if (isDevelopment || allowedOriginsEnv == null) {
            // Development: Allow localhost and common dev ports
            logger.warn("CORS: Using permissive configuration for development. RESTRICT FOR PRODUCTION!");
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
            ));
        } else {
            // Production: Use environment variable or default secure origins
            String[] origins = allowedOriginsEnv.split(",");
            configuration.setAllowedOrigins(Arrays.asList(origins));
            logger.info("CORS: Using restricted origins from environment: {}", Arrays.toString(origins));
        }
        
        // Fallback to secure defaults if environment variable not set in production
        if (!isDevelopment && (allowedOriginsEnv == null || allowedOriginsEnv.isEmpty())) {
            configuration.setAllowedOrigins(Arrays.asList(
                "https://portal.walldotbuilders.com",
                "https://www.walldotbuilders.com"
            ));
            logger.warn("CORS: Using default production origins. Consider setting CORS_ALLOWED_ORIGINS environment variable.");
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
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