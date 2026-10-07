package com.yike.coffee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yike.coffee.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain chain(HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/auth/dev-login", "/api/v1/auth/wechat-login", "/api/v1/auth/refresh",
                    "/api/v1/public/**", "/uploads/**", "/actuator/health", "/api/v3/api-docs/**", "/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(res.getOutputStream(), new ApiResponse<>("UNAUTHORIZED", "请先登录", null,
                    String.valueOf(req.getAttribute("requestId"))));
            }).accessDeniedHandler((req, res, ex) -> {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN); res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(res.getOutputStream(), new ApiResponse<>("FORBIDDEN", "无权执行此操作", null,
                    String.valueOf(req.getAttribute("requestId"))));
            }))
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}
