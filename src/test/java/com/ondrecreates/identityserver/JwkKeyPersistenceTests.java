package com.ondrecreates.identityserver;

import com.nimbusds.jose.jwk.RSAKey;
import com.ondrecreates.identityserver.config.JwkKeyRepository;
import com.ondrecreates.identityserver.config.JwkKeyService;
import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Signature;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Before this, the signing key was regenerated in memory on every startup, silently
 * invalidating every previously-issued token. loadOrGenerate() is called once per real
 * startup (it backs the jwkSource bean) -- calling it twice here stands in for "restart the
 * app" without actually restarting the process.
 */
class JwkKeyPersistenceTests extends AbstractIntegrationTest {

    @Autowired
    private JwkKeyService jwkKeyService;

    @Autowired
    private JwkKeyRepository jwkKeyRepository;

    @Test
    void secondCallReusesTheSameKeyInsteadOfGeneratingANewOne() throws Exception {
        RSAKey first = jwkKeyService.loadOrGenerate();
        RSAKey second = jwkKeyService.loadOrGenerate();

        assertThat(second.getKeyID()).isEqualTo(first.getKeyID());
        assertThat(second.toRSAPublicKey()).isEqualTo(first.toRSAPublicKey());
        assertThat(jwkKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void theStoredPrivateKeyActuallyMatchesThePublicKey() throws Exception {
        RSAKey key = jwkKeyService.loadOrGenerate();
        byte[] message = "verify the round trip through DB storage didn't corrupt the key".getBytes();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key.toRSAPrivateKey());
        signer.update(message);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key.toRSAPublicKey());
        verifier.update(message);
        assertThat(verifier.verify(signature)).isTrue();
    }
}
