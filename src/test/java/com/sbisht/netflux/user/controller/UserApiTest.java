package com.sbisht.netflux.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbisht.netflux.user.TestRsaKeys;
import com.sbisht.netflux.user.TestcontainersConfiguration;
import com.sbisht.netflux.user.dto.LoginRequest;
import com.sbisht.netflux.user.dto.LoginResponse;
import com.sbisht.netflux.user.dto.RegisterRequest;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class UserApiTest {

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should register user and store BCrypt-hashed password")
    void shouldRegisterUser() {
        restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("carol", "password123", "Carol"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        var storedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = ?", String.class, "carol");
        assertThat(storedPassword).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", storedPassword)).isTrue();
    }

    @Test
    @DisplayName("Should return 409 ProblemDetail when username already exists")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn409WhenUsernameExists() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("alice", "password123", "Alice Again"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Resource already exists");
    }

    @Test
    @DisplayName("Should return 409 when concurrent registrations race")
    void shouldReturn409WhenConcurrentRegistrationsRace() throws Exception {
        var requestCount = 10;
        var startLatch = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(requestCount)) {
            var futures = IntStream.range(0, requestCount)
                    .mapToObj(i -> executor.submit(() -> {
                        startLatch.await();
                        return registerStatus(new RegisterRequest("racer", "password123", "Racer"));
                    }))
                    .toList();
            startLatch.countDown();

            var statuses = futures.stream()
                    .map(UserApiTest::await)
                    .toList();

            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        }

        var userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, "racer");
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when register request is invalid")
    void shouldReturn400WhenRegisterRequestInvalid() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("ab", "short", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when request body is malformed")
    void shouldReturn400WhenBodyMalformed() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"username\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("Should return signed token with userId subject and name on valid login")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldLoginWithValidCredentials() {
        var response = restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("alice", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.name()).isEqualTo("Alice");
        var jwt = NimbusJwtDecoder.withPublicKey(TestRsaKeys.publicKey())
                .build()
                .decode(response.token());
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(jwt.getSubject()).isEqualTo("1001");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when password is wrong")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn401WhenPasswordWrong() {
        var problem = restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("alice", "wrongpassword"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when username is unknown")
    void shouldReturn401WhenUsernameUnknown() {
        var problem = restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("nobody", "password123"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when login fields are blank")
    void shouldReturn400WhenLoginFieldsBlank() {
        var problem = restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("", " "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    private int registerStatus(RegisterRequest request) {
        return restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .returnResult()
                .getStatus()
                .value();
    }

    private static <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
