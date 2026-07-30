package com.agentic.urlshortener.controller;

import com.agentic.urlshortener.domain.ShortUrl;
import com.agentic.urlshortener.repository.ClickAggregateRepository;
import com.agentic.urlshortener.repository.ShortUrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickAggregateRepository clickAggregateRepository;

    @BeforeEach
    void cleanDatabase() {
        clickAggregateRepository.deleteAll();
        shortUrlRepository.deleteAll();
    }

    @Test
    void createShortUrl_shouldCreateAndReturnResourceForValidUrl() throws Exception {
        var request = Map.of("destinationUrl", "https://example.com/path?q=1");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.containsString("/r/")))
            .andExpect(jsonPath("$.destinationUrl").value("https://example.com/path?q=1"));

        assertThat(shortUrlRepository.findAll()).hasSize(1);
    }

    @Test
    void createShortUrl_shouldRejectInvalidDestinationUrl() throws Exception {
        var request = Map.of("destinationUrl", "ftp://example.com");

        mockMvc.perform(post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        assertThat(shortUrlRepository.findAll()).isEmpty();
    }

    @Test
    void redirect_shouldReturnLocationAndRecordClick() throws Exception {
        ShortUrl shortUrl = shortUrlRepository.save(new ShortUrl("abc123", "https://example.com", Instant.now(), null));

        mockMvc.perform(get("/r/{code}", shortUrl.getCode()))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com"));

        var analytics = clickAggregateRepository.totalFor(shortUrl);
        assertThat(analytics).isEqualTo(1L);
    }

    @Test
    void analytics_shouldExposeAggregatedClickCount() throws Exception {
        ShortUrl shortUrl = shortUrlRepository.save(new ShortUrl("abc123", "https://example.com", Instant.now(), null));
        mockMvc.perform(get("/r/{code}", shortUrl.getCode())).andExpect(status().isFound());

        mockMvc.perform(get("/api/v1/urls/{code}/analytics", shortUrl.getCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(shortUrl.getCode()))
            .andExpect(jsonPath("$.totalClicks").value(1));
    }
}
