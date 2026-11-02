package com.example.auth;

import static com.example.auth.security.AuthCookies.ACCESS;
import static com.example.auth.security.AuthCookies.REFRESH;
import static com.example.auth.security.CookieAuthFilter.REFRESHED_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AuthFlowIntegrationTest {

    private static final String FRONTEND = "http://localhost:5173";

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2030-01-01T00:00:00Z"));
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired MutableClock clock;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private record Login(Cookie access, Cookie refresh, String sessionId) {}

    private Login login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        String sessionId = JsonPath.read(response.getContentAsString(), "$.sessionId");
        return new Login(response.getCookie(ACCESS), response.getCookie(REFRESH), sessionId);
    }

    @Test
    void loginSetsHttpOnlySecureStrictCookies() throws Exception {
        Login login = login("alice", "password");

        MockCookie access = (MockCookie) login.access();
        assertThat(access.isHttpOnly()).isTrue();
        assertThat(access.getSecure()).isTrue();
        assertThat(access.getSameSite()).isEqualTo("Strict");
        assertThat(access.getPath()).isEqualTo("/");
        assertThat(access.getMaxAge()).isEqualTo(600);

        MockCookie refresh = (MockCookie) login.refresh();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getMaxAge()).isEqualTo(7 * 24 * 3600);
    }

    @Test
    void wrongPasswordIs401WithoutCookies() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void protectedEndpointWithoutCookiesIs401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessCookieNeedsNoRefresh() throws Exception {
        Login login = login("alice", "password");

        mvc.perform(get("/api/me").cookie(login.access(), login.refresh()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(header().doesNotExist(REFRESHED_HEADER));
    }

    @Test
    void expiredAccessIsRefreshedInTheSameRequest() throws Exception {
        Login login = login("alice", "password");
        clock.advance(Duration.ofMinutes(11));

        MvcResult result = mvc.perform(get("/api/me").cookie(login.access(), login.refresh()))
                .andExpect(status().isOk())
                .andExpect(header().string(REFRESHED_HEADER, "true"))
                .andReturn();

        Cookie fresh = result.getResponse().getCookie(ACCESS);
        assertThat(fresh).isNotNull();
        assertThat(fresh.getValue()).isNotEqualTo(login.access().getValue());

        mvc.perform(get("/api/me").cookie(fresh, login.refresh()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(REFRESHED_HEADER));
    }

    @Test
    void missingAccessCookieIsRefreshed() throws Exception {
        Login login = login("alice", "password");

        mvc.perform(get("/api/me").cookie(login.refresh()))
                .andExpect(status().isOk())
                .andExpect(header().string(REFRESHED_HEADER, "true"));
    }

    @Test
    void refreshTokenIsNeverRotated() throws Exception {
        Login login = login("alice", "password");

        for (int i = 0; i < 3; i++) {
            clock.advance(Duration.ofMinutes(11));
            MvcResult result = mvc.perform(get("/api/me").cookie(login.refresh()))
                    .andExpect(status().isOk())
                    .andExpect(header().string(REFRESHED_HEADER, "true"))
                    .andReturn();
            assertThat(result.getResponse().getCookie(REFRESH)).isNull(); // same refresh cookie keeps working
        }
    }

    @Test
    void expiredRefreshTokenRequiresNewLogin() throws Exception {
        Login login = login("alice", "password");
        clock.advance(Duration.ofDays(7).plusSeconds(1));

        MvcResult result = mvc.perform(get("/api/me").cookie(login.access(), login.refresh()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getCookie(REFRESH).getMaxAge()).isZero(); // cleared
    }

    @Test
    void logoutRevokesImmediately() throws Exception {
        Login login = login("alice", "password");

        mvc.perform(post("/api/auth/logout").cookie(login.access(), login.refresh()))
                .andExpect(status().isNoContent());

        // The access token has not expired, but its session is revoked.
        mvc.perform(get("/api/me").cookie(login.access(), login.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokingAnotherSessionEndsIt() throws Exception {
        Login laptop = login("alice", "password");
        Login phone = login("alice", "password");

        mvc.perform(get("/api/auth/sessions").cookie(laptop.access(), laptop.refresh()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(phone.sessionId())));

        mvc.perform(delete("/api/auth/sessions/{id}", phone.sessionId()).cookie(laptop.access(), laptop.refresh()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me").cookie(phone.access(), phone.refresh()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").cookie(laptop.access(), laptop.refresh()))
                .andExpect(status().isOk());
    }

    @Test
    void cannotRevokeAnotherUsersSession() throws Exception {
        Login alice = login("alice", "password");
        Login admin = login("admin", "admin");

        mvc.perform(delete("/api/auth/sessions/{id}", admin.sessionId()).cookie(alice.access(), alice.refresh()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/me").cookie(admin.access(), admin.refresh()))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightAllowsListedOriginWithCredentials() throws Exception {
        mvc.perform(options("/api/me")
                        .header(HttpHeaders.ORIGIN, FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/me")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsExposesTheRefreshHeaderToTheFrontend() throws Exception {
        Login login = login("alice", "password");
        clock.advance(Duration.ofMinutes(11));

        mvc.perform(get("/api/me").header(HttpHeaders.ORIGIN, FRONTEND).cookie(login.access(), login.refresh()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString(REFRESHED_HEADER)));
    }

    @Test
    void openApiDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/auth/login")));
    }
}
