package com.frauddetection.platform.config;

import com.frauddetection.platform.service.StepUpAuthenticationService;
import java.io.IOException;
import java.util.function.Supplier;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AccessDeniedHandler accessDeniedHandler,
        AuthorizationManager<RequestAuthorizationContext> stepUpAuthorizationManager
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(Customizer.withDefaults())
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
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

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
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
}
