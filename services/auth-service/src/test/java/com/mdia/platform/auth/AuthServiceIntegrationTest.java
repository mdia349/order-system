package com.mdia.platform.auth;

import com.mdia.platform.auth.user.AppUser;
import com.mdia.platform.auth.user.AppUserRepository;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("authdb")
                    .withUsername("auth")
                    .withPassword("authpass");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.dll-auto", () -> "validate");

        registry.add("app.jwt.issuer", () -> "portfolio-platform");
        registry.add("app.jwt.secret", () -> "test_secret_at_least_32_chars_long_123456");
        registry.add("app.jwt.accessTokenMinutes", () -> "30");
    }

    @LocalServerPort
    int port;

    @Autowired
    AppUserRepository repo;

    @Autowired
    PasswordEncoder encoder;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        repo.deleteAll();
    }

    @Test
    void register_createsUserRow() {
        client.post()
                .uri("/auth/register")
                .bodyValue(Map.of("email", "reg@test.com", "password", "Password123!"))
                .exchange()
                .expectStatus().isCreated();

        assertThat(repo.existsByEmail("reg@test.com")).isTrue();
    }

    @Test
    void login_returnsAccessToken() {
        seedUser("login@test.com", "Password123!");

        Map<?, ?> resp = client.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", "login@test.com", "password", "Password123!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(resp).isNotNull();
        assertThat(resp.get("accessToken")).isInstanceOf(String.class);
        assertThat(((String) resp.get("accessToken"))).isNotBlank();
    }

    @Test
    void me_returnsSubjectAndRolesWithToken() {
        seedUser("me@test.com", "Password123!");
        String token = loginAndGetToken("me@test.com", "Password123!");

        client.get()
                .uri("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.subject").exists()
                .jsonPath("$.roles").isArray()
                .jsonPath("$.roles[0]").isEqualTo("ROLE_USER");
    }

    @Test
    void me_returns401WithoutToken() {
        client.get()
                .uri("/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String loginAndGetToken(String email, String password) {
        Map<?, ?> resp = client.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(resp).isNotNull();
        return (String) resp.get("accessToken");
    }

    private void seedUser(String email, String password) {
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                encoder.encode(password),
                "ROLE_USER"
        );
        repo.save(user);
    }
}
