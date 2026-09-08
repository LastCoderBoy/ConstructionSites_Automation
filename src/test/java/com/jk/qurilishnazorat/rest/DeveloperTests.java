package com.jk.qurilishnazorat.rest;

import com.jk.qurilishnazorat.base.BaseTest;
import com.jk.qurilishnazorat.common.ApiConstants;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Feature("Developers")
@DisplayName("Developer Endpoints — /developers & /developers/{id}")
class DeveloperTests extends BaseTest {

    private static final String LIST_ENDPOINT = "/developers";
    private static final String DETAIL_ENDPOINT = "/developers/{id}";

    /**
     * A real developer UUID fetched dynamically from the list endpoint
     * before any test runs. Used in detail tests to ensure the ID exists
     * in the current database state.
     */
    private static String dynamicDeveloperId;

    @BeforeAll
    static void fetchDeveloperId() {
        dynamicDeveloperId = given(requestSpec)
                .queryParam("size", 1)
            .when()
                .get(LIST_ENDPOINT)
            .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .extract()
                .jsonPath()
                .getString("items[0].id");
    }

    // =========================================================================
    //  GET /developers — List
    // =========================================================================

    @Nested
    @DisplayName("GET /developers — List")
    class ListTests {

        @Nested
        class HappyPathTests {

            @Test
            @Story("Happy path")
            @DisplayName("Returns 200 with a well-formed response body")
            @Description("A plain request with no params must return 200, items array, hasMore and nextCursor fields.")
            void getDevelopers_returns200WithValidBody() {
                given(requestSpec)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items", notNullValue())
                    .body("hasMore", notNullValue())
                    .body("nextCursor", anyOf(notNullValue(), nullValue()));
            }

            @Test
            @Story("Happy path")
            @DisplayName("Each item's stats object has all required numeric fields")
            @Description("stats must contain: qurilayotgan, kechikishBilanQurilmoqda, qurilibBitkazilgan, kechikishBilanQurilgan, toxtatilganQurilishlar — all >= 0.")
            void getDevelopers_statsHaveRequiredFields() {
                given(requestSpec)
                    .queryParam("size", 5)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.stats.qurilayotgan", everyItem(greaterThanOrEqualTo(0)))
                    .body("items.stats.kechikishBilanQurilmoqda", everyItem(greaterThanOrEqualTo(0)))
                    .body("items.stats.qurilibBitkazilgan", everyItem(greaterThanOrEqualTo(0)))
                    .body("items.stats.kechikishBilanQurilgan", everyItem(greaterThanOrEqualTo(0)))
                    .body("items.stats.toxtatilganQurilishlar", everyItem(greaterThanOrEqualTo(0)));
            }
        }

        @Nested
        class SearchTests {

            @Test
            @Story("Search")
            @DisplayName("Valid q (>= 2 chars) returns 200 with matching results")
            @Description("A query of 2+ characters triggers trigram search and returns 200.")
            void getDevelopers_validQuery_returns200() {
                given(requestSpec)
                    .queryParam("q", "Toshkent")
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items", notNullValue());
            }

            @Test
            @Story("Search")
            @DisplayName("q shorter than 2 chars returns 200 with empty items")
            @Description("A 1-char query is below the minimum — server returns 200 with items=[], DB is not queried.")
            void getDevelopers_singleCharQuery_returnsEmptyItems() {
                given(requestSpec)
                    .queryParam("q", "T")
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items", empty());
            }

            @Test
            @Story("Search")
            @DisplayName("Empty q returns 200 with all items")
            @Description("An empty query string is below the 2-char minimum — returns 200 with all items.")
            void getDevelopers_emptyQuery_returnsEmptyItems() {
                given(requestSpec)
                    .queryParam("q", "")
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items", notNullValue());
            }

            @Test
            @Story("Search")
            @DisplayName("Exactly 2-char query is accepted and returns 200")
            @Description("2 chars is the minimum threshold — must trigger a DB search and return 200.")
            void getDevelopers_twoCharQuery_isAccepted() {
                given(requestSpec)
                    .queryParam("q", "4Y")
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items", notNullValue());
            }
        }

        @Nested
        class PaginationTests {

            @Test
            @Story("Pagination")
            @DisplayName("Default size returns at most 20 items")
            @Description("When ?size is omitted the server defaults to 20 — items.size() must be <= 20.")
            void getDevelopers_defaultSize_returnsAtMost20() {
                given(requestSpec)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.size()", lessThanOrEqualTo(ApiConstants.DEFAULT_PAGE_SIZE));
            }

