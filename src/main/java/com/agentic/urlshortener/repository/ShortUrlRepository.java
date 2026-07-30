package com.agentic.urlshortener.repository;
import com.agentic.urlshortener.domain.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> { Optional<ShortUrl> findByCode(String code); }
