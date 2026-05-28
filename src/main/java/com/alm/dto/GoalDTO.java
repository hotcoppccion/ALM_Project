package com.alm.dto;

/**
 * [DTO] 재무 목표 데이터 전달 객체.
 * DB 컬럼 필드 + 화면 표시용 계산 필드를 함께 가진다.
 *
 * 계산 필드 (DB에 저장되지 않음):
 *   - current_amount : 현재 연동 자산의 실제 금액
 *   - achievement_rate : 달성률 % (0~100, 초과 시 100 고정)
 *   - days_remaining  : 마감일까지 남은 일수 (음수면 기한 초과)
 *   - asset_name      : 연동 자산 표시명 (NULL이면 "전체 자산 합계")
 */
public class GoalDTO {

    // ── DB 컬럼 필드 ──────────────────────────────────────
    private int    goal_id;
    private Integer asset_id;       // NULL = 전체 자산 기준
    private String goal_name;
    private long   target_amount;
    private String end_date;        // "YYYY-MM-DD" or null

    // ── 계산 필드 (Service에서 채움) ───────────────────────
    private long   current_amount;
    private int    achievement_rate; // 0~100
    private long   days_remaining;   // 음수 = 기한 초과
    private String asset_name;       // 표시용 자산명

    // ── Getters / Setters ─────────────────────────────────
    public int     getGoal_id()           { return goal_id; }
    public void    setGoal_id(int v)      { this.goal_id = v; }

    public Integer getAsset_id()          { return asset_id; }
    public void    setAsset_id(Integer v) { this.asset_id = v; }

    public String  getGoal_name()         { return goal_name; }
    public void    setGoal_name(String v) { this.goal_name = v; }

    public long    getTarget_amount()     { return target_amount; }
    public void    setTarget_amount(long v){ this.target_amount = v; }

    public String  getEnd_date()          { return end_date; }
    public void    setEnd_date(String v)  { this.end_date = v; }

    public long    getCurrent_amount()    { return current_amount; }
    public void    setCurrent_amount(long v){ this.current_amount = v; }

    public int     getAchievement_rate()  { return achievement_rate; }
    public void    setAchievement_rate(int v){ this.achievement_rate = v; }

    public long    getDays_remaining()    { return days_remaining; }
    public void    setDays_remaining(long v){ this.days_remaining = v; }

    public String  getAsset_name()        { return asset_name; }
    public void    setAsset_name(String v){ this.asset_name = v; }
}
