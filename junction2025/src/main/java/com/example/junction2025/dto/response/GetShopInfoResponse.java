package com.example.junction2025.dto.response;

import com.example.junction2025.domain.ShopType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class GetShopInfoResponse {
    private String shopId; // 좋아요 누를 때 상점 Id로 요청하기 위함
    private Double latitude;
    private Double longitude;
    private String videoUrl;
    private String shopImageUrl;
    private Double rating;
    private int rating_count;
    private ShopType shopType;
    private String locationString;
    private String openTime;
    private List<String> comments;

    // Mock 데이터 생성 메서드
    public static List<GetShopInfoResponse> getMockData() {
        List<GetShopInfoResponse> shops = new ArrayList<>();

        shops.add(new GetShopInfoResponse(
                "shop001",
                60.1695,
                24.9354,
                "https://example.com/video1.mp4",
                "https://lh3.googleusercontent.com/places/ANXAkqFSLXEJFJZx_fRbA4I6hUOWTimV2zRXH41db5UiTBP9Hb1rRtBi0bObJBQPQDgu3dWNMStlKFmA_yX7hIyVrxx_sk__OjQ5W9E=s1600-w800",
                4.5,
                120,
                ShopType.Cafe,
                "Esplanadi 1, Helsinki, Finland",
                "08:00 - 20:00",
                List.of("맛있어요!", "커피가 좋네요.", "분위기 최고!")
        ));

        shops.add(new GetShopInfoResponse(
                "shop002",
                60.1700,
                24.9370,
                "https://example.com/video2.mp4",
                "https://lh3.googleusercontent.com/places/ANXAkqFSLXEJFJZx_fRbA4I6hUOWTimV2zRXH41db5UiTBP9Hb1rRtBi0bObJBQPQDgu3dWNMStlKFmA_yX7hIyVrxx_sk__OjQ5W9E=s1600-w800",
                4.0,
                80,
                ShopType.Cafe,
                "Mannerheimintie 10, Helsinki, Finland",
                "07:00 - 19:00",
                List.of("빵이 신선해요.", "친절한 직원들")
        ));

        shops.add(new GetShopInfoResponse(
                "shop003",
                60.1680,
                24.9330,
                "https://example.com/video3.mp4",
                "https://lh3.googleusercontent.com/places/ANXAkqFSLXEJFJZx_fRbA4I6hUOWTimV2zRXH41db5UiTBP9Hb1rRtBi0bObJBQPQDgu3dWNMStlKFmA_yX7hIyVrxx_sk__OjQ5W9E=s1600-w800",
                4.8,
                200,
                ShopType.Restaurant,
                "Aleksanterinkatu 5, Helsinki, Finland",
                "11:00 - 22:00",
                List.of("서비스가 훌륭합니다.", "음식이 맛있어요!", "재방문 의사 있습니다.")
        ));

        return shops;
    }
}
