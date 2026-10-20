package hr.fer.artconnect.admin.service;

import hr.fer.artconnect.admin.domain.AdminUserRepository;
import hr.fer.artconnect.admin.web.AdminUser;
import hr.fer.artconnect.admin.web.AdminUserQueryParams;
import hr.fer.artconnect.common.PageResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {
    private final AdminUserRepository users;

    public AdminUserService(AdminUserRepository users) {
        this.users = users;
    }

    public PageResponse<AdminUser> getUsers(AdminUserQueryParams params) {
        // Stalni redoslijed createdAt DESC, id DESC već je zadan u upitu.
        var page = users.findUsers(params.getQ(), params.getRole(), params.getStatus(),
                params.getOrganizerApproval(), params.toPageable(Sort.unsorted()));
        return PageResponse.from(page.map(row ->
                AdminUserMapper.toAdminUser(row.getUser(), row.getOrganizationName())));
    }
}
