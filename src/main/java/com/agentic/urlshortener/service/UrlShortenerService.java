package com.agentic.urlshortener.service;

import com.agentic.urlshortener.config.AppProperties;
import com.agentic.urlshortener.domain.ClickAggregate;
import com.agentic.urlshortener.domain.ShortUrl;
import com.agentic.urlshortener.repository.ClickAggregateRepository;
import com.agentic.urlshortener.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UrlShortenerService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickAggregateRepository clickAggregateRepository;
    private final AppProperties appProperties;

    public UrlShortenerService(ShortUrlRepository shortUrlRepository,
                               ClickAggregateRepository clickAggregateRepository,
                               AppProperties appProperties) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickAggregateRepository = clickAggregateRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public ShortUrl create(String destinationUrl) {
        validateDestination(destinationUrl);
        String code = generateCode();
        ShortUrl entity = new ShortUrl(code, destinationUrl, Instant.now(), null);
        return shortUrlRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<ShortUrl> findByCode(String code) {
        return shortUrlRepository.findByCode(code);
    }

    @Transactional
    public boolean redirectAndRecordClick(ShortUrl shortUrl) {
        if (!shortUrl.isActiveAt(Instant.now())) {
            return false;
        }

        Instant bucketStart = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        Optional<ClickAggregate> existing = clickAggregateRepository.findByShortUrlAndBucketStart(shortUrl, bucketStart);
        if (existing.isPresent()) {
            existing.get().increment();
            clickAggregateRepository.save(existing.get());
        } else {
            clickAggregateRepository.save(new ClickAggregate(shortUrl, bucketStart));
        }
        return true;
    }

    @Transactional(readOnly = true)
    public long getAnalyticsCount(ShortUrl shortUrl) {
        return clickAggregateRepository.totalFor(shortUrl);
    }

    public String buildShortUrl(String code) {
        return appProperties.publicBaseUrl() + "/r/" + code;
    }

    private void validateDestination(String destinationUrl) {
        try {
            URI uri = new URI(destinationUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Only HTTP(S) URLs are supported");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Destination URL must include a host");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Destination URL is invalid", e);
        }
    }

    private String generateCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (shortUrlRepository.findByCode(code).isPresent());
        return code;
    }
}
