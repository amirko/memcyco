package com.memcyco.shortener.redirect;

import com.memcyco.shortener.link.ShortLinkRepository;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {
  private final ShortLinkRepository shortLinkRepository;

  public RedirectService(ShortLinkRepository shortLinkRepository) {
    this.shortLinkRepository = shortLinkRepository;
  }

  @Cacheable(cacheNames = "shortLinks", key = "#shortCode")
  @Transactional(readOnly = true)
  public Optional<RedirectTarget> resolve(String shortCode) {
    return shortLinkRepository.findByShortCode(shortCode).map(RedirectTarget::from);
  }
}
