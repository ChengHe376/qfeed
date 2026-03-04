CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS posts (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     user_id BIGINT NOT NULL,
                                     content TEXT NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     status TINYINT NOT NULL DEFAULT 1,
                                     INDEX idx_posts_created_id (created_at, id),
    INDEX idx_posts_user_created (user_id, created_at, id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS likes (
                                     user_id BIGINT NOT NULL,
                                     post_id BIGINT NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (user_id, post_id)
    ) ENGINE=InnoDB;
