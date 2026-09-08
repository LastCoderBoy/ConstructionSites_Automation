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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.*;

@Feature("Close Construction Sites")
@DisplayName("GET /geo/nearby — Yaqin Qurilishlar")
public class YaqinQurilishlarTests extends BaseTest {

    private static final String ENDPOINT = "/geo/nearby";
    private static final String DEFAULT_LATITUDE  = "41.31065534299677";
    private static final String DEFAULT_LONGITUDE = "69.24794384882865";

    @Nested
    class HappyPathTests {

        @Test
        @Story("Happy path")
        @Description("Valid coordinates should return HTTP 200, items array, hasMore flag, total count, and the echoed radiusM.")
        @DisplayName("Valid coordinates return 200 with well-formed response body")
        void shouldReturn200WithValidCoordinates() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                // 'items' is always present (empty array is fine)
                .body("items",   notNullValue())
                // items.size() is bounded by the default limit (20), NOT total
                .body("items.size()", lessThanOrEqualTo(20))
                // total reflects all matching buildings in radius — can be > limit
                .body("total",   notNullValue())
                // hasMore must always be present
                .body("hasMore", notNullValue())
                // server echoes back the radius it used (default = 5000m)
                .body("radiusM", equalTo(5000));
        }

        @Test
        @Story("Happy path")
        @Description("When a valid radiusM is provided, server should echo it back and return 200.")
        @DisplayName("Custom radiusM is echoed back in the response")
        void shouldReturn200_WithValidRadius() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("radiusM", 10000)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.size()", lessThanOrEqualTo(20))
                .body("radiusM", equalTo(10000));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Each item has all required fields")
        @Description("Every item must contain id, name, lat, lng, issueLevel and distanceM.")
        void shouldReturn200_itemsHaveRequiredFields() {
            Response response = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .extract().response();

            List<Object> items = response.jsonPath().getList("items");
            if (items == null || items.isEmpty()) return; // no data in radius — skip field check

            response.then()
                    .body("items.id", everyItem(notNullValue()))
                    .body("items.name", everyItem(notNullValue()))
                    .body("items.issueLevel", everyItem(notNullValue()))
                    .body("items.distanceM", everyItem(notNullValue()));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Items are sorted by distanceM ascending (nearest first)")
        @Description("API contract: results ordered nearest → farthest. distanceM must be non-decreasing.")
        void shouldReturn200_itemsSortedByDistanceAscending() {
            List<Float> distances = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .queryParam("limit",20)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("items.distanceM", Float.class);

            for (int i = 0; i < distances.size() - 1; i++) {
                assertThat(distances.get(i))
                        .as("items[%d].distanceM (%s) must be <= items[%d].distanceM (%s)",
                                i, distances.get(i), i + 1, distances.get(i + 1))
                        .isLessThanOrEqualTo(distances.get(i + 1));
            }
        }

        @Story("Happy path")
        @DisplayName("Valid issueLevel filter returns 200 and all items match")
        @Description("When a valid issueLevel is provided, all returned items must match that level.")
        @ParameterizedTest(name = "issueLevel={0} → 200, items match")
        @ValueSource(strings = {"NO_ISSUES", "HAS_ISSUES", "PARTIAL_ISSUES"})
        void getCloseConstructions_ValidIssueLevel_returns200(String issueLevel) {
            Response response = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .queryParam("issueLevel", issueLevel)
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .extract().response();

            Set<String> issueLevels = new HashSet<>(
                    response.jsonPath().getList("items.issueLevel", String.class));

            if (!issueLevels.isEmpty()) {
                assertThat(issueLevels).containsOnly(issueLevel);
            }
            // default radiusM must be echoed when not explicitly provided
            assertThat(response.jsonPath().getInt("radiusM")).isEqualTo(5000);
        }

        @Story("Happy path")
        @DisplayName("Valid legalStatus filter returns 200 and all items match")
        @Description("When a valid legalStatus is provided, all returned items must match that status.")
        @ParameterizedTest(name = "legalStatus={0} → 200, items match")
        @ValueSource(strings = {"LEGAL", "UNDER_REVIEW", "IN_COURT", "COURT_RULED_ILLEGAL",
                                "DEMOLITION_ORDERED", "LEGALIZED", "DEMOLISHED"})
        void getCloseConstructions_ValidLegalStatuses_returns200(String legalStatus) {
            Response response = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .queryParam("legalStatus", legalStatus)
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .extract().response();

            Set<String> legalStatuses = new HashSet<>(
                    response.jsonPath().getList("items.legalStatus", String.class));

            if (!legalStatuses.isEmpty()) {
                assertThat(legalStatuses).containsOnly(legalStatus);
            }
            assertThat(response.jsonPath().getInt("radiusM")).isEqualTo(5000);
        }

        @Test
        @Story("Happy path")
        @DisplayName("Combined issueLevel + legalStatus filters return 200 and all items match both")
        @Description("Both filters applied together — every item must satisfy both constraints simultaneously.")
        void getCloseConstructions_CombinedFilters_returns200AndItemsMatchBoth() {
            Response response = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .queryParam("issueLevel", "NO_ISSUES")
                    .queryParam("legalStatus", "LEGAL")
                .when()
                    .get(ENDPOINT)
                .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .extract().response();

            List<String> issueLevels  = response.jsonPath().getList("items.issueLevel",  String.class);
            List<String> legalStatuses = response.jsonPath().getList("items.legalStatus", String.class);

            if (issueLevels != null && !issueLevels.isEmpty()) {
                assertThat(issueLevels).containsOnly("NO_ISSUES");
            }
            if (legalStatuses != null && !legalStatuses.isEmpty()) {
                assertThat(legalStatuses).containsOnly("LEGAL");
            }
        }
    }

