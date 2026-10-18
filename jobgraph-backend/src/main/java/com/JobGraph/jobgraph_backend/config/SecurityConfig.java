package com.JobGraph.jobgraph_backend.config;

import com.JobGraph.jobgraph_backend.repository.AccountRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    // Keep login state in a server-side session and require a CSRF token for writes.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfRepository,
                                           SecurityContextRepository contextRepository) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .authorizeHttpRequests(auth -> auth
                // Error pages must be reachable so API failures keep their real HTTP status.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/gmail/callback").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                // Upload, profile, Gmail, and email APIs require a logged-in account.
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                response.sendError(401, "Login required")));
        return http.build();
    }

    @Bean
    // Passwords are stored as one-way hashes, never as plain text.
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    // Spring Security uses the account's login email to load its password hash.
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmail(email)
                .map(account -> User.withUsername(account.getEmail())
                        .password(account.getPasswordHash()).roles("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    // Both repositories use the browser's session cookie.
    SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
}
