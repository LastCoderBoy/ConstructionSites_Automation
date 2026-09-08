package com.jk.qurilishnazorat.common;

/**
 * Central shared constants for the Novostroyka API automation suite.
 *
 * <p>All constants that may need to change per environment (base URL,
 * test data IDs, timeouts) live here — update this one file instead of
 * hunting across test classes.
 *
 * <p>Values can be overridden at runtime via system properties, e.g.:
 * <pre>
 *   mvn test -Dapi.baseUrl=https://other-env-api.tashkent.uz
 * </pre>
 */
public final class ApiConstants {

    private ApiConstants() {
        // Utility class — no instantiation
    }


    /** Rate-limit headers returned by the server on every response. */
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET  = "X-RateLimit-Reset";

    /** ETag / conditional request headers. */
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_IF_NONE_MATCH  = "If-None-Match";

    // =========================================================================
    //  Content types
    // =========================================================================

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_PROBLEM_JSON = "application/problem+json";

    // =========================================================================
    //  Timeouts  (milliseconds)
    // =========================================================================

    /** Maximum time to wait for a connection to be established. */
    public static final int CONNECT_TIMEOUT_MS = 10_000;

    /** Maximum time to wait for a response after the request is sent. */
    public static final int READ_TIMEOUT_MS = 30_000;

    // =========================================================================
    //  Pagination defaults (mirrors server defaults)
    // =========================================================================

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    // =========================================================================
    //  Stable test data IDs  (from the seeded dev database)
    // TODO: Replace IDs with a real ID once prod database is ready.
    // =========================================================================
    public static final String KNOWN_PROJECT_ID = "f5d9758e-7234-3ad0-8e37-1bc4030a2015";
    public static final String KNOWN_PROJECT_ID_2 = "f5b27e52-4937-3cba-8f41-49f756f6d5e3";
    public static final String KNOWN_DEVELOPER_ID = "cb7f1f24-b939-3b02-a0de-dc46e7481a38";
    public static final String KNOWN_SALES_OFFICE_ID = "b245f3e7-f945-4653-b470-220d0cc7c6c6";
    public static final String KNOWN_MEDIA_ID = "REPLACE_WITH_REAL_MEDIA_ID";
    public static final int KNOWN_DISTRICT_ID = 101;

    /**
     * A valid document type code from /refs/document-types
     * (e.g. "APZ_PERMIT") — used in /projects/{id}/documents/{code}/download.
     */
    public static final String KNOWN_DOCUMENT_TYPE_CODE = "APZ_PERMIT";

    /**
     * Tashkent city-centre coordinates used in geo / nearby tests.
     * These coordinates are well within valid ranges and should return results.
     */
    public static final double TASHKENT_LAT = 41.31065534299677;
    public static final double TASHKENT_LNG = 69.24794384882865;
    public static final double TASHKENT_RADIUS_M = 5612;

    // =========================================================================
    //  Non-existent / invalid IDs  (for negative / 404 tests)
    // =========================================================================

    /** A well-formed UUID that is guaranteed not to exist in the database. */
    public static final String NON_EXISTENT_UUID = "00000000-0000-0000-0000-000000000000";

    /** A completely malformed ID — used to trigger 400 responses. */
    public static final String MALFORMED_ID = "not-a-valid-uuid-!!";
}
