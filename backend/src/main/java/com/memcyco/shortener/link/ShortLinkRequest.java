package com.memcyco.shortener.link;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ShortLinkRequest(
    @NotBlank String originalUrl,
    String customAlias,
    String strategy,
    Instant expiresAt,
    @Min(1) Long maxClicks,
    List<String> tags,
    Map<String, Object> parameters
) {
}
