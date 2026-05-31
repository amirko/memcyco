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
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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
}
