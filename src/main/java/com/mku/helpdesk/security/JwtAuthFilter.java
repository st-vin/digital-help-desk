package com.mku.helpdesk.security;

import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per incoming request, BEFORE Spring Security's own auth
 * filters. Its only job: if there's a valid Bearer token, tell Spring
 * Security who the user is. If not, do nothing and let the request
 * continue — SecurityConfig's authorizeHttpRequests rules will reject
 * it with a 401/403 further down the chain if the endpoint requires
 * authentication.
 *
 * DESIGN CHOICE — we set the User entity itself as the Authentication
 * principal, instead of implementing Spring Security's UserDetails
 * interface. This is a deliberate simplification: since we're fully
 * stateless (no sessions, no AuthenticationManager doing the login for
 * us — AuthService does that manually with PasswordEncoder.matches()),
 * we don't need the UserDetails contract at all. The tradeoff: in
 * controllers, you use @AuthenticationPrincipal User user (not
 * UserDetails) to get the logged-in user. That's intentional, not a bug.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No header, or header doesn't start with "Bearer " (note the
        // trailing space) -> nothing to do, let the request continue
        // unauthenticated. This is also what happens for public endpoints
        // like /api/v1/auth/login, which never send a token in the first
        // place — that's expected and fine.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " (7 characters, including the space) to get the
        // raw token. Forgetting this exact substring length is the single
        // most common JWT bug — get it wrong and parsing fails with a
        // confusing "Malformed JWT" exception that has nothing to do with
        // your actual token.
        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);

            // The "already authenticated" check matters in theory for
            // filter chains with multiple auth mechanisms; harmless here
            // but it's the conventional guard to include.
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (!jwtUtil.isTokenValid(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && user.isActive()) {
                    // THE "ROLE_" PREFIX GOTCHA: Spring Security's hasRole("ADMIN")
                    // actually checks for an authority literally named "ROLE_ADMIN".
                    // If you build the authority as just "ADMIN" here, hasRole()
                    // calls in SecurityConfig will ALWAYS return false — with a
                    // perfectly valid token — and you'll get 403s on everything
                    // with zero indication this prefix is the cause. We add it
                    // explicitly here so SecurityConfig can use the more readable
                    // hasRole(...) instead of hasAuthority(...) everywhere.
                    List<GrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Deliberately swallow the exception here rather than letting it
            // propagate. If extractEmail/isTokenValid throws (malformed,
            // tampered, or expired token), an uncaught exception inside a
            // filter often surfaces to the client as a confusing 500 Internal
            // Server Error instead of a clean 401. By catching it and simply
            // NOT setting an authentication, we let SecurityConfig's normal
            // access rules turn this into a proper 401/403 downstream.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
