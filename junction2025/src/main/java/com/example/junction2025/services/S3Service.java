package com.example.junction2025.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.UUID;

/**
 * AWS S3 파일 업로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    
    @Value("${app.s3.bucket}")
    private String bucketName;
    
    @Value("${app.s3.region}")
    private String region;
    
    @Value("${app.s3.access-key:}")
    private String accessKey;
    
    @Value("${app.s3.secret-key:}")
    private String secretKey;
    
    /**
     * 파일을 S3에 업로드하고 URL을 반환합니다.
     *
     * @param inputStream 파일 입력 스트림
     * @param fileName 파일명
     * @param contentType 컨텐츠 타입 (예: "video/mp4", "image/jpeg")
     * @return S3 URL
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        try {
            // InputStream을 byte 배열로 변환
            byte[] data = inputStream.readAllBytes();
            return uploadFile(data, fileName, contentType);
            
        } catch (Exception e) {
            log.error("InputStream 읽기 실패: {}", e.getMessage(), e);
            throw new RuntimeException("InputStream 읽기 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 바이트 배열을 S3에 업로드하고 URL을 반환합니다.
     *
     * @param data 파일 데이터
     * @param fileName 파일명
     * @param contentType 컨텐츠 타입
     * @return S3 URL
     */
    public String uploadFile(byte[] data, String fileName, String contentType) {
        try {
            S3Client s3Client = createS3Client();
            
            // 고유한 파일명 생성
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            String key = "videos/" + uniqueFileName;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            
            // S3 URL 생성
            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            
            log.info("S3 업로드 성공: {}", url);
            return url;
            
        } catch (S3Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("S3 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("S3 업로드 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    private S3Client createS3Client() {
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
        } else {
            // 환경 변수나 IAM 역할을 사용하는 경우
            return S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }
    }
}

