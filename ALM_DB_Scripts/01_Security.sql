-- ═══════════════════════════════════════════════════════════════════════
--  01_Security.sql  |  보안 및 인증 테이블 생성
--
--  [설계]
--    ALM 은 1인용 로컬 애플리케이션입니다.
--    별도 회원 테이블 없이 마스터 비밀번호 1개만 존재하며,
--    이 테이블에는 항상 행이 정확히 1개만 들어갑니다.
--
--  [보안 원칙]
--    비밀번호 평문을 절대 저장하지 않습니다.
--    Java SecurityUtil 에서 SHA-256 단방향 해시로 변환 후 64자리 16진수 문자열만 DB 에 저장합니다.
--    해시는 역산 불가 → DB 유출 시에도 원본 비밀번호를 알 수 없습니다.
-- ═══════════════════════════════════════════════════════════════════════

USE alm_db;

-- password_hash 자체를 PK 로 사용 — 행이 1개뿐이므로 AUTO_INCREMENT ID 불필요
-- VARCHAR(255) : SHA-256 결과는 64자이지만 여유를 두어 다른 알고리즘 교체 대비
CREATE TABLE user_security (
    password_hash   VARCHAR(255) NOT NULL,  -- SHA-256 해시값 (64자 16진수)
    last_login_date DATETIME,               -- 마지막 로그인 일시 (최초엔 NULL)
    PRIMARY KEY (password_hash)
);

SELECT '✅ 01 보안 테이블(user_security) 생성 완료' AS result;
