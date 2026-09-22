package com.rubenmarin.enrollmentservice.configuration;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        // Preserve the existing public health endpoint.
                        .requestMatchers("/actuator/health").permitAll()

                        // USER and ADMIN may read enrollments.
                        .requestMatchers(HttpMethod.GET, "/enrollments")
                        .hasAnyRole("USER", "ADMIN")

                        // Only ADMIN may create enrollments.
                        .requestMatchers(HttpMethod.POST, "/enrollments")
                        .hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // Authenticate using a validated JWT access token.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                // This API accepts explicitly supplied Bearer headers,
                // rather than authentication cookies.
                .csrf(AbstractHttpConfigurer::disable)

                // Do not persist authentication in an HTTP session.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        // Preserve Spring's default scope -> SCOPE_ authority mapping.
        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();

            Collection<GrantedAuthority> scopeAuthorities =
                    scopeConverter.convert(jwt);

            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            // Keycloak realm roles are nested under realm_access.roles.
            Object realmAccessClaim = jwt.getClaims().get("realm_access");

            if (realmAccessClaim instanceof Map<?, ?> realmAccess
                    && realmAccess.get("roles") instanceof Collection<?> roles) {

                for (Object role : roles) {
                    // Map only roles understood by this API.
                    if (role instanceof String roleName
                            && Set.of("USER", "ADMIN").contains(roleName)) {
                        authorities.add(
                                new SimpleGrantedAuthority("ROLE_" + roleName)
                        );
                    }
                }
            }

            return authorities;
        });

        return converter;
    }
}