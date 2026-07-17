package com.frauddetection.platform.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedOperatorService {

    public String resolveOperator(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authenticated operator identity is required.");
        }
        return authentication.getName();
    }
}
