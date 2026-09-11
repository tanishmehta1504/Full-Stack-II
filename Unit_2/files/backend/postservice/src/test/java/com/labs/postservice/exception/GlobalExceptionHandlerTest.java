package com.labs.postservice.exception;

import com.labs.postservice.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound returns HTTP 404 NOT_FOUND with error envelope")
    void testHandleNotFound() {
        PostNotFoundException ex = new PostNotFoundException(42L);
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("post 42 was not found", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleInvalidState returns HTTP 409 CONFLICT with error envelope")
    void testHandleInvalidState() {
        InvalidPostStateException ex = new InvalidPostStateException("Post 42 is already published");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Post 42 is already published", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleUnexpected returns HTTP 500 INTERNAL_SERVER_ERROR")
    void testHandleUnexpected() {
        RuntimeException ex = new RuntimeException("Database connection timeout");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpected(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected internal server error occurred", response.getBody().getMessage());
    }
}
