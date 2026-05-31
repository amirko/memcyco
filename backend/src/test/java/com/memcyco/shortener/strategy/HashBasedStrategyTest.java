package com.memcyco.shortener.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class HashBasedStrategyTest {
  private final HashBasedStrategy strategy = new HashBasedStrategy();

  @Test
  void generatesFirstAvailableHashPrefix() throws Exception {
    String url = "https://example.com/landing";
    String hash = sha256(url);

    String code = strategy.generate(url, existing -> existing.equals(hash.substring(0, 8)));

    assertThat(code).isEqualTo(hash.substring(0, 9));
  }

  @Test
  void throwsWhenAllSupportedPrefixesCollide() {
    assertThatThrownBy(() -> strategy.generate("https://example.com", ignored -> true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("hash-based");
  }

  private String sha256(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