            @Test
            @Story("Pagination")
            @DisplayName("Custom size is respected")
            @Description("Requesting size=5 must return at most 5 items.")
            void getDevelopers_customSize_isRespected() {
                given(requestSpec)
                    .queryParam("size", 5)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.size()", lessThanOrEqualTo(5));
            }

            @Test
            @Story("Pagination")
            @DisplayName("Maximum size=50 returns at most 50 items")
            @Description("50 is the maximum allowed page size.")
            void getDevelopers_maxSize_returnsAtMost50() {
                given(requestSpec)
                    .queryParam("size", ApiConstants.MAX_PAGE_SIZE)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("items.size()", lessThanOrEqualTo(ApiConstants.MAX_PAGE_SIZE));
            }

            @ParameterizedTest(name = "size={0} → 400")
            @Story("Pagination")
            @DisplayName("Invalid size values return 400")
            @Description("size must be 1–50. Out-of-range values must return 400 Bad Request.")
            @ValueSource(ints = {0, -1, 51, 100})
            void getDevelopers_invalidSize_returns400(int invalidSize) {
                given(requestSpec)
                    .queryParam("size", invalidSize)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .statusCode(400)
                    .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                    .body("status", equalTo(400))
                    .body("type", notNullValue());
            }

            @Test
            @Story("Pagination")
            @DisplayName("nextCursor and hasMore are consistent")
            @Description("hasMore=true must mean nextCursor is not null, and vice versa.")
            void getDevelopers_nextCursorAndHasMoreAreConsistent() {
                Response response = given(requestSpec)
                        .queryParam("size", 1)
                    .when()
                        .get(LIST_ENDPOINT)
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
            @DisplayName("Using nextCursor fetches next page with different items")
            @Description("""
                    Two-step cursor test:
                    1. Fetch page 1 (size=2) → capture nextCursor + IDs.
                    2. Fetch page 2 using cursor → items must differ from page 1.
                    """)
            void getDevelopers_withValidCursor_returnsNextPage() {
                Response firstPage = given(requestSpec)
                        .queryParam("size", 2)
                    .when()
                        .get(LIST_ENDPOINT)
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
                        .get(LIST_ENDPOINT)
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
            void getDevelopers_malformedCursor_returns400() {
                given(requestSpec)
                    .queryParam("cursor", "not-a-valid-cursor-!!@#")
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .statusCode(400)
                    .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
            }
        }

        @Nested
        class CachingTests {

            @Test
            @Story("Caching")
            @DisplayName("Response includes an ETag header")
            @Description("All public GETs must return an ETag header.")
            void getDevelopers_hasETagHeader() {
                given(requestSpec)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .header(ApiConstants.HEADER_ETAG, notNullValue());
            }

            @Test
            @Story("Caching")
            @DisplayName("Matching If-None-Match returns 304")
            @Description("Re-sending with the ETag in If-None-Match must return 304 Not Modified.")
            void getDevelopers_matchingETag_returns304() {
                String etag = given(requestSpec)
                    .when()
                        .get(LIST_ENDPOINT)
                    .then()
                        .statusCode(200)
                        .extract()
                        .header(ApiConstants.HEADER_ETAG);

                given(requestSpec)
                    .header(ApiConstants.HEADER_IF_NONE_MATCH, etag)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .statusCode(304);
            }
        }

        @Nested
        class HeaderTests {

            @Test
            @Story("Rate limiting")
            @DisplayName("Response includes all X-RateLimit-* headers")
            @Description("Server must return X-RateLimit-Limit, X-RateLimit-Remaining and X-RateLimit-Reset.")
            void getDevelopers_hasRateLimitHeaders() {
                given(requestSpec)
                .when()
                    .get(LIST_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .header(ApiConstants.HEADER_RATE_LIMIT_LIMIT, notNullValue())
                    .header(ApiConstants.HEADER_RATE_LIMIT_REMAINING, notNullValue())
                    .header(ApiConstants.HEADER_RATE_LIMIT_RESET, notNullValue());
            }
        }
    }

    // =========================================================================
    //  GET /developers/{id} — Detail
    // =========================================================================

    @Nested
    @DisplayName("GET /developers/{id} — Detail")
    class DetailTests {

        @Nested
        class HappyPathTests {

            @Test
            @Story("Happy path")
            @DisplayName("Known developer ID returns 200 with full detail object")
            @Description("A valid UUID from the list endpoint must return 200 and a complete developer object.")
            void getDeveloperById_knownId_returns200() {
                given(requestSpec)
                    .pathParam("id", dynamicDeveloperId)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("id", equalTo(dynamicDeveloperId))
                    .body("name", notNullValue())
                    .body("stats", notNullValue());
            }

            @Test
            @Story("Happy path")
            @DisplayName("Detail response includes all required fields")
            @Description("Detail object must contain: id, name, legalName, stats.")
            void getDeveloperById_hasAllRequiredFields() {
                given(requestSpec)
                    .pathParam("id", dynamicDeveloperId)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("name", notNullValue())
                    .body("legalName", notNullValue())
                    .body("stats", notNullValue());
            }

            @Test
            @Story("Happy path")
            @DisplayName("Detail stats object has all required non-negative numeric fields")
            @Description("All 5 stats counters must be present and >= 0.")
            void getDeveloperById_statsHaveAllFields() {
                given(requestSpec)
                    .pathParam("id", dynamicDeveloperId)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("stats.qurilayotgan", greaterThanOrEqualTo(0))
                    .body("stats.kechikishBilanQurilmoqda", greaterThanOrEqualTo(0))
                    .body("stats.qurilibBitkazilgan", greaterThanOrEqualTo(0))
                    .body("stats.kechikishBilanQurilgan", greaterThanOrEqualTo(0))
                    .body("stats.toxtatilganQurilishlar", greaterThanOrEqualTo(0));
            }

            @Test
            @Story("Happy path")
            @DisplayName("Returned id matches the requested id")
            @Description("The id in the response body must exactly match the id used in the path.")
            void getDeveloperById_returnedIdMatchesRequest() {
                String returnedId = given(requestSpec)
                        .pathParam("id", ApiConstants.KNOWN_DEVELOPER_ID)
                    .when()
                        .get(DETAIL_ENDPOINT)
                    .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getString("id");

                assertThat(returnedId)
                        .as("Response id must match the requested id")
                        .isEqualTo(ApiConstants.KNOWN_DEVELOPER_ID);
            }
        }

        @Nested
        class NegativeTests {

            @Test
            @Story("Negative")
            @DisplayName("Non-existent UUID returns 404")
            @Description("A valid UUID that does not exist in the database must return 404 Not Found.")
            void getDeveloperById_nonExistentId_returns404() {
                given(requestSpec)
                    .pathParam("id", ApiConstants.NON_EXISTENT_UUID)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .statusCode(404)
                    .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                    .body("status", equalTo(404))
                    .body("type", notNullValue());
            }

            @Test
            @Story("Negative")
            @DisplayName("Malformed UUID returns 400")
            @Description("A path param that is not a valid UUID format must return 400 Bad Request.")
            void getDeveloperById_malformedId_returns400() {
                given(requestSpec)
                    .pathParam("id", ApiConstants.MALFORMED_ID)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .statusCode(400)
                    .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                    .body("status", equalTo(400));
            }
        }

        @Nested
        class CachingTests {

            @Test
            @Story("Caching")
            @DisplayName("Detail response includes an ETag header")
            @Description("All public GETs must return an ETag header.")
            void getDeveloperById_hasETagHeader() {
                given(requestSpec)
                    .pathParam("id", dynamicDeveloperId)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .header(ApiConstants.HEADER_ETAG, notNullValue());
            }

            @Test
            @Story("Caching")
            @DisplayName("Matching If-None-Match on detail returns 304")
            @Description("Re-sending with the ETag in If-None-Match must return 304 Not Modified.")
            void getDeveloperById_matchingETag_returns304() {
                String etag = given(requestSpec)
                        .pathParam("id", dynamicDeveloperId)
                    .when()
                        .get(DETAIL_ENDPOINT)
                    .then()
                        .statusCode(200)
                        .extract()
                        .header(ApiConstants.HEADER_ETAG);

                given(requestSpec)
                    .pathParam("id", dynamicDeveloperId)
                    .header(ApiConstants.HEADER_IF_NONE_MATCH, etag)
                .when()
                    .get(DETAIL_ENDPOINT)
                .then()
                    .statusCode(304);
            }
        }
    }
}
