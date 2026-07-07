package com.ondrecreates.identityserver.support;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

/** Generates a currently-valid TOTP code for a given Base32 secret, mirroring what an authenticator app would show. */
public final class TotpCodes {

    private static final int PERIOD_SECONDS = 30;

    private TotpCodes() {
    }

    public static String currentCode(String base32Secret) {
        try {
            long counter = new SystemTimeProvider().getTime() / PERIOD_SECONDS;
            return new DefaultCodeGenerator().generate(base32Secret, counter);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP code for test", e);
        }
    }
}
