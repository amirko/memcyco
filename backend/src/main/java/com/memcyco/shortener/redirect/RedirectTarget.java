package com.memcyco.shortener.redirect;

import com.memcyco.shortener.link.ShortLink;
import java.time.Instant;

public record RedirectTarget(
    Long id,
    String shortCode,
    String originalUrl,
    Instant expiresAt,
    Long maxClicks,
    long clickCount
) {
  static RedirectTarget from(ShortLink link) {
    return new RedirectTarget(
        link.getId(),
        link.getShortCode(),
        link.getOriginalUrl(),
        link.getExpiresAt(),
        link.getMaxClicks(),
        link.getClickCount()
    );
  }

  boolean expired() {
    return expiresAt != null && !expiresAt.isAfter(Instant.now());
  }

  boolean clickExhausted() {
    return maxClicks != null && clickCount >= maxClicks;
  }
}
