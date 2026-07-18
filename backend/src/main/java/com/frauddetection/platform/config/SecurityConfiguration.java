package com.frauddetection.platform.config;

import com.frauddetection.platform.service.StepUpAuthenticationService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AccessDeniedHandler accessDeniedHandler,
        AuthorizationManager<RequestAuthorizationContext> stepUpAuthorizationManager,
        @Value("${fraud.security.machine-auth.mode:basic}") String machineAuthMode,
        @Value("${fraud.security.machine-auth.roles-claim:roles}") String rolesClaim
    ) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(request -> isMachineAuthorization(request.getHeader("Authorization"), machineAuthMode))
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/session").permitAll()
                .requestMatchers("/api/v1/auth/session", "/api/v1/auth/logout").authenticated()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/actuator/**").hasRole("PLATFORM_ADMIN")
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").hasRole("PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments").hasAnyRole("SCORING_CLIENT", "FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments/simulations").hasAnyRole("SCORING_CLIENT", "FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments/simulations/compare").hasAnyRole("SCORING_CLIENT", "FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments/simulations/compare-saved-profile/*").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-assessments/rules").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-assessments/scoring-profiles/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments/scoring-profiles").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/security/step-up/token", "/api/v1/security/step-up/token/resend", "/api/v1/security/step-up/revoke").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/security/step-up/verify").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/security/step-up/deliveries").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-assessments/scoring-profiles/*/activate").access(stepUpAuthorizationManager)
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-outcomes/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-outcomes/**").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-operations/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-operations/outbound-events/*/incident-note").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-operations/outbound-events/*/retry", "/api/v1/fraud-operations/outbound-events/retry-failed", "/api/v1/fraud-operations/outbound-events/dispatch-now").access(stepUpAuthorizationManager)
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-replays").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-replays/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/challenge-outcome").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-cases/export").access(stepUpAuthorizationManager)
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-cases/**").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-cases/*/assign", "/api/v1/fraud-cases/*/notes").hasAnyRole("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-cases/*/escalate").hasAnyRole("FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-cases/*/release", "/api/v1/fraud-cases/*/confirm-decline", "/api/v1/fraud-cases/*/resolve").access(stepUpAuthorizationManager)
                .anyRequest().denyAll()
            );

        if ("oauth2".equalsIgnoreCase(machineAuthMode)) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(machineJwtAuthenticationConverter(rolesClaim))));
        } else {
            http.httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    JwtAuthenticationConverter machineJwtAuthenticationConverter(String rolesClaim) {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            authorities.addAll(roleAuthorities(jwt, rolesClaim));
            return authorities.stream().distinct().toList();
        });
        return authenticationConverter;
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(StepUpAuthenticationService stepUpAuthenticationService) {
        return (request, response, exception) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (stepUpAuthenticationService.isProtectedRoute(request)
                && stepUpAuthenticationService.hasSupervisorAuthority(authentication)
                && !stepUpAuthenticationService.hasValidStepUp(authentication, request)) {
                writeProblemResponse(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Step-up authentication required",
                    "Complete one-time verification at /api/v1/security/step-up before retrying this operation.",
                    request.getRequestURI()
                );
                return;
            }

            writeProblemResponse(
                response,
                HttpStatus.FORBIDDEN,
                "Access denied",
                "Your role does not permit this operation.",
                request.getRequestURI()
            );
        };
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> stepUpAuthorizationManager(
        StepUpAuthenticationService stepUpAuthenticationService
    ) {
        return new AuthorizationManager<>() {
            @Override
            public AuthorizationResult authorize(
                Supplier<? extends Authentication> authentication,
                RequestAuthorizationContext context
            ) {
                return new AuthorizationDecision(
                stepUpAuthenticationService.hasSupervisorAuthority(authentication.get())
                    && stepUpAuthenticationService.hasValidStepUp(authentication.get(), context.getRequest())
                );
            }
        };
    }

    private void writeProblemResponse(
        jakarta.servlet.http.HttpServletResponse response,
        HttpStatus status,
        String title,
        String detail,
        String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write("""
            {
              "title":"%s",
              "status":%d,
              "detail":"%s",
              "path":"%s"
            }
            """.formatted(title, status.value(), detail, path));
    }

    private boolean isMachineAuthorization(String authorization, String machineAuthMode) {
        if (authorization == null) {
            return false;
        }
        String expectedScheme = "oauth2".equalsIgnoreCase(machineAuthMode) ? "Bearer " : "Basic ";
        return authorization.regionMatches(true, 0, expectedScheme, 0, expectedScheme.length());
    }

    private Collection<GrantedAuthority> roleAuthorities(Jwt jwt, String rolesClaim) {
        Object claim = jwt.getClaims().get(rolesClaim);
        if (claim instanceof Collection<?> roles) {
            return roles.stream()
                .map(Object::toString)
                .map(this::toRoleAuthority)
                .toList();
        }
        if (claim instanceof String roles) {
            return Arrays.stream(roles.split("[ ,]+"))
                .filter(role -> !role.isBlank())
                .map(this::toRoleAuthority)
                .toList();
        }
        return List.of();
    }

    private GrantedAuthority toRoleAuthority(String role) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return new SimpleGrantedAuthority(normalizedRole);
    }
}
