package com.memcyco.shortener.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsIllegalArgumentToBadRequest() {
    var response = handler.badRequest(new IllegalArgumentException("Original URL must be valid"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "Original URL must be valid");
  }

  @Test
  void mapsDataIntegrityErrorsToConflict() {
    var response = handler.conflict(new DataIntegrityViolationException("duplicate key"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).containsEntry("message", "The requested short code already exists");
  }

  @Test
  void mapsValidationErrorsToFirstFieldErrorMessage() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "originalUrl", "must not be blank"));

    var response = handler.validation(new MethodArgumentNotValidException(requestParameter(), bindingResult));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "originalUrl must not be blank");
  }

  @Test
  void mapsValidationErrorsWithoutFieldsToGenericMessage() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

    var response = handler.validation(new MethodArgumentNotValidException(requestParameter(), bindingResult));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "Validation failed");
  }

  private MethodParameter requestParameter() throws NoSuchMethodException {
    Method method = DummyController.class.getDeclaredMethod("create", Object.class);
    return new MethodParameter(method, 0);
  }

  private static class DummyController {
    @SuppressWarnings("unused")
    void create(@Valid @RequestBody Object request) {
    }
  }
}
