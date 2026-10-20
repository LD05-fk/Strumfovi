package hr.fer.artconnect.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.admin.domain.AdminUserRepository;
import hr.fer.artconnect.admin.service.AdminUserService;
import hr.fer.artconnect.admin.web.AdminUserQueryParams;
import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock
    AdminUserRepository users;

    @InjectMocks
    AdminUserService service;

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,NULL,NULL,NULL",
            "Ana,NULL,NULL,NULL",
            "ANA@,ARTIST,ACTIVE,NULL",
            "NULL,ORGANIZER,NULL,PENDING",
            "Galerija,ORGANIZER,ACTIVE,APPROVED"
    }, nullValues = "NULL")
    void prosljedujeFiltreIStranicenjeIVracaDtoSMetapodacima(
            String q, UserRole role, UserStatus status, OrganizerApproval approval) {
        var params = new AdminUserQueryParams();
        params.setQ(q);
        params.setRole(role);
        params.setStatus(status);
        params.setOrganizerApproval(approval);
        params.setPage(2);
        params.setPageSize(1);

        var user = mock(User.class);
        UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        var row = mock(AdminUserRepository.UserWithOrganization.class);
        when(row.getUser()).thenReturn(user);
        when(row.getOrganizationName()).thenReturn("Galerija");
        var pageable = PageRequest.of(1, 1);
        when(users.findUsers(q, role, status, approval, pageable))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 3));

        var response = service.getUsers(params);

        verify(users).findUsers(q, role, status, approval, pageable);
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().id()).isEqualTo(id);
        assertThat(response.data().getFirst().organizationName()).isEqualTo("Galerija");
        assertThat(response.meta().page()).isEqualTo(2);
        assertThat(response.meta().pageSize()).isEqualTo(1);
        assertThat(response.meta().total()).isEqualTo(3);
    }
}
