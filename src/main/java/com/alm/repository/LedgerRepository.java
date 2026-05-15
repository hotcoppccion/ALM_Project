package com.alm.repository;

import com.alm.dto.GeneralLedgerDTO;
import com.alm.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Repository 계층 - DAO]
 * ledger_master 및 general_ledger 테이블에 직접 접근하여 SQL을 실행하는 클래스.
 *
 * [Java 문법 개념] try-with-resources 구문:
 *   - try (Resource r = new Resource()) { ... }
 *   - try 블록이 끝나면 r.close()가 자동 호출됨. AutoCloseable 인터페이스 구현체에만 적용.
 *   - Connection, PreparedStatement, ResultSet이 모두 AutoCloseable을 구현함.
 *   - 이 구문을 사용하면 finally 블록에서 직접 close()를 호출할 필요 없음.
 *   - DB 연결은 유한한 자원이므로, 미반환 시 Connection Pool 고갈 발생 → 반드시 close 필요.
 *
 * [Pure JDBC 설계]
 *   - ORM(JPA/MyBatis) 없이 java.sql 패키지만 사용하여 DB와 직접 통신.
 *   - PreparedStatement: SQL 문자열에 ? 플레이스홀더를 사용하여 파라미터를 바인딩.
 *     → SQL Injection 공격 방지. 문자열 직접 연결(+ 연산자) 방식은 사용하지 않음.
 */
public class LedgerRepository {

    // ─────────────────────────────────────────────────────────────────
    // INSERT
    // ─────────────────────────────────────────────────────────────────

