package com.jk.qurilishnazorat.rest;

import com.jk.qurilishnazorat.base.BaseTest;
import com.jk.qurilishnazorat.common.ApiConstants;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Feature("Search")
@DisplayName("GET /search/suggest — Autocomplete")
class SearchTests extends BaseTest {

    private static final String ENDPOINT = "/search/suggest";

    @Nested
    class HappyPathTests {

        @Test
        @Story("Happy path")
        @DisplayName("Valid query (>= 2 chars) returns 200 with items array")
        @Description("A query of 2+ characters should return HTTP 200 and a non-null 'items' array (may be empty).")
        void validQuery_returns200WithItemsArray() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(0));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Each suggestion has required fields: type, id, label")
        @Description("Every item in the response must contain 'type', 'id', and 'label' fields.")
        void suggestions_haveRequiredFields() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.type", everyItem(notNullValue()))
                .body("items.id", everyItem(notNullValue()))
                .body("items.label", everyItem(notNullValue()));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Suggestion 'type' values are only DEVELOPER, DEVELOPER_PROJECT or PROJECT")
        @Description("The 'type' field must be one of the 3 documented enum values.")
        void suggestions_typeIsOneOfAllowedValues() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.type", everyItem(oneOf("DEVELOPER", "DEVELOPER_PROJECT", "PROJECT")));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Missing 'q' parameter returns 200")
        @Description("'q' is optional — omitting it should return 200 OK with items array.")
        void missingQParam_returns200() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(200)
                .body("items", notNullValue());
        }

        @ParameterizedTest(name = "query=''{0}'' → 200 with items array")
        @Story("Happy path")
        @DisplayName("Cyrillic and mixed-case queries are handled correctly")
        @ValueSource(strings = {"Тошкент", "toshkent", "TOSHKENT", "Yunusobod"})
        void cyrillicAndCaseVariants_return200(String query) {
            given(requestSpec)
                .queryParam("q", query)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", notNullValue());
        }
    }

    @Nested
    class BoundaryTests {

        @Test
        @Story("Boundary")
        @DisplayName("Empty query string returns 200 with empty items array")
        @Description("An empty 'q' is shorter than the 2-char minimum — server must return 200 with items=[].")
        void emptyQuery_returns200WithEmptyItems() {
            given(requestSpec)
                .queryParam("q", "")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", empty());
        }

        @Test
        @Story("Boundary")
        @DisplayName("Single-character query returns 200 with empty items")
        @Description("A 1-char 'q' is below the minimum — server returns 200 with items=[], DB is not queried.")
        void singleCharQuery_returns200WithEmptyItems() {
            given(requestSpec)
                .queryParam("q", "T")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", empty());
        }

        @Test
        @Story("Boundary")
        @DisplayName("Exactly 2-character query is accepted and returns 200")
        @Description("2 chars is exactly the minimum — must hit the DB and return a valid 200 response.")
        void twoCharQuery_isAcceptedAndReturns200() {
            given(requestSpec)
                .queryParam("q", "To")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", notNullValue());
        }

        @Test
        @Story("Boundary")
        @DisplayName("Query over 100 chars is truncated silently — returns 200, NOT 400")
        @Description("API docs state: queries over 100 chars are silently truncated, not rejected with 400.")
        void queryOver100Chars_isTruncatedNotRejected() {
            String longQuery = "T".repeat(120);
            given(requestSpec)
                .queryParam("q", longQuery)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200);
        }

        @Test
        @Story("Boundary")
        @DisplayName("Valid query returns at most 10 suggestions")
        @Description("The API must never return more than 10 results regardless of how many match.")
        void validQuery_returnsAtMost10Results() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.size()", lessThanOrEqualTo(10));
        }
    }

    @Nested
    class CachingTests {

        @Test
        @Story("Caching")
        @DisplayName("Response includes an ETag header")
        @Description("All public GETs must return an ETag header for conditional request support.")
        void response_hasETagHeader() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
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
        void matchingETag_returns304() {
            String etag = given(requestSpec)
                    .queryParam("q", "Toshkent")
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .header(ApiConstants.HEADER_ETAG);

            given(requestSpec)
                .queryParam("q", "Toshkent")
                .header(ApiConstants.HEADER_IF_NONE_MATCH, etag)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(304);
        }
    }

    @Nested
    class HeaderTests {

        @Test
        @Story("Rate limiting")
        @DisplayName("Response includes all X-RateLimit-* headers")
        @Description("Server must return X-RateLimit-Limit, X-RateLimit-Remaining and X-RateLimit-Reset on every response.")
        void response_hasRateLimitHeaders() {
            given(requestSpec)
                .queryParam("q", "Toshkent")
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
