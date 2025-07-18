package com.intellimart.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat; // NEW IMPORT
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'")
    private LocalDateTime timestamp;
    private int status;
    private String error; // HTTP status reason phrase (e.g., "Not Found")
    private String message; // Detailed error message
    private String path; // The request URI
}