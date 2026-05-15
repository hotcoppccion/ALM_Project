-- 01_Security_Settings.sql

CREATE TABLE user_security (
    password_hash VARCHAR(255) NOT NULL,
    last_login_date DATETIME,
    PRIMARY KEY (password_hash)
);