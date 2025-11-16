package com.example.junction2025.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    private String idToken; // 구글 또는 애플 idToken
}
