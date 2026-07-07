package com.ondrecreates.identityserver.web;

import com.ondrecreates.identityserver.mfa.MfaService;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class AccountController {

    private final AppUserRepository appUserRepository;
    private final MfaService mfaService;

    public AccountController(AppUserRepository appUserRepository, MfaService mfaService) {
        this.appUserRepository = appUserRepository;
        this.mfaService = mfaService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String dashboard(Principal principal, Model model) {
        AppUser user = appUserRepository.requireByEmail(principal.getName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mfaEnabled", mfaService.isEnabled(user.getId()));
        return "account/dashboard";
    }
}
