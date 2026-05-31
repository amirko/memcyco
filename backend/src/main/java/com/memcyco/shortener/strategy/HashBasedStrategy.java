package com.memcyco.shortener.strategy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class HashBasedStrategy implements ShortCodeStrategy {
  @Override
  public String name() {
    return "hash_truncate";
  }

  @Override
  public String label() {
    return "Hash Truncation";
  }

  @Override
  public String description() {
    return "Uses a SHA-256 hash of the destination URL and truncates it to a compact code.";
  }

  @Override
  public String generate(String originalUrl, Predicate<String> exists) {
    String hex = sha256(originalUrl);
    for (int length = 8; length <= 16; length++) {
      String candidate = hex.substring(0, length);
      if (!exists.test(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to generate a unique hash-based code");
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
