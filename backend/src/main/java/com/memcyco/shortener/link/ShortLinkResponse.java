package com.memcyco.shortener.link;

import java.time.Instant;
import java.util.List;

public record ShortLinkResponse(
    Long id,
    String shortCode,
    String shortUrl,
    String originalUrl,
    String strategy,
    Instant expiresAt,
    Long maxClicks,
    long totalClicks,
    String status,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {
  static ShortLinkResponse from(ShortLink link, String publicBaseUrl) {
    return new ShortLinkResponse(
        link.getId(),
        link.getShortCode(),
        publicBaseUrl + "/" + link.getShortCode(),
        link.getOriginalUrl(),
        link.getStrategy(),
        link.getExpiresAt(),
        link.getMaxClicks(),
        link.getClickCount(),
        link.status(Instant.now()),
        link.getTags(),
        link.getCreatedAt(),
        link.getUpdatedAt()
    );
  }
}
