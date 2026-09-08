package com.jk.qurilishnazorat.rest;

import com.jk.qurilishnazorat.base.BaseTest;
import com.jk.qurilishnazorat.common.ApiConstants;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Feature("Notifications")
@DisplayName("GET /notifications — Bildirishnomalar ro'yxati")
class NotificationTests extends BaseTest {

    private static final String ENDPOINT = "/notifications";

    // =========================================================================
    //  Happy Path
    // =========================================================================

    @Nested
    class HappyPathTests {

        @Test
        @Story("Happy path")
        @DisplayName("Returns 200 with a valid response body")
        @Description("A plain request with no params must return 200 and a well-formed JSON body.")
        void getNotifications_returns200WithValidBody() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", notNullValue())
                .body("hasMore", notNullValue());
        }

        @Test
        @Story("Happy path")
        @DisplayName("Each notification item has all required fields")
        @Description("Every item must contain: id, type, title, body, publishedAt.")
        void getNotifications_itemsHaveRequiredFields() {
            Response response = given(requestSpec)
                    .queryParam("size", 5)
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.size()", greaterThan(0))
                    .extract().response();

            response.then()
                    .body("items.id", everyItem(notNullValue()))
                    .body("items.type", everyItem(notNullValue()))
                    .body("items.title", everyItem(notNullValue()))
                    .body("items.body", everyItem(notNullValue()))
                    .body("items.publishedAt", everyItem(notNullValue()));
            // Note: projectId can legitimately be null for some notification types
        }

        @Test
        @Story("Happy path")
        @DisplayName("publishedAt values are valid ISO-8601 UTC timestamps")
        @Description("Every item's publishedAt must be parseable as an ISO-8601 instant ending in 'Z'.")
        void getNotifications_publishedAtIsValidIso8601() {
            List<String> timestamps = given(requestSpec)
                    .queryParam("size", 5)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("items.publishedAt", String.class);

            for (String ts : timestamps) {
                assertThat(ts)
                        .as("publishedAt should end with 'Z' (UTC)")
                        .endsWith("Z");
                Instant.parse(ts); // throws DateTimeParseException if malformed
            }
        }

