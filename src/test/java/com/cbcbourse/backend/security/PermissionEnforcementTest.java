package com.cbcbourse.backend.security;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cbcbourse.backend.auth.JwtService;
import com.cbcbourse.backend.auth.dto.AuthenticatedUser;
import com.cbcbourse.backend.config.CorsProperties;
import com.cbcbourse.backend.config.SecurityConfig;
import com.cbcbourse.backend.kpi.KpiController;
import com.cbcbourse.backend.kpi.KpiService;
import com.cbcbourse.backend.kpi.dto.KpiCommercialResponse;
import com.cbcbourse.backend.kpi.dto.KpiPeriodeResponse;
import com.cbcbourse.backend.user.UserController;
import com.cbcbourse.backend.user.UserService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie que chaque endpoint sensible exige bien sa permission, et jamais un role : c'est le principe
 * du RBAC configurable retenu pour ce projet. Ces tests echouent si quelqu'un retire une annotation
 * {@code @PreAuthorize} ou se trompe de code de permission.
 */
@WebMvcTest(controllers = {UserController.class, KpiController.class})
@Import({SecurityConfig.class, PermissionEnforcementTest.TestBeans.class})
class PermissionEnforcementTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:3000"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KpiService kpiService;

    /** Requis par le filtre JWT charge avec la configuration de securite. */
    @MockitoBean
    private JwtService jwtService;

    private static Authentication authWith(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L, "utilisateur@test.local"),
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    @DisplayName("Sans token, la liste des utilisateurs renvoie 401 et non 403")
    void listeUtilisateursSansToken() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("MANAGE_ROLES ne donne pas acces a la gestion des utilisateurs")
    void listeUtilisateursAvecMauvaisePermission() throws Exception {
        mockMvc.perform(get("/api/users").with(authentication(authWith("MANAGE_ROLES"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGE_USERS donne acces a la gestion des utilisateurs")
    void listeUtilisateursAvecBonnePermission() throws Exception {
        given(userService.findAll(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/users").with(authentication(authWith("MANAGE_USERS"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VIEW_OWN_DASHBOARD donne acces a ses propres indicateurs")
    void mesKpisAvecPermission() throws Exception {
        given(kpiService.forUser(anyLong(), any(), any())).willReturn(unKpi());

        mockMvc.perform(get("/api/kpi/me")
                        .param("debut", "2026-01-01")
                        .param("fin", "2026-12-31")
                        .with(authentication(authWith("VIEW_OWN_DASHBOARD"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sans VIEW_OWN_DASHBOARD, ses propres indicateurs sont refuses")
    void mesKpisSansPermission() throws Exception {
        mockMvc.perform(get("/api/kpi/me")
                        .param("debut", "2026-01-01")
                        .param("fin", "2026-12-31")
                        .with(authentication(authWith("EXPORT_REPORTS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Voir ses propres indicateurs ne donne pas acces a ceux de toute l'equipe")
    void vueConsolideeSansPermission() throws Exception {
        mockMvc.perform(get("/api/kpi/commerciaux")
                        .param("debut", "2026-01-01")
                        .param("fin", "2026-12-31")
                        .with(authentication(authWith("VIEW_OWN_DASHBOARD"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEW_ALL_DASHBOARDS donne acces a la vue consolidee")
    void vueConsolideeAvecPermission() throws Exception {
        given(kpiService.forAllCommerciaux(any(), any())).willReturn(
                KpiPeriodeResponse.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), List.of(unKpi())));

        mockMvc.perform(get("/api/kpi/commerciaux")
                        .param("debut", "2026-01-01")
                        .param("fin", "2026-12-31")
                        .with(authentication(authWith("VIEW_ALL_DASHBOARDS"))))
                .andExpect(status().isOk());
    }

    private static KpiCommercialResponse unKpi() {
        return new KpiCommercialResponse(1L, "Awa Diallo", "awa@test.local", 2, new BigDecimal("1000.00"), 1, 3);
    }
}
