package com.ondrecreates.identityserver.mfa;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

/**
 * Handles POST /login/mfa: takes whatever Authentication is already in the SecurityContext
 * (password-verified, per {@link TotpAuthenticationProvider}) and the submitted code, and lets
 * the provider decide whether to merge in the TOTP factor.
 */
public class TotpAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public TotpAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/login/mfa"));
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(new SavedRequestAwareAuthenticationSuccessHandler());
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/login/mfa?error"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        String code = request.getParameter("code");
        return getAuthenticationManager().authenticate(new TotpAuthenticationToken(current, code));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                               AuthenticationException failed) throws IOException, ServletException {
        // Deliberately skip the base class's SecurityContextHolder.clearContext(): the password
        // factor the user already holds is still good, only the TOTP attempt failed. Clearing it
        // would force a full re-login instead of just letting them retry the code.
        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }
}
