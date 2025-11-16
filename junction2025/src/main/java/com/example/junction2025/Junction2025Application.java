package com.example.junction2025;

import com.example.junction2025.services.PlaceCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication
public class Junction2025Application {

    public static void main(String[] args) {
        SpringApplication.run(Junction2025Application.class, args);
    }
}

//@Autowired
//private PlaceCollectionService placeCollectionService;
//
//@Override
//public void run(String... args) {
//    System.out.println("상점 수집 시작... (20km 반경 내 격자 기반 수집)");
//
//    // 중심 위치 (예: 서울)
//    double centerLatitude = 60.155052;
//    double centerLongitude = 24.6313928;
//
//    try {
//        var statistics = placeCollectionService.collectAllStores(centerLatitude, centerLongitude);
//
//        System.out.println("수집 완료!");
//        System.out.println(statistics.toString());
//    } catch (Exception e) {
//        System.err.println("상점 수집 중 오류 발생: " + e.getMessage());
//        e.printStackTrace();
//    }
//}
