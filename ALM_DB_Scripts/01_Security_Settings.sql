-- 01_Security_Settings.sql

CREATE TABLE user_security (
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    is_first_login BOOLEAN DEFAULT TRUE,
    last_login_date DATETIME,
    PRIMARY KEY (password_hash)
);