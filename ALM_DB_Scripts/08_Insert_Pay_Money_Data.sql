-- 1. 통장(계좌) 종류에 '선불충전금' 추가
INSERT INTO account_type (type_name) VALUES 
('선불충전금(페이)'),
('포인트/캐시');

-- 2. 은행(금융사) 목록에 핀테크/커머스 플랫폼 추가
INSERT INTO account_bank (bank_name) VALUES 
('카카오페이'),
('토스페이'),
('네이버페이'),
('쿠팡페이'),
('무신사'),
('SSG페이(쓱페이)');