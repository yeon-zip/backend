-- DB 기반 알림 기능 수동 적용 SQL
--
-- 이 SQL은 서버 DB에 알림 받기 설정과 사용자별 알림 기록을 저장하기 위한
-- 테이블 생성 SQL입니다. 운영 배포 전 대상 DB에 반드시 적용해야 합니다.
--
-- 포함 테이블:
-- - notification_subscription: 사용자가 "특정 도서가 특정 도서관에서 대출 가능해지면 알려줘"라고 등록한 알림 받기 설정
-- - user_notification: 스케줄러가 대출 가능 상태 전이를 감지했을 때 생성하는 사용자별 알림 기록
--
-- 중복 방지 핵심 제약:
-- - notification_subscription: (user_id, book_id, library_id) unique
-- - user_notification: (subscription_id, notification_type, notification_date) unique
--
-- 현재 프로젝트에는 Flyway/Liquibase 자동 migration이 없으므로 운영/스테이징 배포 시
-- 이 SQL을 수동으로 적용했는지 배포 체크리스트에서 확인해야 합니다.
--
-- 운영 전 검토 사항:
-- - 다중 인스턴스에서 스케줄러를 운영하기 전에는 분산락 도입을 검토해야 합니다.
-- - 알림 구독 규모가 커지면 도서관 정보나루 API 호출량 제어(rate limit/backoff/metrics)를 검토해야 합니다.
-- - Flyway/Liquibase 등 자동 migration 도입 여부는 별도 작업으로 검토합니다.

CREATE TABLE IF NOT EXISTS notification_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    library_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    last_stable_availability VARCHAR(30) NOT NULL,
    last_check_outcome VARCHAR(30) NOT NULL,
    last_checked_at DATETIME NULL,
    last_notified_at DATETIME NULL,
    last_dispatch_error_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_notification_subscription PRIMARY KEY (id),
    CONSTRAINT uk_notification_subscription_user_book_library UNIQUE (user_id, book_id, library_id),
    CONSTRAINT fk_notification_subscription_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_notification_subscription_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT fk_notification_subscription_library FOREIGN KEY (library_id) REFERENCES library (id),
    INDEX idx_notification_subscription_active_id (active, id),
    INDEX idx_notification_subscription_user_active (user_id, active)
);

CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    library_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    notification_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT pk_user_notification PRIMARY KEY (id),
    CONSTRAINT uk_user_notification_subscription_type_date UNIQUE (subscription_id, notification_type, notification_date),
    CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_notification_subscription FOREIGN KEY (subscription_id) REFERENCES notification_subscription (id),
    CONSTRAINT fk_user_notification_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT fk_user_notification_library FOREIGN KEY (library_id) REFERENCES library (id),
    INDEX idx_user_notification_user_deleted_id (user_id, deleted_at, id)
);
