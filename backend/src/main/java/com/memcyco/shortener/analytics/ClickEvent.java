package com.memcyco.shortener.analytics;

import com.memcyco.shortener.link.ShortLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "click_events", indexes = {
    @Index(name = "idx_click_events_link_time", columnList = "short_link_id,clickedAt")
})
public class ClickEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "short_link_id", nullable = false)
  private ShortLink shortLink;

  @Column(nullable = false)
  private Instant clickedAt;

  @Column(length = 1024)
  private String referer;

  @Column(length = 1024)
  private String userAgent;

  @Column(length = 128)
  private String ipAddress;

  public Long getId() {
    return id;
  }

  public ShortLink getShortLink() {
    return shortLink;
  }

  public void setShortLink(ShortLink shortLink) {
    this.shortLink = shortLink;
  }

  public Instant getClickedAt() {
    return clickedAt;
  }

  public void setClickedAt(Instant clickedAt) {
    this.clickedAt = clickedAt;
  }

  public String getReferer() {
    return referer;
  }

  public void setReferer(String referer) {
    this.referer = referer;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
}
