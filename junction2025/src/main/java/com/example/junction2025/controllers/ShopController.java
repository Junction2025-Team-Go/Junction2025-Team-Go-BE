package com.example.junction2025.controllers;

import com.example.junction2025.dto.ApiResponse;
import com.example.junction2025.dto.request.GetShopInfosRequest;
import com.example.junction2025.dto.response.GetShopInfoResponse;
import com.example.junction2025.services.StoreService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@AllArgsConstructor
public class ShopController {
    private final StoreService storeService;

    @GetMapping("/get_shop_infos")
    public ResponseEntity<ApiResponse<List<GetShopInfoResponse>>> getShopInfos(@RequestBody GetShopInfosRequest request) {
        List<GetShopInfoResponse> response = storeService.getShopInfoResponses(request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