        @Test
        @Story("Happy path")
        @DisplayName("Items are ordered by publishedAt descending (newest first)")
        @Description("API contract states ordering is published_at DESC — each item must be <= the previous one.")
        void getNotifications_itemsOrderedByPublishedAtDesc() {
            List<String> timestamps = given(requestSpec)
                    .queryParam("size", 10)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("items.publishedAt", String.class);

            if (timestamps.size() < 2) return;

            for (int i = 0; i < timestamps.size() - 1; i++) {
                Instant current = Instant.parse(timestamps.get(i));
                Instant next = Instant.parse(timestamps.get(i + 1));
                assertThat(current)
                        .as("Item[%d].publishedAt (%s) must be >= item[%d].publishedAt (%s)", i, current, i + 1, next)
                        .isAfterOrEqualTo(next);
            }
        }
    }

    // =========================================================================
    //  Pagination Tests
    // =========================================================================

    @Nested
    class PaginationTests {

        @Test
        @Story("Pagination")
        @DisplayName("Default page size is at most 20 items")
        @Description("When ?size is omitted the server defaults to 20 — response must contain <= 20 items.")
        void getNotifications_noSizeParam_returnsAtMost20Items() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.size()", lessThanOrEqualTo(ApiConstants.DEFAULT_PAGE_SIZE));
        }

        @Test
        @Story("Pagination")
        @DisplayName("size=5 returns at most 5 items")
        @Description("Custom size param must be respected — response must contain <= requested size.")
        void getNotifications_customSize_returnsCorrectItemCount() {
            given(requestSpec)
                .queryParam("size", 5)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.size()", lessThanOrEqualTo(5));
        }

        @Test
        @Story("Pagination")
        @DisplayName("size=50 (maximum) returns at most 50 items")
        @Description("Maximum allowed page size is 50 — server must not exceed it.")
        void getNotifications_maxSize50_returnsAtMost50Items() {
            given(requestSpec)
                .queryParam("size", ApiConstants.MAX_PAGE_SIZE)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.size()", lessThanOrEqualTo(ApiConstants.MAX_PAGE_SIZE));
        }

        @ParameterizedTest(name = "size={0} → 400 Bad Request")
        @Story("Pagination")
        @DisplayName("Invalid size values return 400")
        @Description("size must be 1–50. Values outside this range must return 400 Bad Request.")
        @ValueSource(ints = {0, -1, 51, 100})
        void getNotifications_invalidSize_returns400(int invalidSize) {
            given(requestSpec)
                .queryParam("size", invalidSize)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                .body("status", equalTo(400))
                .body("type", notNullValue());
        }

        @Test
        @Story("Pagination")
        @DisplayName("nextCursor and hasMore are consistent")
        @Description("When hasMore=true nextCursor must not be null, and vice versa.")
        void getNotifications_nextCursorAndHasMoreAreConsistent() {
            Response response = given(requestSpec)
                    .queryParam("size", 1)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract().response();

            boolean hasMore = response.jsonPath().getBoolean("hasMore");
            String nextCursor = response.jsonPath().getString("nextCursor");

            if (hasMore) {
                assertThat(nextCursor)
                        .as("nextCursor must not be null when hasMore=true")
                        .isNotNull()
                        .isNotBlank();
            } else {
                assertThat(nextCursor)
                        .as("nextCursor must be null when hasMore=false")
                        .isNull();
            }
        }

        @Test
        @Story("Pagination")
        @DisplayName("Using nextCursor fetches the next page with different items")
        @Description("""
                Two-step cursor pagination test:
                1. Fetch page 1 (size=2) → capture nextCursor + item IDs.
                2. Fetch page 2 using that cursor → verify items are different from page 1.
                """)
        void getNotifications_withValidCursor_returnsNextPage() {
            Response firstPage = given(requestSpec)
                    .queryParam("size", 2)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .body("hasMore", equalTo(true))
                    .body("nextCursor", notNullValue())
                    .extract().response();

            String nextCursor = firstPage.jsonPath().getString("nextCursor");
            List<String> firstPageIds = firstPage.jsonPath().getList("items.id", String.class);

            List<String> secondPageIds = given(requestSpec)
                    .queryParam("size", 2)
                    .queryParam("cursor", nextCursor)
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.size()", greaterThan(0))
                    .extract()
                    .jsonPath()
                    .getList("items.id", String.class);

            assertThat(secondPageIds)
                    .as("Second page must contain different items from the first page")
                    .doesNotContainAnyElementsOf(firstPageIds);
        }

        @Test
        @Story("Pagination")
        @DisplayName("Malformed cursor returns 400")
        @Description("A cursor not issued by the server must return 400 Bad Request.")
        void getNotifications_malformedCursor_returns400() {
            given(requestSpec)
                .queryParam("cursor", "this-is-not-a-valid-cursor-!!@#$")
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }
    }

    // =========================================================================
    //  Caching Tests
    // =========================================================================

    @Nested
    class CachingTests {

        @Test
        @Story("Caching")
        @DisplayName("Response includes an ETag header")
        @Description("All public GETs must return an ETag header for conditional request support.")
        void getNotifications_responseHasETagHeader() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .header(ApiConstants.HEADER_ETAG, notNullValue());
        }

        @Test
        @Story("Caching")
        @DisplayName("Matching If-None-Match returns 304 with no body")
        @Description("Send the ETag back in If-None-Match — server must respond 304 Not Modified.")
        void getNotifications_matchingETag_returns304() {
            String etag = given(requestSpec)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .header(ApiConstants.HEADER_ETAG);

            given(requestSpec)
                .header(ApiConstants.HEADER_IF_NONE_MATCH, etag)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(304);
        }
    }

    // =========================================================================
    //  Header Tests
    // =========================================================================

    @Nested
    class HeaderTests {

        @Test
        @Story("Rate limiting")
        @DisplayName("Response includes all X-RateLimit-* headers")
        @Description("Server must return X-RateLimit-Limit, X-RateLimit-Remaining and X-RateLimit-Reset.")
        void getNotifications_hasRateLimitHeaders() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .header(ApiConstants.HEADER_RATE_LIMIT_LIMIT, notNullValue())
                .header(ApiConstants.HEADER_RATE_LIMIT_REMAINING, notNullValue())
                .header(ApiConstants.HEADER_RATE_LIMIT_RESET, notNullValue());
        }
    }
}
