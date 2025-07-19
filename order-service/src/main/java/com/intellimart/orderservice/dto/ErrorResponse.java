package com.intellimart.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema; // <--- NEW IMPORT
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standardized error response DTO")
public class ErrorResponse {
    @Schema(description = "The API path where the error occurred", example = "/api/orders/123/status")
    private String apiPath;
    @Schema(description = "The HTTP status code of the error", example = "404 NOT_FOUND")
    private HttpStatus errorCode;
    @Schema(description = "A detailed error message", example = "Order not found with ID: 123")
    private String errorMessage;
    @Schema(description = "Timestamp when the error occurred", example = "2023-10-26T11:45:30.123")
    private LocalDateTime errorTime;
}