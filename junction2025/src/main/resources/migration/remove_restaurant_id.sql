-- reviews 테이블에서 restaurant_id 컬럼 제거
-- 이 스크립트는 Restaurant에서 Store로 변경된 후 남아있는 restaurant_id 컬럼을 제거합니다.

-- 1. 먼저 restaurant_id 컬럼이 있는지 확인
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'teamgo' AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'restaurant_id';

-- 2. restaurant_id 컬럼이 있다면 제거
ALTER TABLE reviews DROP COLUMN IF EXISTS restaurant_id;

-- 3. store_id 컬럼이 없다면 추가 (이미 있다면 무시됨)
-- ALTER TABLE reviews ADD COLUMN store_id BIGINT NULL;

-- 4. store_id에 외래 키 제약 조건 추가 (이미 있다면 무시됨)
-- ALTER TABLE reviews ADD CONSTRAINT fk_reviews_store 
--     FOREIGN KEY (store_id) REFERENCES stores(id);

-- 5. store_id를 NOT NULL로 변경 (데이터가 있다면 먼저 마이그레이션 필요)
-- ALTER TABLE reviews MODIFY COLUMN store_id BIGINT NOT NULL;

