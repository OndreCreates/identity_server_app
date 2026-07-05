package com.ondrecreates.identityserver.admin;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/clients")
public class AdminClientController {

    private final ClientAdminService clientAdminService;

    public AdminClientController(ClientAdminService clientAdminService) {
        this.clientAdminService = clientAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientAdminService.listClients());
        return "admin/clients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ClientFormDto());
        model.addAttribute("isNew", true);
        return "admin/clients/form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") ClientFormDto form, Model model) {
        try {
            clientAdminService.save(form);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("isNew", true);
            model.addAttribute("error", ex.getMessage());
            return "admin/clients/form";
        }
        return "redirect:/admin/clients";
    }

    @GetMapping("/{clientId}/edit")
    public String editForm(@PathVariable String clientId, Model model) {
        RegisteredClient client = clientAdminService.findByClientId(clientId);
        if (client == null) {
            return "redirect:/admin/clients";
        }
        model.addAttribute("form", ClientFormDto.from(client));
        model.addAttribute("isNew", false);
        return "admin/clients/form";
    }

    // clientId is read-only once created (see form.html) -- save() always resolves the
    // client to update by the id embedded in the form, not the path variable.
    @PostMapping("/{clientId}")
    public String update(@PathVariable String clientId, @ModelAttribute("form") ClientFormDto form, Model model) {
        try {
            clientAdminService.save(form);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("isNew", false);
            model.addAttribute("error", ex.getMessage());
            return "admin/clients/form";
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/{clientId}/delete")
    public String delete(@PathVariable String clientId) {
        clientAdminService.delete(clientId);
        return "redirect:/admin/clients";
    }
}
