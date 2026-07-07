package com.ondrecreates.identityserver.mfa;

import com.ondrecreates.identityserver.audit.AuditEventType;
import com.ondrecreates.identityserver.audit.AuditLog;
import com.ondrecreates.identityserver.audit.AuditLogRepository;
import com.ondrecreates.identityserver.audit.LockoutPolicy;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Controller
@RequestMapping("/account/mfa")
public class MfaController {

    private static final String SESSION_PENDING_SECRET = "PENDING_MFA_SECRET";

    private final AppUserRepository appUserRepository;
    private final MfaService mfaService;
    private final AuditLogRepository auditLogRepository;

    public MfaController(AppUserRepository appUserRepository, MfaService mfaService,
                          AuditLogRepository auditLogRepository) {
        this.appUserRepository = appUserRepository;
        this.mfaService = mfaService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String status(Principal principal, Model model) {
        AppUser user = currentUser(principal);
        model.addAttribute("mfaEnabled", mfaService.isEnabled(user.getId()));
        return "mfa/status";
    }

    @GetMapping("/setup")
    public String setupForm(HttpSession session, Principal principal, Model model) {
        AppUser user = currentUser(principal);
        String secret = mfaService.generateSecret();
        session.setAttribute(SESSION_PENDING_SECRET, secret);
        model.addAttribute("secret", secret);
        model.addAttribute("qrCodeDataUri", mfaService.qrCodeDataUri(user.getEmail(), secret));
        return "mfa/setup";
    }

    @PostMapping("/setup")
    public String confirmSetup(@RequestParam String code, HttpSession session, Principal principal, Model model) {
        AppUser user = currentUser(principal);
        String pendingSecret = (String) session.getAttribute(SESSION_PENDING_SECRET);

        if (pendingSecret == null) {
            return "redirect:/account/mfa/setup";
        }

        if (!mfaService.verifyCode(pendingSecret, code)) {
            model.addAttribute("error", "Kód nesouhlasí, zkus to znovu.");
            model.addAttribute("secret", pendingSecret);
            model.addAttribute("qrCodeDataUri", mfaService.qrCodeDataUri(user.getEmail(), pendingSecret));
            return "mfa/setup";
        }

        // Rendered directly rather than redirected: a redirect would issue a brand new GET
        // that gets authorization-checked on its own, and this session hasn't proven the TOTP
        // factor yet at this exact moment (it just proved the secret works, which isn't the
        // same thing as completing the /login/mfa challenge) -- it would bounce the user into
        // a confusing second challenge before they ever see their own recovery codes.
        List<String> recoveryCodes = mfaService.enroll(user.getId(), pendingSecret);
        session.removeAttribute(SESSION_PENDING_SECRET);
        model.addAttribute("recoveryCodes", recoveryCodes);
        return "mfa/recovery-codes";
    }

    // Requires the current TOTP or a recovery code, not just an authenticated session --
    // otherwise a hijacked session that only has the password factor (before completing the
    // /login/mfa challenge) could permanently strip MFA off the account. Checked against the
    // same LockoutPolicy as the /login/mfa challenge itself (and recorded the same way) so
    // this form can't be used as an unthrottled side door around that lockout.
    @PostMapping("/disable")
    public String disable(@RequestParam String code, Principal principal, Model model) {
        AppUser user = currentUser(principal);

        long recentFailures = auditLogRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                user.getId(), AuditEventType.MFA_FAILURE, Instant.now().minus(LockoutPolicy.WINDOW));
        if (recentFailures >= LockoutPolicy.MAX_ATTEMPTS) {
            model.addAttribute("mfaEnabled", true);
            model.addAttribute("error", "Příliš mnoho neplatných pokusů, zkus to znovu později.");
            return "mfa/status";
        }

        if (!mfaService.verifyLoginCode(user.getId(), code)) {
            auditLogRepository.save(new AuditLog(user.getId(), user.getEmail(), AuditEventType.MFA_FAILURE, null));
            model.addAttribute("mfaEnabled", true);
            model.addAttribute("error", "Kód nesouhlasí, MFA zůstává zapnuté.");
            return "mfa/status";
        }

        mfaService.disable(user.getId());
        return "redirect:/account/mfa";
    }

    private AppUser currentUser(Principal principal) {
        return appUserRepository.requireByEmail(principal.getName());
    }
}
