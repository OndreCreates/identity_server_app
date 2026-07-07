package com.ondrecreates.identityserver.mfa;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;

/**
 * Spring Security's multi-factor {@code AuthorizationManager} denies a request that's missing
 * a required factor the same way it denies any other insufficient-authority request: as a plain
 * {@link AccessDeniedException}. There's no built-in "go complete this custom factor" redirect
 * for a factor Spring doesn't know about (TOTP isn't one of its built-in ones), so this handler
 * fills that gap -- if the denial is specifically "has password, missing TOTP", save the
 * request they were trying to reach and send them to the challenge page instead of a 403.
 */
public class MfaAccessDeniedHandler implements AccessDeniedHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final AccessDeniedHandler fallback = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPassword = MfaAuthorities.hasFactor(authentication, FactorGrantedAuthority.PASSWORD_AUTHORITY);
        boolean hasTotp = MfaAuthorities.hasFactor(authentication, MfaAuthorities.TOTP_FACTOR);

        if (hasPassword && !hasTotp) {
            requestCache.saveRequest(request, response);
            response.sendRedirect(request.getContextPath() + "/login/mfa");
            return;
        }

        fallback.handle(request, response, accessDeniedException);
    }
}
