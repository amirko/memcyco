package com.memcyco.shortener.analytics;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
  List<ClickEvent> findByShortLinkIdOrderByClickedAtAsc(Long shortLinkId);

  long deleteByShortLinkId(Long shortLinkId);
}
