package com.agentic.urlshortener.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="click_aggregate", uniqueConstraints=@UniqueConstraint(name="uk_click_aggregate_bucket", columnNames={"short_url_id","bucket_start"}))
public class ClickAggregate {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="short_url_id") private ShortUrl shortUrl;
 @Column(name="bucket_start", nullable=false) private Instant bucketStart;
 @Column(name="click_count", nullable=false) private long clickCount;
 protected ClickAggregate() { } public ClickAggregate(ShortUrl shortUrl, Instant bucketStart){this.shortUrl=shortUrl;this.bucketStart=bucketStart;this.clickCount=1;}
 public void increment(){clickCount++;} public long getClickCount(){return clickCount;}
}
