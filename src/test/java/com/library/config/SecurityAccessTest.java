package com.library.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The access rules from proposal 3.3: the catalog is public, circulation
 * needs an account, and the staff area needs the LIBRARIAN role. The last
 * test proves the seeded librarian can really sign in through form login —
 * seeder, UserDetailsService, and BCrypt hashing checked end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void catalogIsPublic() throws Exception {
        mvc.perform(get("/catalog")).andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void staffAreaSendsAnonymousVisitorsToLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void accountPageSendsAnonymousVisitorsToLogin() throws Exception {
        mvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void staffAreaIsForbiddenToMembers() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void staffAreaOpensForLibrarians() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }

    @Test
    void seededLibrarianCanSignIn() throws Exception {
        mvc.perform(formLogin().user("email", "librarian@library.local").password("changeme123"))
                .andExpect(authenticated().withRoles("LIBRARIAN"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin().user("email", "librarian@library.local").password("not-the-password"))
                .andExpect(unauthenticated());
    }
}
