package com.ondrecreates.identityserver.mfa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaSecretRepository extends JpaRepository<MfaSecret, Long> {

    Optional<MfaSecret> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
