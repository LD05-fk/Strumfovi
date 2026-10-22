package hr.fer.artconnect.admin.web;

import hr.fer.artconnect.admin.service.AdminUserService;
import hr.fer.artconnect.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService users;

    public AdminUserController(AdminUserService users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminUser> getUsers(@Valid @ModelAttribute AdminUserQueryParams params) {
        return users.getUsers(params);
    }
}
