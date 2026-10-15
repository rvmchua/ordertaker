package com.rvmc.ordertaker.security;

import com.rvmc.ordertaker.entity.enums.Role;
import io.jsonwebtoken.io.Decoders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {
    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        String encoded = properties.jwt() == null ? null : properties.jwt().secretBase64();
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
        }

        byte[] key;
        try {
            key = Decoders.BASE64.decode(encoded);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64", exception);
        }
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(JwtProperties properties) {
        List<String> origins = properties.cors() == null
                ? List.of()
                : properties.cors().allowedOrigins();
        origins.forEach(SecurityConfiguration::validateOrigin);

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.setStatus(401)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/signup", "/auth/login", "/auth/restaurants/signup",
                                "/auth/restaurants/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants",
                                "/api/v1/restaurants/{restaurantId:[0-9]+}").permitAll()
                        .requestMatchers("/api/v1/customers/**").hasAuthority(Role.CUSTOMER.getPrefixedName())
                        .requestMatchers("/api/v1/restaurants/**").hasAuthority(Role.RESTAURANT.getPrefixedName())
                        .requestMatchers("/auth/logout").hasAnyAuthority(
                                Role.CUSTOMER.getPrefixedName(), Role.RESTAURANT.getPrefixedName())
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void validateOrigin(String origin) {
        if ("*".equals(origin)) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must not contain wildcard origins");
        }
        try {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
                throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must contain exact HTTP(S) origins");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS contains an invalid origin", exception);
        }
    }
}
