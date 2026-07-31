package com.agentic.urlshortener.e2e;

import com.agentic.urlshortener.domain.ShortUrl;
import com.agentic.urlshortener.repository.ClickAggregateRepository;
import com.agentic.urlshortener.repository.ShortUrlRepository;
import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    }
)
class UrlShortenerE2ETest {

    @LocalServerPort
    int port;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickAggregateRepository clickAggregateRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        clickAggregateRepository.deleteAll();
        shortUrlRepository.deleteAll();
    }

    @Test
    void shouldCreateRedirectAndReturnAnalytics() {
        Map<String, String> request = new HashMap<>();
        request.put("destinationUrl", "https://example.com/docs?q=1");

        String code = given()
            .contentType("application/json")
            .body(request)
            .when()
            .post("/api/v1/urls")
            .then()
            .statusCode(201)
            .body("code", notNullValue())
            .body("shortUrl", containsString("/r/"))
            .body("destinationUrl", equalTo("https://example.com/docs?q=1"))
            .extract()
            .path("code");

        given()
            .config(RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
            .get("/r/{code}", code)
            .then()
            .statusCode(302)
            .header("Location", "https://example.com/docs?q=1");

        given()
            .when()
            .get("/api/v1/urls/{code}/analytics", code)
            .then()
            .statusCode(200)
            .body("code", equalTo(code))
            .body("totalClicks", equalTo(1));
    }

    @Test
    void shouldRejectInvalidDestinationUrl() {
        given()
            .contentType("application/json")
            .body(Map.of("destinationUrl", "ftp://example.com"))
            .when()
            .post("/api/v1/urls")
            .then()
            .statusCode(400);
    }

    @Test
    void shouldReturn404ForUnknownCode() {
        given()
            .when()
            .get("/r/does-not-exist")
            .then()
            .statusCode(404);

        given()
            .when()
            .get("/api/v1/urls/does-not-exist/analytics")
            .then()
            .statusCode(404);
    }
}