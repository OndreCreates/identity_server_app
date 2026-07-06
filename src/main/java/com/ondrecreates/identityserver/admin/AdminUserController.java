package com.ondrecreates.identityserver.admin;

import com.ondrecreates.identityserver.user.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userAdminService.listUsers());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserFormDto());
        model.addAttribute("isNew", true);
        return "admin/users/form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") UserFormDto form, Model model) {
        try {
            userAdminService.create(form);
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("isNew", true);
            model.addAttribute("error", friendlyMessage(ex));
            return "admin/users/form";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AppUser user = userAdminService.require(id);
        model.addAttribute("form", UserFormDto.from(user));
        model.addAttribute("isNew", false);
        model.addAttribute("userId", id);
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") UserFormDto form, Model model,
                          Principal principal) {
        try {
            userAdminService.update(id, form, principal.getName());
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("isNew", false);
            model.addAttribute("userId", id);
            model.addAttribute("error", friendlyMessage(ex));
            return "admin/users/form";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            userAdminService.delete(id, principal.getName());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private static String friendlyMessage(Exception ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return "Email už existuje, nebo jsou zadaná data neplatná.";
        }
        return ex.getMessage();
    }
}
