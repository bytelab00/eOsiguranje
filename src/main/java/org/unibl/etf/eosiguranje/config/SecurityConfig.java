package org.unibl.etf.eosiguranje.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.unibl.etf.eosiguranje.filter.AccessControlFilter;
import org.unibl.etf.eosiguranje.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AccessControlFilter accessControlFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AccessControlFilter accessControlFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.accessControlFilter = accessControlFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // auth endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // admin zone
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // public policy browsing
                        .requestMatchers("/api/policies/**").permitAll()

                        // Stripe/PayPal webhook (public)
                        .requestMatchers("/api/purchase/webhook").permitAll()

                        // authenticated purchase flow
                        .requestMatchers("/api/purchase/**").authenticated()

                        // everything else
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // JWT FIRST
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Business-rules filter AFTER JWT
                .addFilterAfter(accessControlFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
