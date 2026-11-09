package com.sbisht.netflux.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbisht.netflux.user.TestcontainersConfiguration;
import com.sbisht.netflux.user.dto.AddWatchlistItemRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = {"/sql/insert-users.sql", "/sql/insert-watchlist.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WatchlistApiTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    RestTestClient restTestClient;

    @Test
    @DisplayName("Should return movieIds in insertion order")
    void shouldReturnWatchlistInInsertionOrder() {
        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return empty list when user has no watchlist entries")
    void shouldReturnEmptyWatchlist() {
        assertThat(getWatchlist(1002L)).isEmpty();
    }

    @Test
    @DisplayName("Should add movie to watchlist")
    void shouldAddMovie() {
        restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "1002")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistItemRequest(500L))
                .exchange()
                .expectStatus().isCreated();

        assertThat(getWatchlist(1002L)).containsExactly(500L);
    }

    @Test
    @DisplayName("Should return 201 without duplicating when movie already in watchlist")
    void shouldAddExistingMovieIdempotently() {
        restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "1001")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistItemRequest(100L))
                .exchange()
                .expectStatus().isCreated();

        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is missing")
    void shouldReturn400WhenMovieIdMissing() {
        var problem = restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "1001")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistItemRequest(null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is not positive")
    void shouldReturn400WhenMovieIdNotPositive() {
        var problem = restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "1001")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistItemRequest(0L))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should remove movie from watchlist")
    void shouldRemoveMovie() {
        restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 100)
                .header(USER_ID_HEADER, "1001")
                .exchange()
                .expectStatus().isNoContent();

        assertThat(getWatchlist(1001L)).containsExactly(300L, 200L);
    }

    @Test
    @DisplayName("Should return 204 when removing movie not in watchlist")
    void shouldReturn204WhenRemovingAbsentMovie() {
        restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 999)
                .header(USER_ID_HEADER, "1001")
                .exchange()
                .expectStatus().isNoContent();

        assertThat(getWatchlist(1001L)).containsExactly(300L, 100L, 200L);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when deleting with non-positive movieId")
    void shouldReturn400WhenDeletingNonPositiveMovieId() {
        var problem = restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 0)
                .header(USER_ID_HEADER, "1001")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is missing")
    void shouldReturn401WhenHeaderMissing() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is not numeric")
    void shouldReturn401WhenHeaderNotNumeric() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "abc")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail on GET when user does not exist")
    void shouldReturn404OnGetWhenUserUnknown() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "9999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail on POST when user does not exist")
    void shouldReturn404OnPostWhenUserUnknown() {
        var problem = restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "9999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddWatchlistItemRequest(100L))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should return 404 ProblemDetail on DELETE when user does not exist")
    void shouldReturn404OnDeleteWhenUserUnknown() {
        var problem = restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 100)
                .header(USER_ID_HEADER, "9999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(404);
    }

    private List<Long> getWatchlist(Long userId) {
        return restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Long>>() {})
                .returnResult()
                .getResponseBody();
    }
}
