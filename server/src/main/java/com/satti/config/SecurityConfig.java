package com.satti.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
//    private final SessionAuthFilter sessionAuthFilter;
//
//    @Autowired
//    public SecurityConfig(SessionAuthFilter sessionAuthFilter) {
//        this.sessionAuthFilter = sessionAuthFilter;
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 以下采用spring security 6.1+ 的配置方式
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable) // 禁用CSRF保护
            .authorizeHttpRequests(auth-> auth
                    .requestMatchers("/user/movie/register").permitAll()
                    .requestMatchers("/user/movie/login").permitAll()
                    .requestMatchers("user/movie/hello").permitAll()
                    .anyRequest().authenticated() // 其他请求需要认证
            )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler()))
//                .sessionManagement(sessionManagement->sessionManagement)
//                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 如果需要则创建会话
                .sessionManagement(sessionManagementCustomizer -> sessionManagementCustomizer
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .logout(logout -> logout
                    .logoutUrl("/user/movie/logout") // 设置登出URL
                    .logoutSuccessUrl("/user/movie/hello") // 登出成功后跳转的URL
                    .invalidateHttpSession(true) // 使会话失效
                )
                .addFilterBefore(sessionAuthFilter(), UsernamePasswordAuthenticationFilter.class)
            .formLogin(AbstractHttpConfigurer::disable); // 禁用表单登录


        return http.build();
    }

    @Bean
    public SessionAuthFilter sessionAuthFilter() {
        return new SessionAuthFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8889"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            // 返回 JSON 格式的错误响应
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("{\"code\":403,\"msg\":\"无权限访问，请先登录\"}");
        };
    }
}
