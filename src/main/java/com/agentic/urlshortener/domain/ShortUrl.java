package com.agentic.urlshortener.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "short_url", uniqueConstraints = @UniqueConstraint(name = "uk_short_url_code", columnNames = "code"))
public class ShortUrl {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, length = 16) private String code;
 @Column(name = "destination_url", nullable = false, length = 2048) private String destinationUrl;
 @Column(name = "created_at", nullable = false) private Instant createdAt;
 @Column(name = "expires_at") private Instant expiresAt;
 @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private UrlStatus status;
 protected ShortUrl() { }
 public ShortUrl(String code, String destinationUrl, Instant createdAt, Instant expiresAt) { this.code=code; this.destinationUrl=destinationUrl; this.createdAt=createdAt; this.expiresAt=expiresAt; this.status=UrlStatus.ACTIVE; }
 public Long getId(){return id;} public String getCode(){return code;} public String getDestinationUrl(){return destinationUrl;} public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;}
 public boolean isActiveAt(Instant now){return status==UrlStatus.ACTIVE && (expiresAt==null || expiresAt.isAfter(now));}
}
