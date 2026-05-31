package com.memcyco.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memcyco.shortener.analytics.ClickEvent;
import com.memcyco.shortener.analytics.ClickEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "app.public-base-url=http://short.local"
})
@ActiveProfiles("test")
class ShortLinkServiceTest {
  @Autowired
  ShortLinkService service;

  @Autowired
  ShortLinkRepository repository;

  @Autowired
  ClickEventRepository clickEventRepository;

  @Autowired
  CacheManager cacheManager;

  @AfterEach
  void clearCache() {
    Cache cache = cacheManager.getCache("shortLinks");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void createsLinkWithCustomAlias() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/page",
        "example",
        "random_base62",
        Instant.now().plusSeconds(3600),
        5L,
        List.of("campaign"),
        null
    ));

    assertThat(created.shortCode()).isEqualTo("example");
    assertThat(created.shortUrl()).isEqualTo("http://short.local/example");
    assertThat(created.status()).isEqualTo("active");
  }

  @Test
  void createsLinkWithGeneratedCodeDefaultStrategyAndCleanTags() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/generated",
        null,
        " ",
        null,
        null,
        List.of(" launch ", "", "paid", "launch"),
        null
    ));

    assertThat(created.shortCode()).matches("[0-9A-Za-z]{7}");
    assertThat(created.strategy()).isEqualTo("random_base62");
    assertThat(created.tags()).containsExactly("launch", "paid");
  }

  @Test
  void listAndGetReturnPersistedLinks() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/list",
        "list-link",
        "random_base62",
        null,
        null,
        null,
        null
    ));

    assertThat(service.get(created.id()).shortCode()).isEqualTo("list-link");
    assertThat(service.list()).extracting(ShortLinkResponse::shortCode).contains("list-link");
  }

  @Test
  void updateCanRenameAliasAndEvictsOldAndNewCacheEntries() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/old",
        "old-code",
        "random_base62",
        null,
        null,
        null,
        null
    ));
    Cache cache = cacheManager.getCache("shortLinks");
    assertThat(cache).isNotNull();
    cache.put("old-code", "stale-old");
    cache.put("new-code", "stale-new");

    ShortLinkResponse updated = service.update(created.id(), new ShortLinkRequest(
        "https://example.com/new",
        "new-code",
        "hash_truncate",
        null,
        25L,
        List.of("updated"),
        null
    ));

    assertThat(updated.shortCode()).isEqualTo("new-code");
    assertThat(updated.originalUrl()).isEqualTo("https://example.com/new");
    assertThat(updated.strategy()).isEqualTo("hash_truncate");
    assertThat(updated.maxClicks()).isEqualTo(25);
    assertThat(updated.tags()).containsExactly("updated");
    assertThat(cache.get("old-code")).isNull();
    assertThat(cache.get("new-code")).isNull();
  }

  @Test
  void updateKeepsAliasWhenCustomAliasIsBlank() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/keep",
        "keep-code",
        "random_base62",
        null,
        null,
        null,
        null
    ));

    ShortLinkResponse updated = service.update(created.id(), new ShortLinkRequest(
        "https://example.com/keep-updated",
        " ",
        "random_base62",
        null,
        null,
        null,
        null
    ));

    assertThat(updated.shortCode()).isEqualTo("keep-code");
    assertThat(updated.originalUrl()).isEqualTo("https://example.com/keep-updated");
  }

  @Test
  void deleteRemovesLinkAndEvictsCacheEntry() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/delete",
        "delete-me",
        "random_base62",
        null,
        null,
        null,
        null
    ));
    Cache cache = cacheManager.getCache("shortLinks");
    assertThat(cache).isNotNull();
    cache.put("delete-me", "stale");

    service.delete(created.id());

    assertThat(repository.findById(created.id())).isEmpty();
    assertThat(cache.get("delete-me")).isNull();
  }

  @Test
  void deleteRemovesClickEventsBeforeDeletingLink() {
    ShortLinkResponse created = service.create(new ShortLinkRequest(
        "https://example.com/delete-with-clicks",
        "delete-clicks",
        "random_base62",
        null,
        null,
        null,
        null
    ));
    ShortLink link = repository.findById(created.id()).orElseThrow();
    ClickEvent event = new ClickEvent();
    event.setShortLink(link);
    event.setClickedAt(Instant.now());
    event.setReferer("https://referer.example");
    clickEventRepository.save(event);

    service.delete(created.id());

    assertThat(repository.findById(created.id())).isEmpty();
    assertThat(clickEventRepository.findByShortLinkIdOrderByClickedAtAsc(created.id())).isEmpty();
  }

  @Test
  void rejectsDuplicateAlias() {
    service.create(new ShortLinkRequest("https://example.com/one", "dupe", "random_base62", null, null, null, null));

    assertThatThrownBy(() -> service.create(new ShortLinkRequest("https://example.com/two", "dupe", "random_base62", null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already in use");
  }

  @Test
  void rejectsInvalidDestinationUrl() {
    assertThatThrownBy(() -> service.create(new ShortLinkRequest("ftp://example.com", null, "random_base62", null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("http or https");
  }

  @Test
  void rejectsMalformedAlias() {
    assertThatThrownBy(() -> service.create(new ShortLinkRequest("https://example.com", "x!", "random_base62", null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Custom alias");
  }

  @Test
  void rejectsPastExpiration() {
    assertThatThrownBy(() -> service.create(new ShortLinkRequest(
        "https://example.com",
        "past-expiration",
        "random_base62",
        Instant.now().minusSeconds(5),
        null,
        null,
        null
    )))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("future");
  }

  @Test
  void rejectsUnknownStrategy() {
    assertThatThrownBy(() -> service.create(new ShortLinkRequest("https://example.com", null, "missing", null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown generation strategy");
  }

  @Test
  void rejectsMissingLinkLookupUpdateAndDelete() {
    assertThatThrownBy(() -> service.get(999999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");

    assertThatThrownBy(() -> service.update(999999L, new ShortLinkRequest("https://example.com", null, "random_base62", null, null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");

    assertThatThrownBy(() -> service.delete(999999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }
}
