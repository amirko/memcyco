package com.memcyco.shortener.analytics;

import com.memcyco.shortener.link.ShortLinkRepository;
import com.memcyco.shortener.link.ShortLinkService;
import java.time.Instant;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickTrackingService {
  private final ClickEventRepository clickEventRepository;
  private final ShortLinkRepository shortLinkRepository;
  private final ShortLinkService shortLinkService;

  public ClickTrackingService(
      ClickEventRepository clickEventRepository,
      ShortLinkRepository shortLinkRepository,
      ShortLinkService shortLinkService
  ) {
    this.clickEventRepository = clickEventRepository;
    this.shortLinkRepository = shortLinkRepository;
    this.shortLinkService = shortLinkService;
  }

  @Async
  @Transactional
  public void track(Long shortLinkId, String shortCode, String referer, String userAgent, String ipAddress) {
    shortLinkRepository.findById(shortLinkId).ifPresent(link -> {
      ClickEvent event = new ClickEvent();
      event.setShortLink(link);
      event.setClickedAt(Instant.now());
      event.setReferer(truncate(referer, 1024));
      event.setUserAgent(truncate(userAgent, 1024));
      event.setIpAddress(truncate(ipAddress, 128));
      clickEventRepository.save(event);

      link.setClickCount(link.getClickCount() + 1);
      shortLinkRepository.save(link);
      if (link.getMaxClicks() != null) {
        shortLinkService.evictCode(shortCode);
      }
    });
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
