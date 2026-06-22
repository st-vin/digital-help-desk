package com.mku.helpdesk.config;

import com.mku.helpdesk.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // .formLogin(AbstractHttpConfigurer::disable)
                // .httpBasic(AbstractHttpConfigurer::disable)
                // CSRF protection exists for cookie-based, server-rendered forms.
                // We're a stateless JSON API authenticated with a Bearer token in
                // a header, not a cookie — CSRF doesn't apply here, and leaving it
                // enabled just blocks your POST/PUT requests with confusing 403s
                // for no reason connected to your actual code.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authenticationProvider(authenticationProvider())
                // No sessions, ever. Every request must carry its own valid JWT.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ORDER MATTERS. More specific / more permissive rules MUST
                        // come before broader catch-all rules. If "/api/v1/**"
                        // .authenticated() were listed before "/api/v1/auth/**"
                        // .permitAll(), the broad rule would match first and your
                        // login endpoint would itself require you to already be
                        // logged in — a chicken-and-egg lockout. Public endpoints
                        // are listed first here for that reason.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Admin-only endpoints.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ICT staff dashboard + admin can also act as staff.
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("ICT_STAFF", "ADMIN")

                        // Student-only endpoints — includes /api/v1/tickets/categories,
                        // which is fine since a student must be logged in to submit
                        // a ticket anyway.
                        .requestMatchers("/api/v1/tickets/**").hasRole("STUDENT")

                        // Anything else not explicitly listed above requires SOME
                        // valid authentication, but no specific role. Good safety net
                        // in case you add a new endpoint and forget to add a matcher
                        // for it — it fails closed (locked down) rather than open.
                        .anyRequest().authenticated()
                )
                // Insert our filter BEFORE Spring's own username/password filter,
                // since we're replacing that mechanism entirely with JWT.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 10 is bcrypt's default and a reasonable balance for a
        // student project — high enough to resist brute force, low enough
        // not to make every login request noticeably slow.
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Without this, your API will work perfectly in Postman (Postman doesn't
     * enforce CORS) and then fail with a browser console CORS error the
     * moment your vanilla-JS frontend calls the same endpoint with fetch().
     * This is one of the most common "it worked five minutes ago" moments
     * in this kind of project. Test a real fetch() call from a blank HTML
     * file today, not in Phase 3, so you're not debugging this AND new
     * frontend code at the same time later.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
