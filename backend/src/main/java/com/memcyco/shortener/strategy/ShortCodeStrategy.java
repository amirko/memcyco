package com.memcyco.shortener.strategy;

import java.util.function.Predicate;

public interface ShortCodeStrategy {
  String name();

  String label();

  String description();

  String generate(String originalUrl, Predicate<String> exists);
}
