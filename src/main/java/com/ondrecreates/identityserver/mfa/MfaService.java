package com.ondrecreates.identityserver.mfa;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String ISSUER = "OndreCreates Identity Server";

    private final MfaSecretRepository mfaSecretRepository;
    private final MfaRecoveryCodeRepository mfaRecoveryCodeRepository;
    private final AppUserRepository appUserRepository;
    private final MfaSecretCipher cipher;
    private final PasswordEncoder passwordEncoder;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();

    public MfaService(MfaSecretRepository mfaSecretRepository,
                       MfaRecoveryCodeRepository mfaRecoveryCodeRepository,
                       AppUserRepository appUserRepository,
                       MfaSecretCipher cipher,
                       PasswordEncoder passwordEncoder) {
        this.mfaSecretRepository = mfaSecretRepository;
        this.mfaRecoveryCodeRepository = mfaRecoveryCodeRepository;
        this.appUserRepository = appUserRepository;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isEnabled(Long userId) {
        return mfaSecretRepository.findByUserId(userId).isPresent();
    }

    /** Used by the login-time authorization check, which only has the principal's email/username. */
    public boolean isEnabledForEmail(String email) {
        return appUserRepository.findByEmail(email)
                .map(user -> isEnabled(user.getId()))
                .orElse(false);
    }

    /**
     * Checks a login-time code against the user's TOTP secret first, then their unused
     * recovery codes. A matching recovery code is consumed (marked used) on success.
     */
    @Transactional
    public boolean verifyLoginCode(Long userId, String code) {
        MfaSecret secret = mfaSecretRepository.findByUserId(userId).orElse(null);
        if (secret == null) {
            return false;
        }
        if (verifyCode(cipher.decrypt(secret.getSecretEncrypted()), code)) {
            return true;
        }
        return consumeRecoveryCodeIfValid(userId, code);
    }

    private boolean consumeRecoveryCodeIfValid(Long userId, String code) {
        for (MfaRecoveryCode recoveryCode : mfaRecoveryCodeRepository.findByUserId(userId)) {
            if (!recoveryCode.isUsed() && passwordEncoder.matches(code, recoveryCode.getCodeHash())) {
                recoveryCode.markUsed();
                mfaRecoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String qrCodeDataUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] png = qrGenerator.generate(data);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + Base64.getEncoder().encodeToString(png);
        } catch (QrGenerationException ex) {
            throw new IllegalStateException("Failed to generate MFA QR code", ex);
        }
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    /** Persists the confirmed secret and issues a fresh set of recovery codes, replacing any that existed before. */
    @Transactional
    public List<String> enroll(Long userId, String rawSecret) {
        mfaSecretRepository.deleteByUserId(userId);
        mfaSecretRepository.save(new MfaSecret(userId, cipher.encrypt(rawSecret)));

        mfaRecoveryCodeRepository.deleteByUserId(userId);
        String[] codes = recoveryCodeGenerator.generateCodes(RECOVERY_CODE_COUNT);
        for (String code : codes) {
            mfaRecoveryCodeRepository.save(new MfaRecoveryCode(userId, passwordEncoder.encode(code)));
        }
        return List.of(codes);
    }

    @Transactional
    public void disable(Long userId) {
        mfaSecretRepository.deleteByUserId(userId);
        mfaRecoveryCodeRepository.deleteByUserId(userId);
    }
}
