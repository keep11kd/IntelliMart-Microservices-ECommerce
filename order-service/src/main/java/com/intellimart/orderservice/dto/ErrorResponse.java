package com.intellimart.orderservice.dto; // <--- This MUST match the import path

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String apiPath;
    private HttpStatus errorCode; // You might need to import org.springframework.http.HttpStatus
    private String errorMessage;
    private LocalDateTime errorTime;
}