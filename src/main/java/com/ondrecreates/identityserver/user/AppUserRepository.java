package com.ondrecreates.identityserver.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    /** For controllers acting on the currently authenticated principal, where "not found" can only mean a bug. */
    default AppUser requireByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
