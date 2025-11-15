package com.example.junction2025.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    @Schema(description = "응답 상태 코드", example = "200")
    private int status;
    @Schema(description = "응답 메세지", example = "example message.")
    private String message;
    @Schema(description = "응답 데이터 (에러가 발생하거나 반환 데이터가 없을 시 null 값이 옵니다.)", example = "null", nullable = true)
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, null, data);
    }

    public static <T> ApiResponse<T> success(String message,T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(status, null, data);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
