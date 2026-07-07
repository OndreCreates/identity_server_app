package com.ondrecreates.identityserver.admin;

import com.ondrecreates.identityserver.audit.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private static final int MAX_ENTRIES = 200;

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("entries",
                auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_ENTRIES)));
        return "admin/audit/list";
    }
}