    @Nested
    class BoundaryCases {

        @Story("Boundary cases")
        @Description("Edge values for limit (1 and 50) must be accepted and items.size() must not exceed the limit.")
        @DisplayName("Edge limit values (1, 50) return 200 and items.size() <= limit")
        @ParameterizedTest(name = "limit={0} → 200, items.size() <= {0}")
        @ValueSource(ints = {1, 50})
        void shouldReturn200_WhenLimitIsEdgeValue(int limit) {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("limit", limit)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                // items.size() must respect the limit — NOT total
                .body("items.size()", lessThanOrEqualTo(limit));
        }

        @Story("Boundary cases")
        @Description("Edge values for radiusM (100 and 50000) must be accepted and echoed back in the response.")
        @DisplayName("Edge radiusM values (100, 50000) return 200 and radiusM is echoed")
        @ParameterizedTest(name = "radiusM={0} → 200, radiusM echoed")
        @ValueSource(ints = {100, 50000})
        void getCloseConstructions_EdgeRadiusValues_returns200(int radius) {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("radiusM", radius)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                // server must echo back exactly the radiusM we sent
                .body("radiusM", equalTo(radius));
        }

        @Story("Boundary cases")
        @DisplayName("hasMore=true only when items.size() == limit and total > limit")
        @Description("If fewer items than limit are returned, hasMore must be false.")
        @Test
        void getCloseConstructions_hasMoreFalse_WhenFewerItemsThanLimit() {
            Response response = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .queryParam("limit", 50) // max limit — unlikely to have more
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract().response();

            int itemCount = response.jsonPath().getList("items").size();
            int total = response.jsonPath().getInt("total");
            boolean hasMore = response.jsonPath().getBoolean("hasMore");

            if (itemCount < 50) {
                assertThat(hasMore)
                        .as("hasMore must be false when items returned (%d) < limit (50)", itemCount)
                        .isFalse();
            }
            // total must always equal the real count in the radius
            assertThat(total).isGreaterThanOrEqualTo(itemCount);
        }
    }

    @Nested
    @Story("Negative Path")
    class NegativeTests {

        @Test
        @Description("lat is required. Omitting it should return 400 Bad Request.")
        @DisplayName("Missing lat returns 400")
        void getCloseConstructions_MissingLat_returns400() {
            given(requestSpec)
                .queryParam("lng", DEFAULT_LONGITUDE)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }

        @Test
        @Description("lng is required. Omitting it should return 400 Bad Request.")
        @DisplayName("Missing lng returns 400")
        void getCloseConstructions_MissingLng_returns400() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }

