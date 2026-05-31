package com.memcyco.shortener.link;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "short_links", indexes = {
    @Index(name = "idx_short_links_code", columnList = "shortCode", unique = true)
})
public class ShortLink {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String shortCode;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(nullable = false, length = 64)
  private String strategy;

  private Instant expiresAt;

  private Long maxClicks;

  @Column(nullable = false)
  private long clickCount = 0;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "short_link_tags", joinColumns = @JoinColumn(name = "short_link_id"))
  @Column(name = "tag", length = 64)
  private List<String> tags = new ArrayList<>();

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void beforeCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void beforeUpdate() {
    updatedAt = Instant.now();
  }

  public boolean isExpired(Instant now) {
    return expiresAt != null && !expiresAt.isAfter(now);
  }

  public boolean isClickExhausted() {
    return maxClicks != null && clickCount >= maxClicks;
  }

  public String status(Instant now) {
    if (isExpired(now)) {
      return "expired";
    }
    if (isClickExhausted()) {
      return "click_exhausted";
    }
    return "active";
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getShortCode() {
    return shortCode;
  }

  public void setShortCode(String shortCode) {
    this.shortCode = shortCode;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String strategy) {
    this.strategy = strategy;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Long getMaxClicks() {
    return maxClicks;
  }

  public void setMaxClicks(Long maxClicks) {
    this.maxClicks = maxClicks;
  }

  public long getClickCount() {
    return clickCount;
  }

  public void setClickCount(long clickCount) {
    this.clickCount = clickCount;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
