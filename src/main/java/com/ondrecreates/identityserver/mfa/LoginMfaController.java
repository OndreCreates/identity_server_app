package com.ondrecreates.identityserver.mfa;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginMfaController {

    @GetMapping("/login/mfa")
    public String challengeForm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!MfaAuthorities.hasFactor(authentication, FactorGrantedAuthority.PASSWORD_AUTHORITY)) {
            // Landed here without even having logged in with a password -- start over.
            return "redirect:/login";
        }
        if (MfaAuthorities.hasFactor(authentication, MfaAuthorities.TOTP_FACTOR)) {
            // Already has both factors, nothing left to challenge.
            return "redirect:/";
        }
        return "mfa/challenge";
    }
}
