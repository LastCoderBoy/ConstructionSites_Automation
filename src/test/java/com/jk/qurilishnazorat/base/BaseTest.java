package com.jk.qurilishnazorat.base;

import com.jk.qurilishnazorat.common.ApiConstants;
import com.jk.qurilishnazorat.config.ConfigReader;
import com.jk.qurilishnazorat.spec.SpecFactory;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {

    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    /**
     * Configures REST Assured defaults once before any test in the suite runs.
     *
     * <p>{@code @BeforeAll} combined with the static fields means every subclass
     * inherits the same fully-configured spec without repeating boilerplate.
     */
    @BeforeAll
    static void setUpRestAssured() {
        requestSpec = SpecFactory.requestSpec();
        responseSpec = SpecFactory.responseSpec();

        // ------------------------------------------------------------------
        // Global REST Assured defaults (fallback for any ad-hoc given()...when()...then())
        // ------------------------------------------------------------------
        RestAssured.baseURI  = ConfigReader.getBaseUrl();
        RestAssured.basePath = ConfigReader.get("api.version.url");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
