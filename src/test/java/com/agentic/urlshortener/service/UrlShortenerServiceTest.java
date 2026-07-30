package com.agentic.urlshortener.service;

import com.agentic.urlshortener.config.AppProperties;
import com.agentic.urlshortener.domain.ClickAggregate;
import com.agentic.urlshortener.domain.ShortUrl;
import com.agentic.urlshortener.repository.ClickAggregateRepository;
import com.agentic.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UrlShortenerServiceTest {

    private ShortUrlRepository shortUrlRepo;
    private ClickAggregateRepository clickRepo;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        shortUrlRepo = mock(ShortUrlRepository.class);
        clickRepo = mock(ClickAggregateRepository.class);
        AppProperties props = new AppProperties("http://localhost:8080");
        service = new UrlShortenerService(shortUrlRepo, clickRepo, props);

        when(shortUrlRepo.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // T-02: scheme validation
    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "file:///etc/passwd", "javascript:alert(1)", "not-a-url"})
    void create_shouldRejectNonHttpSchemes(String url) {
        assertThatThrownBy(() -> service.create(url, null))
            .isInstanceOf(IllegalArgumentException.class);
        verify(shortUrlRepo, never()).save(any());
    }

    // T-02: oversized URL
    @Test
    void create_shouldRejectUrlExceeding2048Chars() {
        String oversized = "https://example.com/" + "x".repeat(2048);
        assertThatThrownBy(() -> service.create(oversized, null))
            .isInstanceOf(IllegalArgumentException.class);
        verify(shortUrlRepo, never()).save(any());
    }

    // T-02: no host
    @Test
    void create_shouldRejectUrlWithNoHost() {
        assertThatThrownBy(() -> service.create("https:///no-host", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // T-03: code collision retry produces a unique code
    @Test
    void create_shouldRetryOnCodeCollision() {
        ShortUrl existing = new ShortUrl("collision", "https://existing.com", Instant.now(), null);
        // first findByCode returns a collision, second returns empty (unique code found)
        when(shortUrlRepo.findByCode(any()))
            .thenReturn(Optional.of(existing))
            .thenReturn(Optional.empty());

        ShortUrl result = service.create("https://example.com", null);

        assertThat(result).isNotNull();
        // save was called once with the unique code
        verify(shortUrlRepo, times(1)).save(any(ShortUrl.class));
        // findByCode was called at least twice (one collision + one success)
        verify(shortUrlRepo, atLeast(2)).findByCode(any());
    }

    // T-05: expired code is inactive
    @Test
    void isActiveAt_shouldReturnFalseForExpiredUrl() {
        ShortUrl expired = new ShortUrl("x", "https://example.com", Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(3600));
        assertThat(expired.isActiveAt(Instant.now())).isFalse();
    }

    @Test
    void isActiveAt_shouldReturnTrueWhenNoExpiry() {
        ShortUrl noExpiry = new ShortUrl("x", "https://example.com", Instant.now(), null);
        assertThat(noExpiry.isActiveAt(Instant.now())).isTrue();
    }

    // buildShortUrl delegates to config
    @Test
    void buildShortUrl_shouldUseConfiguredBase() {
        assertThat(service.buildShortUrl("abc123")).isEqualTo("http://localhost:8080/r/abc123");
    }

    // redirectAndRecordClick returns false for expired/inactive URL
    @Test
    void redirectAndRecordClick_shouldReturnFalseForInactiveUrl() {
        ShortUrl expired = new ShortUrl("x", "https://example.com", Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(3600));
        boolean result = service.redirectAndRecordClick(expired);
        assertThat(result).isFalse();
        verify(clickRepo, never()).save(any());
    }

    // redirectAndRecordClick increments existing aggregate bucket
    @Test
    void redirectAndRecordClick_shouldIncrementExistingBucket() {
        ShortUrl active = new ShortUrl("x", "https://example.com", Instant.now(), null);
        ClickAggregate existing = new ClickAggregate(active, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS));
        when(clickRepo.findByShortUrlAndBucketStart(eq(active), any())).thenReturn(Optional.of(existing));
        when(clickRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.redirectAndRecordClick(active);

        assertThat(result).isTrue();
        assertThat(existing.getClickCount()).isEqualTo(2);
    }
}
