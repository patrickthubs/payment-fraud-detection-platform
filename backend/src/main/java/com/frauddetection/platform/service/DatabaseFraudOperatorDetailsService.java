package com.frauddetection.platform.service;

import com.frauddetection.platform.entity.FraudOperatorEntity;
import com.frauddetection.platform.repository.FraudOperatorRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseFraudOperatorDetailsService implements UserDetailsService {

    private final FraudOperatorRepository fraudOperatorRepository;

    public DatabaseFraudOperatorDetailsService(FraudOperatorRepository fraudOperatorRepository) {
        this.fraudOperatorRepository = fraudOperatorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        FraudOperatorEntity operator = fraudOperatorRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Fraud operator %s was not found.".formatted(username)));

        List<SimpleGrantedAuthority> authorities = operator.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
            .toList();

        return new FraudOperatorPrincipal(
            operator.getUsername(),
            operator.getPasswordHash(),
            operator.isActive(),
            true,
            true,
            operator.isAccountNonLocked(),
            authorities,
            operator.getOrganization()
        );
    }
}
