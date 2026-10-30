package com.socialflow;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
      Map.of(
        "message",
        e.getReason() == null ? "Request rejected" : e.getReason()
      )
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(
      Map.of("message", "Check the required fields and their length")
    );
  }

  @ExceptionHandler(DuplicateKeyException.class)
  ResponseEntity<?> duplicate() {
    return ResponseEntity.status(409).body(
      Map.of("message", "That record already exists")
    );
  }
}
