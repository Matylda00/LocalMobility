package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.services.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/emails")
    public List<String> getCreatedEmails() {
        return adminService.getCreatedEmails();
    }
}