package com.jk.qurilishnazorat.spec;

import com.jk.qurilishnazorat.common.ApiConstants;
import com.jk.qurilishnazorat.config.ConfigReader;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static org.hamcrest.Matchers.lessThan;

public class SpecFactory {

    /** Maximum acceptable response time in milliseconds for all assertions. */
    private static final long RESPONSE_TIME_THRESHOLD_MS = 5_000L;

    /**
     * Shared {@link RequestSpecification} pre-loaded with:
     * <ul>
     *   <li>Base URI ({@link ConfigReader#getBaseUrl()})</li>
     *   <li>Base path ({@link ConfigReader#get(String)})</li>
     *   <li>Accept: application/json</li>
     *   <li>Connection &amp; read timeouts</li>
     *   <li>Allure filter for report attachments</li>
     *   <li>Request logging on failure</li>
     * </ul>
     *
     * Test classes may build on top of this using
     * {@code given(requestSpec).header(...).queryParam(...)...}
     */
    public static RequestSpecification requestSpec(){
        return new RequestSpecBuilder()
                .setBaseUri(ConfigReader.getBaseUrl())
                .setBasePath(ConfigReader.get("api.version.url"))

                // Accept JSON by default (individual tests may override for binary downloads)
                .setAccept(ContentType.JSON)

                // Explicitly set Accept header as a plain string — avoids REST Assured's
                // default multi-value header (application/json, application/javascript, ...)
                // that ContentType.JSON alone sometimes doesn't fully override
                .addHeader("Accept", ApiConstants.CONTENT_TYPE_JSON)

                // Timeouts — using correct Apache HttpClient 4 parameter names
                .setConfig(RestAssured
                        .config()
                        .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", ApiConstants.CONNECT_TIMEOUT_MS)
                                .setParam("http.socket.timeout", ApiConstants.READ_TIMEOUT_MS)
                        )
                )

                // Allure: automatically attach request + response details to the HTML report
                .addFilter(new AllureRestAssured())

                .log(LogDetail.ALL)

                .build();
    }

    /**
     * Shared {@link ResponseSpecification} that asserts:
     * <ul>
     *   <li>Content-Type is {@code application/json}</li>
     *   <li>Response time is under {@value RESPONSE_TIME_THRESHOLD_MS} ms</li>
     * </ul>
     *
     * Test classes add their own status code and body assertions on top:
     * {@code .then().spec(responseSpec).statusCode(200).body("items", not(empty()))}
     */
    public static ResponseSpecification responseSpec(){
        return new ResponseSpecBuilder()
                // All successful API responses must be JSON
                .expectContentType(ContentType.JSON)

                // Enforce a reasonable response-time SLA
                .expectResponseTime(lessThan(RESPONSE_TIME_THRESHOLD_MS))

                .build();
    }

}
