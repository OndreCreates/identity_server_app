package com.ondrecreates.identityserver.user;

import com.ondrecreates.identityserver.audit.AuditEventType;
import com.ondrecreates.identityserver.audit.AuditLogRepository;
import com.ondrecreates.identityserver.audit.LockoutPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AppUserDetailsService implements UserDetailsService {

    // Same brute-force concern as the TOTP factor, one step earlier: without this, a
    // password could be guessed with unlimited attempts. DaoAuthenticationProvider checks
    // accountNonLocked before even comparing the password, so setting it here is all that's
    // needed -- no separate lockout enforcement code required.
    private final AppUserRepository appUserRepository;
    private final AuditLogRepository auditLogRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository, AuditLogRepository auditLogRepository) {
        this.appUserRepository = appUserRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));

        long recentFailures = auditLogRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                appUser.getId(), AuditEventType.LOGIN_FAILURE, Instant.now().minus(LockoutPolicy.WINDOW));

        return User.withUsername(appUser.getEmail())
                .password(appUser.getPassword())
                .disabled(!appUser.isEnabled())
                .accountLocked(recentFailures >= LockoutPolicy.MAX_ATTEMPTS)
                .authorities(appUser.getRoles().stream()
                        .map(role -> "ROLE_" + role)
                        .toArray(String[]::new))
                .build();
    }
}
