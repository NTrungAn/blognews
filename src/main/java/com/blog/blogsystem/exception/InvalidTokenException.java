package com.blog.blogsystem.exception;

/**
 * Custom exception cho các lỗi liên quan đến token (revoked, expired, reuse detected...).
 * Được xử lý bởi GlobalExceptionHandler → HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
