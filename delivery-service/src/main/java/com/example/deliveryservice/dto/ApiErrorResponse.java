package com.example.deliveryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard API error response")
public class ApiErrorResponse {
    private String error;
    private String message;

    public ApiErrorResponse() {}

    public ApiErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
