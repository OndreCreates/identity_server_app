package com.ondrecreates.identityserver.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminIndexController {

    @GetMapping("/admin")
    public String index() {
        return "redirect:/admin/clients";
    }
}
