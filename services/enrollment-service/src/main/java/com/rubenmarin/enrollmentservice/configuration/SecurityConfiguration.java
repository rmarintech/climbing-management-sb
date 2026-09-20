package com.rubenmarin.enrollmentservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize

                        // Health endpoint is public.
                        .requestMatchers("/actuator/health").permitAll()

                        // USER & ADMIN users may read enrollments.
                        .requestMatchers(HttpMethod.GET, "/enrollments")
                        .hasAnyRole("USER", "ADMIN")

                        // Only ADMIN may create enrollments
                        .requestMatchers(HttpMethod.POST, "/enrollments")
                        .hasRole("ADMIN")

                        // Every other HTTP request requires authentication.
                        .anyRequest()
                        .authenticated()
                )

                // Enable HTTP Basic authentication.
                .httpBasic(Customizer.withDefaults())

                // NOT:
                // "REST API → always disable CSRF"
                // BUT:
                // "Stateless API where authentication is not automatically
                // sent by the browser → CSRF protection may not be necessary"
                //
                // If we were building a traditional web application with:
                //     - login form → session cookie → browser
                // we would normally keep CSRF enabled.
                .csrf(AbstractHttpConfigurer::disable)


                // STATELESS: The server does not remember your authentication in an HTTP session between requests.
                // Spring Security will not use an HTTP session to persist authentication.
                // HTTP Basic authenticates each request independently.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
        ;


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder
    ) {

        UserDetails user = User.withUsername("ruben")
                .password(passwordEncoder.encode("1234"))
                .roles("USER")
                .build();

        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("1234"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}