CREATE TABLE IF NOT EXISTS book_vote (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    vote_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT pk_book_vote PRIMARY KEY (id),
    CONSTRAINT uk_book_vote_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_book_vote_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_book_vote_book FOREIGN KEY (book_id) REFERENCES book (id),
    INDEX idx_book_vote_book_id (book_id)
);
