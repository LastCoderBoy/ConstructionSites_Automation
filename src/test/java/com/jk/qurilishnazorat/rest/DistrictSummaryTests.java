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

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Feature("Districts")
@DisplayName("GET /districts/summary — Tumanlar bo'yicha obyektlar soni")
class DistrictSummaryTests extends BaseTest {

    private static final String ENDPOINT = "/districts/summary";

    // =========================================================================
    //  Happy Path
    // =========================================================================

    @Nested
    class HappyPathTests {

        @Test
        @Story("Happy path")
        @DisplayName("Returns 200 with a non-empty items array")
        @Description("A plain request must return 200 and a non-empty items array — districts always exist.")
        void getDistrictSummary_returns200WithItems() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThan(0));
        }

        @Test
        @Story("Happy path")
        @DisplayName("Each district item has all required fields")
        @Description("Every item must contain: id, name, totalCount, muammosizCount, qismanMuammoliCount, muammoliCount, noqonuniyCount.")
        void getDistrictSummary_itemsHaveRequiredFields() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.id", everyItem(notNullValue()))
                .body("items.name", everyItem(notNullValue()))
                .body("items.totalCount", everyItem(notNullValue()))
                .body("items.muammosizCount", everyItem(notNullValue()))
                .body("items.qismanMuammoliCount", everyItem(notNullValue()))
                .body("items.muammoliCount", everyItem(notNullValue()))
                .body("items.noqonuniyCount", everyItem(notNullValue()));
        }

        @Test
        @Story("Happy path")
        @DisplayName("All count fields are non-negative integers")
        @Description("All 5 counter fields (totalCount + 4 sub-counts) must be >= 0 for every district.")
        void getDistrictSummary_allCountsAreNonNegative() {
            given(requestSpec)
            .when()
                .get(ENDPOINT)
            .then()
                .spec(responseSpec)
                .statusCode(200)
                .body("items.totalCount", everyItem(greaterThanOrEqualTo(0)))
                .body("items.muammosizCount", everyItem(greaterThanOrEqualTo(0)))
                .body("items.qismanMuammoliCount", everyItem(greaterThanOrEqualTo(0)))
                .body("items.muammoliCount", everyItem(greaterThanOrEqualTo(0)))
                .body("items.noqonuniyCount", everyItem(greaterThanOrEqualTo(0)));
        }

    }

    // =========================================================================
    //  Data Integrity
    // =========================================================================

    @Nested
    class DataIntegrityTests {

        @Test
        @Story("Data integrity")
        @DisplayName("The 4 sub-counts sum exactly to totalCount for every district")
        @Description("""
                API contract: muammosizCount + qismanMuammoliCount + muammoliCount + noqonuniyCount = totalCount.
                The 4 counters are mutually exclusive and exhaustive.
                """)
        void getDistrictSummary_subCountsSumToTotalCount() {
            Response response = given(requestSpec)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract().response();

            List<Integer> totalCounts = response.jsonPath().getList("items.totalCount", Integer.class);
            List<Integer> muammosiz = response.jsonPath().getList("items.muammosizCount", Integer.class);
            List<Integer> qismanMuammoli = response.jsonPath().getList("items.qismanMuammoliCount", Integer.class);
            List<Integer> muammoli = response.jsonPath().getList("items.muammoliCount", Integer.class);
            List<Integer> noqonuniy = response.jsonPath().getList("items.noqonuniyCount", Integer.class);
            List<String> names = response.jsonPath().getList("items.name", String.class);

            for (int i = 0; i < totalCounts.size(); i++) {
                int subSum = muammosiz.get(i)
                        + qismanMuammoli.get(i)
                        + muammoli.get(i)
                        + noqonuniy.get(i);

                assertThat(subSum)
                        .as("District '%s': muammosizCount(%d) + qismanMuammoliCount(%d) + muammoliCount(%d) + noqonuniyCount(%d) = %d must equal totalCount(%d)",
                                names.get(i),
                                muammosiz.get(i), qismanMuammoli.get(i),
                                muammoli.get(i), noqonuniy.get(i),
                                subSum, totalCounts.get(i))
                        .isEqualTo(totalCounts.get(i));
            }
        }

        @Test
        @Story("Data integrity")
        @DisplayName("Each district id is unique")
        @Description("No two districts in the response should share the same id.")
        void getDistrictSummary_districtIdsAreUnique() {
            List<Integer> ids = given(requestSpec)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("items.id", Integer.class);

            assertThat(ids)
                    .as("District IDs must all be unique")
                    .doesNotHaveDuplicates();
        }

        @Test
        @Story("Data integrity")
        @DisplayName("Each district name is unique and non-blank")
        @Description("No two districts should share the same name and no name may be blank.")
        void getDistrictSummary_districtNamesAreUniqueAndNonBlank() {
            List<String> names = given(requestSpec)
                .when()
                    .get(ENDPOINT)
                .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("items.name", String.class);

            assertThat(names)
                    .as("District names must all be unique")
                    .doesNotHaveDuplicates();

            names.forEach(name ->
                assertThat(name)
                        .as("District name must not be blank")
                        .isNotBlank()
            );
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
        void getDistrictSummary_hasETagHeader() {
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
        @DisplayName("Matching If-None-Match returns 304")
        @Description("Re-sending with the ETag in If-None-Match must return 304 Not Modified.")
        void getDistrictSummary_matchingETag_returns304() {
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
        void getDistrictSummary_hasRateLimitHeaders() {
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
