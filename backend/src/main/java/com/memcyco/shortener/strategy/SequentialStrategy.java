package com.memcyco.shortener.strategy;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class SequentialStrategy implements ShortCodeStrategy {
  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

  @Override
  public String name() {
    return "sequential_base62";
  }

  @Override
  public String label() {
    return "Sequential Base62";
  }

  @Override
  public String description() {
    return "Encodes a monotonically increasing counter as Base62.";
  }

  @Override
  public String generate(String originalUrl, Predicate<String> exists) {
    for (int attempt = 0; attempt < 100; attempt++) {
      String candidate = encode(counter.incrementAndGet());
      if (!exists.test(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to generate a sequential code");
  }

  private String encode(long value) {
    StringBuilder encoded = new StringBuilder();
    long current = value;
    while (current > 0) {
      encoded.append(ALPHABET[(int) (current % ALPHABET.length)]);
      current /= ALPHABET.length;
    }
    return encoded.reverse().toString();
  }
}
