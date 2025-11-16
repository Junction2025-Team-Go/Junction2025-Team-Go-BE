package com.example.junction2025.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SupportCountResponse {
    
    @JsonProperty("total-supporters")
    private Integer totalSupporters;
}

