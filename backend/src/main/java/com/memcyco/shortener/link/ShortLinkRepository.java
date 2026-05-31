package com.memcyco.shortener.link;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
  Optional<ShortLink> findByShortCode(String shortCode);

  boolean existsByShortCode(String shortCode);
}
