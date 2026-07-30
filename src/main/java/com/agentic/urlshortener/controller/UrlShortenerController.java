package com.agentic.urlshortener.controller;

import com.agentic.urlshortener.domain.ShortUrl;
import com.agentic.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request) {
        String destinationUrl = request.get("destinationUrl");
        if (destinationUrl == null || destinationUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ShortUrl created = urlShortenerService.create(destinationUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "code", created.getCode(),
                "shortUrl", urlShortenerService.buildShortUrl(created.getCode()),
                "destinationUrl", created.getDestinationUrl(),
                "createdAt", created.getCreatedAt().toString()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        var shortUrl = urlShortenerService.findByCode(code);
        if (shortUrl.isPresent() && urlShortenerService.redirectAndRecordClick(shortUrl.get())) {
            return ResponseEntity.<Void>status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, shortUrl.get().getDestinationUrl())
                .build();
        }
        return ResponseEntity.<Void>notFound().build();
    }

    @GetMapping("/api/v1/urls/{code}/analytics")
    public ResponseEntity<?> analytics(@PathVariable String code) {
        return urlShortenerService.findByCode(code)
            .map(shortUrl -> ResponseEntity.ok(Map.of(
                "code", shortUrl.getCode(),
                "totalClicks", urlShortenerService.getAnalyticsCount(shortUrl),
                "from", Instant.EPOCH.toString(),
                "to", Instant.now().toString()
            )))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
