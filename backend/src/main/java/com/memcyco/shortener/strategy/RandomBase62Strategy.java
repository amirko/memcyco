package com.memcyco.shortener.strategy;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class RandomBase62Strategy implements ShortCodeStrategy {
  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private final SecureRandom random = new SecureRandom();

  @Override
  public String name() {
    return "random_base62";
  }

  @Override
  public String label() {
    return "Random Base62";
  }

  @Override
  public String description() {
    return "Generates a random 7-character Base62 code.";
  }

  @Override
  public String generate(String originalUrl, Predicate<String> exists) {
    for (int attempt = 0; attempt < 25; attempt++) {
      StringBuilder code = new StringBuilder(7);
      for (int i = 0; i < 7; i++) {
        code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
      }
      if (!exists.test(code.toString())) {
        return code.toString();
      }
    }
    throw new IllegalStateException("Unable to generate a unique short code");
  }
}
