package com.memcyco.shortener.link;

import com.memcyco.shortener.analytics.ClickEventRepository;
import com.memcyco.shortener.strategy.StrategyService;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {
  private static final String ALIAS_PATTERN = "^[A-Za-z0-9_-]{3,64}$";

  private final ShortLinkRepository repository;
  private final ClickEventRepository clickEventRepository;
  private final StrategyService strategyService;
  private final CacheManager cacheManager;
  private final String publicBaseUrl;

  public ShortLinkService(
      ShortLinkRepository repository,
      ClickEventRepository clickEventRepository,
      StrategyService strategyService,
      CacheManager cacheManager,
      @Value("${app.public-base-url}") String publicBaseUrl
  ) {
    this.repository = repository;
    this.clickEventRepository = clickEventRepository;
    this.strategyService = strategyService;
    this.cacheManager = cacheManager;
    this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
  }

  @Transactional(readOnly = true)
  public List<ShortLinkResponse> list() {
    return repository.findAll().stream()
        .map(link -> ShortLinkResponse.from(link, publicBaseUrl))
        .toList();
  }

  @Transactional(readOnly = true)
  public ShortLinkResponse get(Long id) {
    return ShortLinkResponse.from(find(id), publicBaseUrl);
  }

  @Transactional
  public ShortLinkResponse create(ShortLinkRequest request) {
    validateRequest(request);
    ShortLink link = new ShortLink();
    link.setOriginalUrl(request.originalUrl());
    link.setStrategy(normalizeStrategy(request.strategy()));
    link.setExpiresAt(request.expiresAt());
    link.setMaxClicks(request.maxClicks());
    link.setTags(cleanTags(request.tags()));

    String alias = cleanAlias(request.customAlias());
    link.setShortCode(alias == null
        ? strategyService.generate(link.getStrategy(), request.originalUrl(), repository::existsByShortCode)
        : reserveCustomAlias(alias));

    return ShortLinkResponse.from(repository.save(link), publicBaseUrl);
  }

  @Transactional
  public ShortLinkResponse update(Long id, ShortLinkRequest request) {
    validateRequest(request);
    ShortLink link = find(id);
    String oldCode = link.getShortCode();
    link.setOriginalUrl(request.originalUrl());
    link.setStrategy(normalizeStrategy(request.strategy()));
    link.setExpiresAt(request.expiresAt());
    link.setMaxClicks(request.maxClicks());
    link.setTags(cleanTags(request.tags()));

    String alias = cleanAlias(request.customAlias());
    if (alias != null && !alias.equals(link.getShortCode())) {
      link.setShortCode(reserveCustomAlias(alias));
    }

    ShortLinkResponse response = ShortLinkResponse.from(repository.save(link), publicBaseUrl);
    evictCode(oldCode);
    evictCode(response.shortCode());
    return response;
  }

  @Transactional
  public void delete(Long id) {
    ShortLink link = find(id);
    clickEventRepository.deleteByShortLinkId(id);
    repository.delete(link);
    evictCode(link.getShortCode());
  }

  public void evictCode(String shortCode) {
    Cache cache = cacheManager.getCache("shortLinks");
    if (cache != null) {
      cache.evict(shortCode);
    }
  }

  private ShortLink find(Long id) {
    return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Short link not found"));
  }

  private void validateRequest(ShortLinkRequest request) {
    validateUrl(request.originalUrl());
    if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
      throw new IllegalArgumentException("Expiration date must be in the future");
    }
    String alias = cleanAlias(request.customAlias());
    if (alias != null && !alias.matches(ALIAS_PATTERN)) {
      throw new IllegalArgumentException("Custom alias must be 3-64 characters using letters, numbers, underscores, or hyphens");
    }
    String strategy = normalizeStrategy(request.strategy());
    strategyService.requireStrategy(strategy);
  }

  private void validateUrl(String url) {
    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      if (!scheme.equals("http") && !scheme.equals("https")) {
        throw new IllegalArgumentException("Original URL must use http or https");
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new IllegalArgumentException("Original URL must include a host");
      }
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Original URL must be a valid http or https URL");
    }
  }

  private String reserveCustomAlias(String alias) {
    if (repository.existsByShortCode(alias)) {
      throw new IllegalArgumentException("Custom alias is already in use");
    }
    return alias;
  }

  private String normalizeStrategy(String strategy) {
    return strategy == null || strategy.isBlank() ? "random_base62" : strategy.trim();
  }

  private String cleanAlias(String alias) {
    return alias == null || alias.isBlank() ? null : alias.trim();
  }

  private List<String> cleanTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }
    return tags.stream()
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .distinct()
        .limit(20)
        .toList();
  }
}
