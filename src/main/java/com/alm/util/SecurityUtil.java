package com.alm.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * [Util 계층 - 보안 유틸리티]
 * SHA-256 해시 알고리즘을 사용하여 평문 비밀번호를 단방향 암호화하는 유틸리티 클래스.
 *
 * [SHA-256 개념]
 *   - SHA(Secure Hash Algorithm)-256: 미국 NSA가 설계한 단방향 해시 함수.
 *   - 단방향(One-way): 해시값으로 원본 복원 불가. 같은 입력은 항상 같은 출력 보장(결정론적).
 *   - 출력: 256비트(32바이트) → 16진수(Hexadecimal) 문자열로 표현 시 64자리.
 *   - 용도: 비밀번호를 평문으로 DB에 저장하지 않기 위해 해시값만 저장.
 *   - 로그인 검증: 입력 비밀번호를 해시 → DB의 해시와 비교 (평문 비교 불필요).
 *
 * [한계점] 솔트(Salt) 미적용:
 *   - 동일한 비밀번호는 항상 동일한 해시값 → 레인보우 테이블 공격에 취약.
 *   - 실무: BCrypt, Argon2 등 솔트를 자동 포함하는 알고리즘 권장. 학습 목적으로 SHA-256 사용.
 */
public class SecurityUtil {

    /**
     * 평문 비밀번호를 SHA-256으로 해시하여 64자리 16진수 문자열 반환.
     *
     * [Java 문법 개념] java.security.MessageDigest:
     *   - Java 표준 라이브러리(java.security 패키지)의 해시 처리 클래스.
     *   - 팩토리 메서드 패턴: new MessageDigest() 대신 MessageDigest.getInstance("알고리즘명")으로 획득.
     *   - 지원 알고리즘: MD5, SHA-1, SHA-256, SHA-512 등 (Java 8 이상 기본 제공).
     *
     * @param plainText 사용자가 입력한 평문 비밀번호
     * @return 64자리 16진수 SHA-256 해시 문자열
     * @throws RuntimeException SHA-256 알고리즘 미지원 시 (정상 JVM 환경에서는 발생 안 함)
     */
    public static String encryptSHA256(String plainText) {
        try {
            // 1. MessageDigest 인스턴스 획득: SHA-256 알고리즘 지정
            //    NoSuchAlgorithmException: 지정한 알고리즘이 JVM에서 지원되지 않을 때 checked exception.
            //    Java 표준 스펙상 SHA-256은 반드시 지원해야 하므로 실제로는 거의 발생 안 함.
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 2. 평문 문자열 → 바이트 배열(byte[]) 변환
            //    String.getBytes(): 문자열을 JVM 기본 인코딩(UTF-8)으로 바이트 배열화.
            //    해시 함수는 바이트 배열을 입력으로 받음.
            md.update(plainText.getBytes());

            // 3. 해시 연산 수행 → 32바이트(256비트) 바이트 배열 반환
            //    digest(): 지금까지 update()로 추가한 데이터를 해시 연산. 호출 후 상태 초기화.
            byte[] byteData = md.digest();

            // 4. 바이트 배열 → 16진수 문자열 변환
            //    [Java 문법 개념] StringBuffer:
            //      - String은 불변(Immutable)이므로 + 연산마다 새 객체 생성 → 반복 연결 시 비효율.
            //      - StringBuffer: 가변(Mutable) 문자열 버퍼. append()로 기존 객체에 문자열 추가.
            //      - StringBuilder와 차이: StringBuffer는 동기화(synchronized) → 멀티스레드 안전.
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < byteData.length; i++) {
                // byteData[i] & 0xff:
                //   - Java의 byte는 부호 있는 8비트 정수(-128 ~ 127).
                //   - 음수 byte를 양수(0 ~ 255) 범위로 변환하기 위해 0xff(=255)와 비트 AND 연산.
                //   - 예: byteData[i]=-1(0xFF) & 0xff(0xFF) = 255(0xFF) → 부호 제거.
                // + 0x100: 변환 결과에 256을 더해 항상 3자리 이상의 16진수로 만듦.
                //   - 예: 255 → 255+256=511(0x1ff) → 16진수 "1ff" → substring(1) → "ff" (2자리 보장)
                //   - 이 트릭 없이 직접 변환하면 1자리 값(예: 0xf)이 "f"로 나와 2자리 "0f"가 안 됨.
                // Integer.toString(n, 16): 정수 n을 16진수(radix=16) 문자열로 변환.
                // substring(1): 앞에서 더한 0x100의 "1" 자리를 제거해 2자리 16진수만 추출.
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            // 5. 64자리 16진수 해시 문자열 반환
            //    StringBuffer.toString(): StringBuffer에 담긴 내용을 String으로 변환.
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // RuntimeException: unchecked exception. 호출자가 try-catch 없이도 컴파일 가능.
            // 이 에러는 JVM 환경 자체 문제이므로 복구 불가 → RuntimeException으로 감싸 던짐.
            throw new RuntimeException("보안 에러: 시스템이 SHA-256을 지원하지 않습니다.");
        }
    }
}