    /**
     * ledger_master 테이블에 부모 레코드를 먼저 삽입하고, DB가 발급한 AUTO_INCREMENT ID를 반환.
     * [Class Table Inheritance 패턴]
     *   general_ledger의 ledger_id는 AUTO_INCREMENT가 아닌 PK/FK.
     *   반드시 ledger_master에서 ID를 먼저 발급받은 뒤, 그 ID를 자식 테이블 삽입에 사용해야 함.
     *
     * @param typeCode 내역 유형 코드. 일반 지출입은 "GEN" 전달.
     * @return DB가 자동 생성한 ledger_id. 실패 시 -1 반환.
     */
    public int insertLedgerMaster(String typeCode) {
        // Statement.RETURN_GENERATED_KEYS: INSERT 후 AUTO_INCREMENT로 생성된 PK 값을 반환하도록 요청하는 상수
        String sql = "INSERT INTO ledger_master (type_code, created_at) VALUES (?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // setString(컬럼 순서, 값): PreparedStatement의 1번째 ? 플레이스홀더에 typeCode 바인딩
            pstmt.setString(1, typeCode);
            pstmt.executeUpdate(); // DML(INSERT) 실행. 반환값은 영향받은 행 수(int).

            // getGeneratedKeys(): INSERT로 생성된 AUTO_INCREMENT 키 값을 ResultSet으로 반환
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                // rs.next(): 결과셋의 커서를 다음 행으로 이동. 데이터가 있으면 true 반환.
                if (rs.next()) return rs.getInt(1); // 1번째 컬럼(생성된 PK)을 int로 반환
            }
        } catch (SQLException e) {
            // System.err: 표준 에러 스트림. 콘솔에 빨간색으로 출력됨. 일반 출력(System.out)과 구분.
            System.err.println("[ERROR] ledger_master 생성 실패: " + e.getMessage());
        }
        return -1; // 실패를 나타내는 sentinel 값
    }

    /**
     * general_ledger 테이블에 실제 거래 내역을 삽입.
     * insertLedgerMaster()로 얻은 ledger_id가 dto에 세팅되어 있어야 함.
     *
     * @param dto ledger_id, asset_id, category_id, amount, transaction_date가 채워진 DTO
     * @return 삽입 성공 시 true, 실패 시 false
     */
    public boolean insertGeneralLedger(GeneralLedgerDTO dto) {
        // ledger_id: PK이자 FK (ledger_master 참조). AUTO_INCREMENT가 아니므로 직접 지정.
        String sql = "INSERT INTO general_ledger (ledger_id, asset_id, category_id, amount, transaction_date) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dto.getLedger_id());         // ledger_master에서 발급받은 ID
            pstmt.setLong(2, dto.getAsset_id());          // 연동 자산 ID (FK)
            pstmt.setInt(3, dto.getCategory_id());        // 카테고리 ID (FK)
            pstmt.setLong(4, dto.getAmount());            // 부호 포함 금액 (양수=수입, 음수=지출)
            // java.sql.Date.valueOf(): "YYYY-MM-DD" 형식 String → java.sql.Date 타입으로 변환
            // java.sql.Date는 java.util.Date의 서브클래스. JDBC에서 SQL DATE 타입과 매핑됨.
            pstmt.setDate(5, java.sql.Date.valueOf(dto.getTransaction_date()));

            // executeUpdate() > 0: 영향받은 행이 1개 이상이면 true → 삽입 성공
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 저장 실패: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SELECT
    // ─────────────────────────────────────────────────────────────────

    /**
     * 모든 일반 지출입 내역을 최신 날짜 순으로 조회.
     * [SQL JOIN 설계]
     *   - LEFT JOIN ledger_category: 카테고리 이름을 category_id → category_name으로 변환
     *   - LEFT JOIN account_table + account_bank: ACC 타입 자산명 조회
     *   - LEFT JOIN cash_asset: CSH 타입 자산명 조회
     *   - COALESCE(a, b): a가 NULL이면 b를 반환하는 MySQL 함수. ACC/CSH 중 어느 쪽인지 자동 선택.
     *   - SUBSTR(acc_number, -4): acc_number의 뒤 4자리 추출. 보안상 전체 번호 미노출.
     *
     * @return GeneralLedgerDTO 리스트 (조회 결과 없으면 빈 ArrayList 반환)
     */
    public List<GeneralLedgerDTO> findAllGeneralLedger() {
        // List: 순서가 있는 컬렉션 인터페이스. ArrayList: 동적 배열로 구현된 List.
        List<GeneralLedgerDTO> list = new ArrayList<>();

        // 멀티라인 SQL 문자열: Java에서는 + 연산자로 문자열을 이어붙임.
        // 가독성을 위해 각 절(SELECT, FROM, JOIN, WHERE, ORDER BY)을 개행으로 구분.
        String sql = "SELECT gl.ledger_id, lm.type_code, gl.asset_id, gl.category_id, " +
                     "gl.amount, DATE_FORMAT(gl.transaction_date, '%Y-%m-%d') AS transaction_date, " +
                     "lc.category_name, " +
                     "COALESCE(CONCAT(ab.bank_name, ' (', SUBSTR(at2.acc_number, -4), ')'), ca.name) AS asset_name " +
                     "FROM general_ledger gl " +
                     "JOIN ledger_master lm ON gl.ledger_id = lm.ledger_id " +
                     "LEFT JOIN ledger_category lc ON gl.category_id = lc.category_id " +
                     "LEFT JOIN account_table at2 ON gl.asset_id = at2.asset_id " +
                     "LEFT JOIN account_bank ab ON at2.bank_id = ab.bank_id " +
                     "LEFT JOIN cash_asset ca ON gl.asset_id = ca.asset_id " +
                     "ORDER BY gl.transaction_date DESC, gl.ledger_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) { // SELECT는 executeQuery() 사용 (executeUpdate() 아님)

            // while(rs.next()): ResultSet 커서를 한 행씩 이동하며 데이터가 있는 동안 반복
            while (rs.next()) {
                GeneralLedgerDTO dto = new GeneralLedgerDTO(); // 행마다 새 DTO 객체 생성

                // rs.getXxx("컬럼명"): ResultSet에서 해당 컬럼의 값을 지정 타입으로 추출
                dto.setLedger_id(rs.getInt("ledger_id"));
                dto.setType_code(rs.getString("type_code"));
                dto.setAsset_id(rs.getLong("asset_id"));
                dto.setCategory_id(rs.getInt("category_id"));
                dto.setAmount(rs.getLong("amount"));
                dto.setTransaction_date(rs.getString("transaction_date")); // DATE_FORMAT으로 이미 문자열화됨
                dto.setCategory_name(rs.getString("category_name"));
                dto.setAsset_name(rs.getString("asset_name"));

                list.add(dto); // List에 DTO 추가
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /**
     * 삭제 전 잔액 복원을 위해 특정 내역의 asset_id와 amount를 단건 조회.
     * 삭제 후에는 데이터가 사라지므로, 반드시 삭제 전에 호출해야 함.
     *
     * @param ledgerId 조회할 내역의 ledger_id
     * @return GeneralLedgerDTO (asset_id, amount만 채워짐). 없으면 null.
     */
    public GeneralLedgerDTO findGeneralLedgerById(int ledgerId) {
        String sql = "SELECT ledger_id, asset_id, amount FROM general_ledger WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) { // 결과가 1행 존재하면
                    GeneralLedgerDTO dto = new GeneralLedgerDTO();
                    dto.setLedger_id(rs.getInt("ledger_id"));
                    dto.setAsset_id(rs.getLong("asset_id"));
                    dto.setAmount(rs.getLong("amount"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] general_ledger 단건 조회 실패: " + e.getMessage());
        }
        return null; // 해당 ID의 내역이 없을 경우 null 반환
    }

    /**
     * [기획서 5.3절] 이달(현재 연월 기준) 수입 합계와 지출 합계를 카테고리별로 집계하여 반환.
     * 기획서 메서드명: getSumByCategory(). 카테고리 구분 합산 확장을 고려한 이름.
     * 현재 구현은 전체 합계만 반환하며, 향후 GROUP BY category_id로 카테고리별 분리 가능.
     *
     * [SQL 집계 함수]
     *   - SUM(): 컬럼 값의 합계. NULL은 자동으로 무시됨.
     *   - CASE WHEN: SQL의 조건 분기문 (Java의 if-else와 동일한 역할).
     *   - ABS(): 절댓값 함수. 지출(음수)을 양수로 변환하여 표출.
     *   - YEAR()/MONTH(): 날짜 컬럼에서 연도/월 추출 함수.
     *   - CURDATE(): MySQL의 현재 날짜 반환 함수.
     *   - COALESCE(SUM(...), 0): SUM 결과가 NULL(내역 없음)일 때 0으로 대체.
     *
     * @return Map<String, Long> with keys: "totalIncome", "totalExpense", "net"
     */
    public Map<String, Long> getSumByCategory() {
        Map<String, Long> result = new HashMap<>(); // HashMap: key-value 쌍을 저장하는 자료구조

        String sql = "SELECT " +
                     "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS total_income, " +
                     "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) AS total_expense " +
                     "FROM general_ledger " +
                     "WHERE YEAR(transaction_date) = YEAR(CURDATE()) " +
                     "AND MONTH(transaction_date) = MONTH(CURDATE())";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long income  = rs.getLong("total_income");
                long expense = rs.getLong("total_expense");
                result.put("totalIncome",  income);
                result.put("totalExpense", expense);
                result.put("net",          income - expense); // 순수지 = 수입 - 지출
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 월별 요약 조회 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * 가계부 내역 추가 폼의 카테고리 드롭다운 목록을 반환.
     *
     * @return [{"category_id": 1, "category_name": "식비"}, ...] 형태의 Map 리스트
     */
    public List<Map<String, Object>> findAllCategories() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name FROM ledger_category ORDER BY category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                // Map<String, Object>: 컬럼명(String)과 값(Object)을 쌍으로 저장
                // Object 타입 사용 이유: category_id(int)와 category_name(String)이 혼재하므로
                Map<String, Object> row = new HashMap<>();
                row.put("category_id",   rs.getInt("category_id"));
                row.put("category_name", rs.getString("category_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 카테고리 목록 조회 실패: " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────

    /**
     * ledger_master에서 레코드를 삭제.
     * [ON DELETE CASCADE 동작]
     *   - SQL 스키마(03_Ledger_Domain.sql)에서 general_ledger의 ledger_id FK가 CASCADE로 설정됨.
     *   - ledger_master 레코드 삭제 시 general_ledger의 연결된 행도 DB가 자동으로 함께 삭제.
     *   - 따라서 general_ledger를 별도로 DELETE할 필요 없음.
     *
     * @param ledgerId 삭제할 내역의 ledger_id
     * @return 삭제 성공 시 true
     */
    public boolean deleteLedgerMaster(int ledgerId) {
        String sql = "DELETE FROM ledger_master WHERE ledger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ledgerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERROR] ledger_master 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 자산 잔액 조정 (트랜잭션 연동 핵심 로직)
    // ─────────────────────────────────────────────────────────────────

    /**
     * 가계부 거래 발생 시 연동 자산(account_table 또는 cash_asset)의 잔액을 delta만큼 증감.
     * [UPDATE balance = balance + delta 설계]
     *   - 현재 잔액을 Java로 가져와 계산 후 재저장하는 방식이 아닌, SQL 수식으로 직접 처리.
     *   - 이유: Java에서 읽고 → 계산하고 → 쓰는 3단계 사이에 다른 요청이 끼어들면 잔액이 틀림.
     *     (Race Condition / 경쟁 상태 문제). SQL 단일 문장 업데이트는 DB 내부에서 원자적(Atomic) 처리됨.
     *   - 수입 등록: delta = +amount (양수). 지출 등록: delta = -amount (음수).
     *   - 내역 삭제 시 복원: delta = -savedAmount (원래 delta의 반대 부호).
     *
     * [ACC/CSH 구분 없이 두 테이블 모두 시도하는 이유]
     *   - asset_id만 알고 있을 때, 해당 자산이 ACC인지 CSH인지 별도 조회 없이 처리 가능.
     *   - account_table에 해당 asset_id가 없으면 WHERE 조건 불일치로 0행 업데이트 → 무해함.
     *   - cash_asset도 동일 방식. 둘 중 하나만 실제로 업데이트됨.
     *
     * @param assetId   잔액을 조정할 자산의 asset_id
     * @param delta     증감액. 양수면 잔액 증가, 음수면 잔액 감소.
     * @return 두 테이블 중 하나라도 업데이트되면 true
     */
    public boolean adjustAssetBalance(long assetId, long delta) {
        boolean updated = false;

        // 1. account_table 시도 (ACC 타입 자산)
        String accSql = "UPDATE account_table SET balance = balance + ? WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(accSql)) {
            pstmt.setLong(1, delta);
            pstmt.setLong(2, assetId);
            // executeUpdate() 반환값: 실제로 업데이트된 행의 수
            if (pstmt.executeUpdate() > 0) updated = true;
        } catch (SQLException e) {
            System.err.println("[ERROR] account_table 잔액 조정 실패: " + e.getMessage());
        }

        // 2. cash_asset 시도 (CSH 타입 자산)
        // account_table에서 이미 updated=true라도 cash_asset 시도는 영향 없음 (asset_id 불일치로 0행)
        String cshSql = "UPDATE cash_asset SET balance = balance + ? WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cshSql)) {
            pstmt.setLong(1, delta);
            pstmt.setLong(2, assetId);
            if (pstmt.executeUpdate() > 0) updated = true;
        } catch (SQLException e) {
            System.err.println("[ERROR] cash_asset 잔액 조정 실패: " + e.getMessage());
        }

        return updated;
    }

    /**
     * 가계부에서 사용 가능한 자산(ACC + CSH 타입)의 목록을 반환.
     * 가계부 내역 등록 폼에서 "연동할 자산" 드롭다운 목록에 사용.
     * REA(부동산)와 PHY(실물)는 유동 잔액이 없으므로 제외.
     *
     * @return [{"asset_id": 1, "display_name": "카카오뱅크 (4567)", "type_code": "ACC"}, ...] 리스트
     */
    public List<Map<String, Object>> findLiquidAssets() {
        List<Map<String, Object>> list = new ArrayList<>();

        // ACC 타입: 은행명 + 계좌번호 뒤 4자리로 표시명 구성
        String accSql = "SELECT m.asset_id, m.type_code, " +
                        "CONCAT(b.bank_name, ' (', SUBSTR(a.acc_number, -4), ')') AS display_name " +
                        "FROM asset_master m " +
                        "JOIN account_table a ON m.asset_id = a.asset_id " +
                        "JOIN account_bank b ON a.bank_id = b.bank_id " +
                        "WHERE m.type_code = 'ACC' ORDER BY m.asset_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(accSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("asset_id",     rs.getLong("asset_id"));
                row.put("type_code",    rs.getString("type_code"));
                row.put("display_name", rs.getString("display_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] ACC 자산 목록 조회 실패: " + e.getMessage());
        }

        // CSH 타입: cash_asset.name 필드를 표시명으로 사용
        String cshSql = "SELECT m.asset_id, m.type_code, c.name AS display_name " +
                        "FROM asset_master m JOIN cash_asset c ON m.asset_id = c.asset_id " +
                        "WHERE m.type_code = 'CSH' ORDER BY m.asset_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cshSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("asset_id",     rs.getLong("asset_id"));
                row.put("type_code",    rs.getString("type_code"));
                row.put("display_name", rs.getString("display_name"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] CSH 자산 목록 조회 실패: " + e.getMessage());
        }

        return list;
    }

    /**
     * 특정 asset_id를 참조하는 general_ledger 내역이 존재하는지 확인.
     * DeleteValidatorService에서 자산 삭제 가능 여부 판단 시 사용.
     *
     * @param assetId 확인할 자산의 asset_id
     * @return 참조 내역이 1개 이상 존재하면 true
     */
    public boolean existsByAssetId(long assetId) {
        String sql = "SELECT COUNT(*) FROM general_ledger WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, assetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                // COUNT(*): 조건을 만족하는 행의 총 개수를 반환하는 집계 함수.
                // 결과는 항상 1행 1컬럼. 값이 0이면 참조 없음, 1 이상이면 참조 있음.
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] 가계부 참조 여부 확인 실패: " + e.getMessage());
        }
        return false;
    }
}
