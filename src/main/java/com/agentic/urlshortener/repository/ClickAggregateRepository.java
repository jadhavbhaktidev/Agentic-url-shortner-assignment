package com.agentic.urlshortener.repository;
import com.agentic.urlshortener.domain.*;
import java.time.Instant; import java.util.Optional;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ClickAggregateRepository extends JpaRepository<ClickAggregate, Long> {
 Optional<ClickAggregate> findByShortUrlAndBucketStart(ShortUrl shortUrl, Instant bucketStart);
 @Query("select coalesce(sum(c.clickCount), 0) from ClickAggregate c where c.shortUrl = :url") long totalFor(@Param("url") ShortUrl url);
}