        @Test
        @Description("Both lat and lng are required. Omitting both should return 400.")
        @DisplayName("Missing both lat and lng returns 400")
        void getCloseConstructions_MissingBothCoordinates_returns400() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }

        @DisplayName("Out-of-range coordinates return 400")
        @Description("lat must be -90..90 and lng must be -180..180. Values outside these ranges must return 400.")
        @ParameterizedTest(name = "lat={0}, lng={1} → 400")
        @CsvSource({
                "999,  69.24",    // lat too high
                "-999, 69.24",    // lat too low
                "41.29, 999",     // lng too high
                "41.29, -999"     // lng too low
        })
        void getCloseConstructions_InvalidCoordinates_returns400(String lat, String lng) {
            given(requestSpec)
                .queryParam("lat", lat)
                .queryParam("lng", lng)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }

        @DisplayName("radiusM out of range returns 400")
        @Description("radiusM must be 100–50000. Values outside this range must return 400 — use RFC 9457 structure, not exact message text.")
        @ParameterizedTest(name = "radiusM={0} → 400")
        @ValueSource(ints = {-100, 0, 99, 50001})
        void getCloseConstructions_InvalidRadius_returns400(int invalidRadius) {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("radiusM", invalidRadius)
            .when()
                .get(ENDPOINT)
            .then()
                // assert RFC 9457 structure - not the exact Uzbek message text (fragile)
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                .body("status", equalTo(400))
                .body("type",   notNullValue());
        }

        @DisplayName("limit out of range returns 400")
        @Description("limit must be 1–50. Values outside this range must return 400.")
        @ParameterizedTest(name = "limit={0} → 400")
        @ValueSource(ints = {-1, 0, 51, 100})
        void getCloseConstructions_InvalidLimit_returns400(int invalidLimit) {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("limit", invalidLimit)
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON)
                .body("status", equalTo(400))
                .body("type", notNullValue());
        }

        @Test
        @DisplayName("Invalid issueLevel returns 400")
        @Description("An unrecognised enum value for issueLevel should return 400 Bad Request.")
        void getCloseConstructions_InvalidIssueLevel_returns400() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("issueLevel", "INVALID_LEVEL")
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }

        @Test
        @DisplayName("Invalid legalStatus returns 400")
        @Description("An unrecognised enum value for legalStatus should return 400 Bad Request.")
        void getCloseConstructions_InvalidLegalStatus_returns400() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
                .queryParam("legalStatus", "INVALID_STATUS")
            .when()
                .get(ENDPOINT)
            .then()
                .statusCode(400)
                .contentType(ApiConstants.CONTENT_TYPE_PROBLEM_JSON);
        }
    }

    @Nested
    @Story("Caching")
    class CachingTests{

        @Test
        @Description("A valid request must include an ETag header in the response.")
        @DisplayName("Response includes an ETag header")
        void getCloseConstructions_responseHasETagHeader() {
            given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .when()
                    .get(ENDPOINT)
                    .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .header(ApiConstants.HEADER_ETAG, notNullValue());
        }

        @Test
        @Description("Re-sending with If-None-Match set to the previous ETag must return 304 Not Modified.")
        @DisplayName("Matching If-None-Match returns 304")
        void getCloseConstructions_matchingETag_returns304() {
            String eTag = given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .when()
                    .get(ENDPOINT)
                    .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .extract()
                    .header(ApiConstants.HEADER_ETAG);

            given(requestSpec)
                    .queryParam("lat", DEFAULT_LATITUDE)
                    .queryParam("lng", DEFAULT_LONGITUDE)
                    .header(ApiConstants.HEADER_IF_NONE_MATCH, eTag)
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
        @Description("Server must return X-RateLimit-Limit, X-RateLimit-Remaining and X-RateLimit-Reset.")
        void getCloseConstructions_hasRateLimitHeaders() {
            given(requestSpec)
                .queryParam("lat", DEFAULT_LATITUDE)
                .queryParam("lng", DEFAULT_LONGITUDE)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .header(ApiConstants.HEADER_RATE_LIMIT_LIMIT,     notNullValue())
                .header(ApiConstants.HEADER_RATE_LIMIT_REMAINING, notNullValue())
                .header(ApiConstants.HEADER_RATE_LIMIT_RESET,     notNullValue());
        }
    }
}
