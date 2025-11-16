package com.example.junction2025.services;

import com.example.junction2025.domain.Store;
import com.example.junction2025.domain.Review;
import com.example.junction2025.dto.request.GenerateVideoRequest;
import com.example.junction2025.dto.response.GenerateVideoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;

/**
 * Google Veo 3 API를 호출하는 서비스
 * Google Cloud AI Platform의 REST API를 사용합니다.
 */
@Slf4j
@Service
public class Veo3Service {
    
    private final RestClient restClient;
    private final S3Service s3Service;
    private final String projectId;
    private final String modelId;
    private final String location;
    private final String accessToken;
    private final int durationSeconds;
    private final int sampleCount;
    
    public Veo3Service(
            RestClient.Builder restClientBuilder,
            S3Service s3Service,
            @Value("${veo3.api.project-id:}") String projectId,
            @Value("${veo3.api.model-id:veo-3.1-generate-preview}") String modelId,
            @Value("${veo3.api.location:us-central1}") String location,
            @Value("${veo3.api.access-token:}") String accessToken,
            @Value("${veo3.api.duration-seconds:8}") int durationSeconds,
            @Value("${veo3.api.sample-count:1}") int sampleCount
    ) {
        this.restClient = restClientBuilder.build();
        this.s3Service = s3Service;
        this.projectId = projectId;
        this.modelId = modelId;
        this.location = location;
        this.accessToken = accessToken;
        this.durationSeconds = durationSeconds;
        this.sampleCount = sampleCount;
    }
    
    /**
     * Store와 Review 데이터를 기반으로 영상을 생성합니다.
     *
     * @param store 상점 정보
     * @param reviews 리뷰 목록
     * @return 생성된 영상의 S3 URL (실패 시 null)
     */
    public String generateVideoForStore(Store store, List<Review> reviews) {
        if (store == null || reviews == null || reviews.isEmpty()) {
            log.warn("Store 또는 Review가 없어서 영상 생성 불가");
            return null;
        }
        
        // 리뷰에서 이미지 URL 수집 (최대 3개)
        List<String> imageUrls = new ArrayList<>();
        for (Review review : reviews) {
            if (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) {
                for (String imageUrl : review.getImageUrls()) {
                    if (imageUrls.size() >= 3) {
                        break;
                    }
                    imageUrls.add(imageUrl);
                }
            }
            if (imageUrls.size() >= 3) {
                break;
            }
        }
        
        if (imageUrls.size() < 3) {
            log.warn("리뷰 이미지가 3개 미만이어서 영상 생성 불가 (개수: {})", imageUrls.size());
            return null;
        }

        List<Review> promptReviews = reviews.subList(0, 5);

        // 프롬프트 생성
        String prompt = String.format(
            "이 리뷰 이미지들을 이용해 %s에서 판매하는 제품들을 소개하는 영상을 만들어줘. 최대한 이미지의 상점 모습과 똑같아야 해. 그리고 다음의 상점 리뷰들: %s, %s, %s, %s, %s 들을 종합하여 상점을 소개하듯 말해줘. 영어 음성을 추가해주고 자막까지 영어로 추가해주면 돼.",
                store.getName(),
                promptReviews.get(0),
                promptReviews.get(1),
                promptReviews.get(2),
                promptReviews.get(3),
                promptReviews.get(4)
        );
        
        return generateVideo(prompt, imageUrls, store.getName());
    }
    
    /**
     * GenerateVideoRequest를 받아서 영상을 생성합니다 (Controller용).
     *
     * @param request 비디오 생성 요청
     * @return 생성된 영상의 S3 URL을 포함한 응답
     */
    public GenerateVideoResponse generateVideo(GenerateVideoRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isEmpty()) {
            throw new IllegalArgumentException("프롬프트가 필요합니다.");
        }
        
        // 이미지 URL 수집
        List<String> imageUrls = new ArrayList<>();
        if (request.getImage1() != null && !request.getImage1().isEmpty()) {
            imageUrls.add(request.getImage1());
        }
        if (request.getImage2() != null && !request.getImage2().isEmpty()) {
            imageUrls.add(request.getImage2());
        }
        if (request.getImage3() != null && !request.getImage3().isEmpty()) {
            imageUrls.add(request.getImage3());
        }
        
        if (imageUrls.size() < 3) {
            throw new IllegalArgumentException("이미지 3개가 필요합니다.");
        }
        
