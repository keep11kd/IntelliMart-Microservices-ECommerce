package com.intellimart.cartservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) // 503 Service Unavailable
public class ProductServiceCommunicationException extends RuntimeException {
    public ProductServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductServiceCommunicationException(String message) {
        super(message);
    }
}