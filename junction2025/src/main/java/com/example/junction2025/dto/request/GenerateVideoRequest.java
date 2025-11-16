package com.example.junction2025.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateVideoRequest {
    private String prompt;
    private String image1;  // Image URI
    private String image2;  // Image URI
    private String image3;  // Image URI
}