        String videoUrl = generateVideo(request.getPrompt(), imageUrls, null);
        
        if (videoUrl == null) {
            return new GenerateVideoResponse(null, "비디오 생성 실패");
        }
        
        return new GenerateVideoResponse(videoUrl, "비디오 생성 완료");
    }
    
    /**
     * Veo 3 API를 호출하여 영상을 생성합니다.
     *
     * @param prompt 텍스트 프롬프트
     * @param imageUrls 참조 이미지 URL 목록 (최대 3개)
     * @param storeName 상점 이름 (파일명 생성용, 선택사항)
     * @return 생성된 영상의 S3 URL (실패 시 null)
     */
    public String generateVideo(String prompt, List<String> imageUrls, String storeName) {
        if (imageUrls == null || imageUrls.isEmpty() || imageUrls.size() > 3) {
            log.warn("이미지 URL이 없거나 3개를 초과합니다. (개수: {})", imageUrls != null ? imageUrls.size() : 0);
            return null;
        }
        
        if (projectId == null || projectId.isEmpty()) {
            log.error("Veo3 API project-id가 설정되지 않았습니다.");
            return null;
        }
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("Veo3 API access-token이 설정되지 않았습니다.");
            return null;
        }
        
        try {
            // 이미지 URL에서 이미지 데이터를 다운로드하여 base64로 변환
            List<Map<String, Object>> referenceImages = new ArrayList<>();
            for (String imageUrl : imageUrls) {
                try {
                    byte[] imageData = downloadImage(imageUrl);
                    String base64Image = Base64.getEncoder().encodeToString(imageData);
                    
                    Map<String, Object> referenceImage = new HashMap<>();
                    Map<String, String> imageDataMap = new HashMap<>();
                    imageDataMap.put("bytesBase64Encoded", base64Image);
                    imageDataMap.put("mimeType", "image/jpeg"); // 기본값, 실제로는 이미지 타입에 맞게 설정
                    referenceImage.put("image", imageDataMap);
                    referenceImage.put("referenceType", "asset");
                    
                    referenceImages.add(referenceImage);
                } catch (Exception e) {
                    log.warn("이미지 다운로드 실패: {}", imageUrl, e);
                }
            }
            
            if (referenceImages.isEmpty()) {
                log.warn("다운로드된 이미지가 없습니다.");
                return null;
            }
            
            // 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            
            // instances 배열
            Map<String, Object> instance = new HashMap<>();
            instance.put("prompt", prompt);
            instance.put("referenceImages", referenceImages);
            
            List<Map<String, Object>> instances = new ArrayList<>();
            instances.add(instance);
            requestBody.put("instances", instances);
            
            // parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("durationSeconds", durationSeconds);
            parameters.put("sampleCount", sampleCount);
            requestBody.put("parameters", parameters);
            
            // 엔드포인트 URL 생성
            String endpoint = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning",
                location, projectId, location, modelId
            );
            
            log.info("Veo3 API 호출 시작 - 엔드포인트: {}, 이미지 개수: {}, 프롬프트: {}", 
                endpoint, referenceImages.size(), prompt);
            
            // API 호출 (Long Running Operation)
            // 먼저 응답을 String으로 받아서 확인
            String responseString = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            
            log.info("Veo3 API 원시 응답: {}", responseString);
            
            // JSON 파싱
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Veo3OperationResponse operationResponse;
            try {
                operationResponse = mapper.readValue(responseString, Veo3OperationResponse.class);
            } catch (Exception e) {
                log.error("JSON 파싱 실패: {}", e.getMessage(), e);
                log.error("응답 본문: {}", responseString);
                return null;
            }
            
            if (operationResponse == null || operationResponse.name() == null) {
                log.error("Veo3 API 작업 생성 실패 - 응답이 null이거나 name이 없습니다. 응답: {}", responseString);
                return null;
            }
            
            log.info("Veo3 API 작업 생성 성공 - 작업 이름: {}", operationResponse.name());


            // 작업 완료 대기 (폴링)
            // Google Cloud AI Platform 문서에 따르면 작업 상태 확인은 POST 요청을 사용하고
            // 원본 operation name을 그대로 사용해야 함
            String operationName = operationResponse.name();
            log.info("작업 이름 (원본, 그대로 사용): {}", operationName);
            
            Veo3OperationResponse operation = operationResponse;

            String lastOperationResponseString = null; // 마지막 작업 상태 응답 저장
            int maxAttempts = 60; // 최대 10분 대기 (10초 * 60)
            int attempt = 0;
            
            while ((operation.done() == null || !operation.done()) && attempt < maxAttempts) {
                log.info("영상 생성 대기 중... (시도 {}/{})", attempt + 1, maxAttempts);
                try {
                    Thread.sleep(10000); // 10초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("작업 대기 중단됨", e);
                    return null;
                }
                
                // 작업 상태 확인
                // Google Cloud AI Platform 문서에 따르면 POST 요청을 사용하고
                // 요청 본문에 operationName을 포함해야 함
                // URL: https://{location}-aiplatform.googleapis.com/v1/projects/{project}/locations/{location}/publishers/google/models/{model}:fetchPredictOperation
                String getOperationUrl = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:fetchPredictOperation",
                    location, projectId, location, modelId
                );
                
                // 요청 본문에 operationName 포함
                Map<String, String> statusRequestBody = new HashMap<>();
                statusRequestBody.put("operationName", operationName);
                
                log.info("작업 상태 확인 URL: {}", getOperationUrl);
                log.info("작업 상태 확인 요청 본문: {}", statusRequestBody);
                
                try {
                    // POST 요청으로 작업 상태 확인
                    String operationResponseString = restClient.post()
                            .uri(getOperationUrl)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(statusRequestBody)
                            .retrieve()
                            .body(String.class);
                    
                    log.info("작업 상태 응답 (원본): {}", operationResponseString);
                    lastOperationResponseString = operationResponseString; // 마지막 응답 저장
                    
                    // JSON 파싱
                    try {
                        operation = mapper.readValue(operationResponseString, Veo3OperationResponse.class);
                    } catch (Exception e) {
                        log.error("작업 상태 응답 파싱 실패: {}", e.getMessage(), e);
                        log.error("파싱 실패한 응답: {}", operationResponseString);
                        return null;
                    }
                    
                    if (operation == null) {
                        log.error("작업 상태 확인 실패 - 응답이 null입니다");
                        return null;
                    }
                    
                    log.info("작업 상태 파싱 결과 - done: {}, response null 여부: {}", 
                        operation.done(), operation.response() == null);
                    
                    // 응답 구조 디버깅
                    if (operation.response() != null) {
                        log.info("응답 구조 - type: {}, raiMediaFilteredCount: {}, videos null 여부: {}, videos 크기: {}", 
                            operation.response().type(), 
                            operation.response().raiMediaFilteredCount(), 
                            operation.response().videos() == null,
                            operation.response().videos() != null ? operation.response().videos().size() : 0);
                    } else {
                        log.warn("operation.response()가 null입니다. done: {}", operation.done());
                    }
                } catch (RestClientResponseException e) {
                    log.error("작업 상태 확인 실패 - HTTP {}: {}", 
                        e.getStatusCode().value(), e.getResponseBodyAsString(), e);
                    return null;
                }
                
                attempt++;
            }
            
            if (operation.done() == null || !operation.done()) {
                log.error("영상 생성 시간 초과 (최대 {}초 대기)", maxAttempts * 10);
                return null;
            }

            // 완료된 작업에서 영상 정보 추출
            if (operation.response() == null) {
                log.error("작업 응답이 없습니다. 응답: {}", operation);
                if (lastOperationResponseString != null) {
                    log.error("마지막 원본 응답: {}", lastOperationResponseString);
                }
                return null;
            }
            
            // 응답에서 Base64 인코딩된 영상 데이터 추출
            String base64EncodedVideo = extractBase64EncodedVideo(operation, lastOperationResponseString);

            if (base64EncodedVideo == null || base64EncodedVideo.isEmpty()) {
                log.error("Base64 인코딩된 영상 데이터를 추출할 수 없습니다. 응답: {}", operation);
                return null;
            }

            // Base64 디코딩
            log.info("Base64 디코딩 시작");
            byte[] videoData;
            try {
                videoData = Base64.getDecoder().decode(base64EncodedVideo);
                log.info("Base64 디코딩 완료 - 크기: {} bytes", videoData.length);
            } catch (IllegalArgumentException e) {
                log.error("Base64 디코딩 실패: {}", e.getMessage(), e);
                return null;
            }

            if (videoData == null || videoData.length == 0) {
                log.error("디코딩된 영상 데이터가 비어있습니다.");
                return null;
            }
            
            // S3에 영상 업로드
            String fileName = (storeName != null && !storeName.isEmpty()) ?
                storeName.replaceAll("[^a-zA-Z0-9가-힣]", "_") + "_video_" + System.currentTimeMillis() + ".mp4" :
                "generated_video_" + System.currentTimeMillis() + ".mp4";
            String videoUrl = s3Service.uploadFile(videoData, fileName, "video/mp4");

            log.info("Veo3 API 영상 생성 및 S3 업로드 완료 - S3 URL: {} (파일명: {}, 크기: {} bytes)", 
                videoUrl, fileName, videoData.length);
            log.info("이 URL은 Store의 videoUrl 필드에 저장됩니다.");
            return videoUrl;
            
        } catch (RestClientResponseException e) {
            log.error("Veo3 API 호출 실패 - HTTP {}: {}", 
                e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return null;
        } catch (RestClientException e) {
            log.error("Veo3 API 통신 오류", e);
            return null;
        } catch (Exception e) {
            log.error("Veo3 API 영상 생성 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 작업 응답에서 Base64 인코딩된 영상 데이터를 추출합니다.
     */
    private String extractBase64EncodedVideo(Veo3OperationResponse operation, String originalJsonResponse) {
        if (operation == null) {
            log.error("작업 응답이 null입니다");
            return null;
        }
        
        log.info("Base64 인코딩된 영상 데이터 추출 시도 시작");
        log.info("operation.response() null 여부: {}", operation.response() == null);
        
        if (operation.response() == null) {
            log.error("operation.response()가 null입니다");
            if (originalJsonResponse != null) {
                log.error("원본 JSON 응답: {}", originalJsonResponse);
            }
            return null;
        }
        
        log.info("operation.response() 구조 - type: {}, raiMediaFilteredCount: {}, videos: {}", 
            operation.response().type(), 
            operation.response().raiMediaFilteredCount(), 
            operation.response().videos());
        
        // 실제 응답 구조 - videos 배열에서 bytesBase64Encoded 추출
        if (operation.response().videos() == null) {
            log.error("operation.response().videos()가 null입니다");
            return null;
        }
        
        if (operation.response().videos().isEmpty()) {
            log.error("operation.response().videos()가 비어있습니다");
            return null;
        }
        
        Veo3VideoInfo videoInfo = operation.response().videos().get(0);
        log.info("VideoInfo 구조: {}", videoInfo);
        
        if (videoInfo == null) {
            log.error("videoInfo가 null입니다");
            return null;
        }
        
        if (videoInfo.bytesBase64Encoded() == null || videoInfo.bytesBase64Encoded().isEmpty()) {
            log.error("videoInfo.bytesBase64Encoded()가 null이거나 비어있습니다. videoInfo: {}", videoInfo);
            return null;
        }
        
        log.info("Base64 인코딩된 영상 데이터 추출 성공 (videos[0].bytesBase64Encoded) - 길이: {} 문자", 
            videoInfo.bytesBase64Encoded().length());
        return videoInfo.bytesBase64Encoded();
    }
    
    /**
     * 이미지 URL에서 이미지 데이터를 다운로드합니다.
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            return restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", imageUrl, e);
            throw new RuntimeException("이미지 다운로드 실패: " + imageUrl, e);
        }
    }
    
    /**
     * Veo3 API Operation 응답 DTO
     */
    private record Veo3OperationResponse(
            String name, // 작업 이름
            Boolean done, // 작업 완료 여부
            Veo3OperationResult response // 작업 결과
    ) {
    }
    
    /**
     * 작업 결과 (실제 응답 구조)
     */
    private record Veo3OperationResult(
            @com.fasterxml.jackson.annotation.JsonProperty("@type") String type, // @type 필드
            Integer raiMediaFilteredCount, // raiMediaFilteredCount 필드
            List<Veo3VideoInfo> videos // videos 배열
    ) {
    }
    
    /**
     * 실제 응답 구조: videos 배열의 각 항목
     */
    private record Veo3VideoInfo(
            String bytesBase64Encoded, // Base64 인코딩된 영상 데이터
            String mimeType // MIME 타입
    ) {
    }
}
