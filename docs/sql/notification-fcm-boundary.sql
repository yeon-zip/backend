CREATE TABLE IF NOT EXISTS user_push_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    last_confirmed_at DATETIME NOT NULL,
    deactivated_at DATETIME NULL,
    deactivation_reason VARCHAR(100) NULL,
    CONSTRAINT pk_user_push_token PRIMARY KEY (id),
    CONSTRAINT uk_user_push_token_platform_device UNIQUE (platform, device_token),
    CONSTRAINT fk_user_push_token_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    INDEX idx_user_push_token_user_active (user_id, active)
);

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
