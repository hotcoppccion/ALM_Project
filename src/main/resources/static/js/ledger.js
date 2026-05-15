/**
 * ledger.js — 가계부 페이지 전용 스크립트
 * 대응 HTML: Ledger.html
 * 대응 CSS : css/Ledger.css
 *
 * [주요 기능]
 *  1. 달력 렌더링: 해당 월의 날짜 그리드 + 일별 수입/지출 합산 금액 표시
 *  2. 날짜 선택 필터: 달력 날짜 클릭 → 오른쪽 패널에 해당일 내역만 출력
 *  3. 월 이동: < > 버튼으로 이전/다음 달 이동
 *  4. 내역 추가 모달: 일반 지출입(GEN) 등록
 *  5. 내역 삭제: 우클릭 컨텍스트 메뉴 → DELETE 요청 + 자산 잔액 복원
 *  6. 탭 전환: 일반지출입 / 고정지출 / 정기수입 / 변동지출
 */

'use strict';

// ══════════════════════════════════════════════════════════
//  전역 상태
// ══════════════════════════════════════════════════════════
let allEntries       = [];    // GET /api/ledger/list 캐시 (클라이언트 필터링에 사용)
let selectedDate     = null;  // 달력에서 선택된 날짜 "YYYY-MM-DD" or null
let selectedLedgerId = null;  // 우클릭으로 선택된 내역 ID
let liquidAssets     = [];    // 자산 목록 캐시 (스케줄 탭 모달 재사용)

/**
 * 달력의 현재 표시 연/월 상태 객체.
 * month: 0-indexed (0 = 1월, 11 = 12월)
 */
const calState = {
    year:  new Date().getFullYear(),
    month: new Date().getMonth()
};

// ══════════════════════════════════════════════════════════
//  숫자 포맷 유틸
// ══════════════════════════════════════════════════════════

/** 절댓값 + "원" 단위 한국어 포맷. 예: 50000 → "50,000원" */
const fmt = (n) => new Intl.NumberFormat('ko-KR').format(Math.abs(n)) + '원';

/**
 * 달력 셀 내부 짧은 표시용 포맷.
 * 100만 이상 → "X.X만", 1만 이상 → "X만", 그 외 → 세자리 쉼표
 */
function shortFmt(n) {
    const abs = Math.abs(n);
    if (abs >= 1000000) return (n / 10000).toFixed(0) + '만';
    if (abs >= 10000)   return (n / 10000).toFixed(1).replace(/\.0$/, '') + '만';
    return new Intl.NumberFormat('ko-KR').format(n);
}

/**
 * 날짜 부분 문자열 "YYYY-MM-DD" 생성.
 * @param {number} year
 * @param {number} month 0-indexed
 * @param {number} day
 */
function toDateStr(year, month, day) {
    return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

// ══════════════════════════════════════════════════════════
//  초기 로드 (DOMContentLoaded)
// ══════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', async () => {
    await loadAllEntries();   // 전체 내역 캐시 로드
    loadSummaryFromAPI();     // 상단 요약 카드
    loadCategories();         // 모달 카테고리 드롭다운
    await loadLiquidAssets(); // 모달 연동 자산 드롭다운 (캐시 저장)
    renderCalendar();         // 달력 그리기
    renderDetailList();       // 이달 전체 내역 (초기 상태)
    document.getElementById('inp-date').valueAsDate = new Date();
    // 스케줄 탭 초기 로드
    loadFixedRules();
    loadFixedReceipts();
    loadIncomeRules();
    loadIncomeReceipts();
    loadVariableRules();
    loadVariableReceipts();
});

// ══════════════════════════════════════════════════════════
//  탭 전환
// ══════════════════════════════════════════════════════════
/**
 * 탭 버튼 클릭 시 호출.
 * @param {string} tabId  'general' | 'fixed' | 'income' | 'variable'
 * @param {HTMLElement} btn 클릭된 버튼 요소
 */
function switchTab(tabId, btn) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    document.getElementById('tab-' + tabId).classList.add('active');
    btn.classList.add('active');
}

// ══════════════════════════════════════════════════════════
//  공통 유틸 (스케줄 탭)
// ══════════════════════════════════════════════════════════

/** p_unit 영문 코드 → 한글 변환. */
function pUnitLabel(unit) {
    return { DAY: '일', WEEK: '주', MONTH: '개월' }[unit] || unit;
}

/** 모달 열기 공통 함수. */
function openModal(id) {
    document.getElementById(id).classList.add('active');
}

/** 모달 닫기 공통 함수. */
function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// ══════════════════════════════════════════════════════════
//  API 호출
// ══════════════════════════════════════════════════════════

/** 전체 일반 지출입 내역을 캐시(allEntries)에 로드. */
async function loadAllEntries() {
    try {
        const res  = await fetch('/api/ledger/list');
        allEntries = await res.json();
    } catch (e) {
        allEntries = [];
        console.error('[Ledger] 내역 로드 실패', e);
    }
}

/**
 * 상단 요약 카드: /api/ledger/summary 기반.
 * DB에서 현재 달력 월과 무관하게 "시스템 기준 이번 달"을 반환.
 */
async function loadSummaryFromAPI() {
    try {
        const res  = await fetch('/api/ledger/summary');
        const data = await res.json();
        document.getElementById('total-income').textContent  = fmt(data.totalIncome  || 0);
        document.getElementById('total-expense').textContent = fmt(data.totalExpense || 0);
        const net = data.net || 0;
        const el  = document.getElementById('net-amount');
        el.textContent = (net >= 0 ? '+' : '-') + fmt(net);
        el.style.color = net >= 0 ? '#4ade80' : '#f87171';
    } catch (e) {
        console.error('[Ledger] 요약 로드 실패', e);
    }
}

/** 카테고리 드롭다운 초기 로드. */
async function loadCategories() {
    try {
        const res  = await fetch('/api/ledger/categories');
        const list = await res.json();
        document.getElementById('sel-category').innerHTML =
            list.map(c => `<option value="${c.category_id}">${c.category_name}</option>`).join('');
    } catch (e) {
        console.error('[Ledger] 카테고리 로드 실패', e);
    }
}

/** 연동 자산(ACC + CSH) 드롭다운 초기 로드. 캐시(liquidAssets)에도 저장. */
async function loadLiquidAssets() {
    try {
        const res  = await fetch('/api/ledger/liquid-assets');
        liquidAssets = await res.json();
        const sel  = document.getElementById('sel-asset');
        liquidAssets.forEach(a => {
            const opt = document.createElement('option');
            opt.value       = a.asset_id;
            opt.textContent = `[${a.type_code}] ${a.display_name}`;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error('[Ledger] 자산 로드 실패', e);
    }
}

/**
 * 자산 드롭다운 옵션 HTML 생성 (스케줄 모달 재사용).
 * @returns {string} option 요소들의 HTML 문자열
 */
function buildAssetOptions() {
    return liquidAssets.map(a =>
        `<option value="${a.asset_id}">[${a.type_code}] ${a.display_name}</option>`
    ).join('');
}

// ══════════════════════════════════════════════════════════
//  달력 렌더링
// ══════════════════════════════════════════════════════════

/**
 * calState(year, month) 기준으로 달력 그리드를 렌더링.
 * allEntries 캐시에서 해당 월 내역을 날짜별로 집계하여 셀에 표시.
 */
function renderCalendar() {
    const { year, month } = calState;
    document.getElementById('cal-title').textContent = `${year}년 ${month + 1}월`;

    // 이달 내역을 날짜(YYYY-MM-DD)로 그룹핑
    const byDate = {};
    allEntries.forEach(e => {
        const d = new Date(e.transaction_date);
        if (d.getFullYear() === year && d.getMonth() === month) {
            if (!byDate[e.transaction_date]) byDate[e.transaction_date] = [];
            byDate[e.transaction_date].push(e);
        }
    });

    // 달력 계산
    const firstWeekday = new Date(year, month, 1).getDay(); // 0=일
    const daysInMonth  = new Date(year, month + 1, 0).getDate();
    const todayStr     = toDateStr(new Date().getFullYear(), new Date().getMonth(), new Date().getDate());

    let html       = '';
    let dayOffset  = 1 - firstWeekday; // 음수 = 이전 달 날짜

    for (let row = 0; row < 6; row++) {
        html += '<tr>';
        for (let col = 0; col < 7; col++) {
            const cellDate    = new Date(year, month, dayOffset);
            const cellYear    = cellDate.getFullYear();
            const cellMonth   = cellDate.getMonth();
            const cellDay     = cellDate.getDate();
            const dateStr     = toDateStr(cellYear, cellMonth, cellDay);
            const isCurMonth  = (cellMonth === month && cellYear === year);
            const isToday     = (dateStr === todayStr);
            const isSelected  = (dateStr === selectedDate);
            const entries     = byDate[dateStr] || [];

            // CSS 클래스 조립
            let cls = 'cal-cell';
            if (!isCurMonth) cls += ' other-month';
            if (isToday)     cls += ' today';
            if (isSelected)  cls += ' selected';

            // 날짜 숫자: 오늘이면 파란 원
            const dayNumHtml = isToday
                ? `<div class="cal-day-num-wrap"><div class="cal-day-num">${cellDay}</div></div>`
                : `<div class="cal-day-num">${cellDay}</div>`;

            // 금액 표시: 이달 + 내역 있을 때만
            let amtHtml = '';
            if (isCurMonth && entries.length > 0) {
                const net    = entries.reduce((s, e) => s + e.amount, 0);
                const amtCls = net > 0 ? 'cal-income' : net < 0 ? 'cal-expense' : 'cal-mixed';
                amtHtml = `<div class="cal-amount ${amtCls}">${net > 0 ? '+' : ''}${shortFmt(net)}</div>`;
            }

            // 클릭 이벤트: 이달 날짜만 선택 가능
            const clickAttr = isCurMonth ? `onclick="selectDate('${dateStr}')"` : '';
            html += `<td class="${cls}" ${clickAttr}>${dayNumHtml}${amtHtml}</td>`;
            dayOffset++;
        }
        html += '</tr>';
        // 이달 날짜가 모두 렌더링됐고 최소 5행을 채웠으면 종료
        if (dayOffset > daysInMonth && row >= 4) break;
    }

    document.getElementById('cal-body').innerHTML = html;
    updateCalMonthSummary(byDate);
}

/**
 * 달력 하단 월 요약 (이달 수입 / 지출 / 순수지) 업데이트.
 * @param {Object} byDate 날짜별 entry 배열 맵
 */
function updateCalMonthSummary(byDate) {
    let income = 0, expense = 0;
    Object.values(byDate).forEach(entries => {
        entries.forEach(e => {
            if (e.amount > 0) income  += e.amount;
            else              expense += Math.abs(e.amount);
        });
    });
    document.getElementById('cal-income').textContent  = '+' + fmt(income);
    document.getElementById('cal-expense').textContent = '-' + fmt(expense);
    const net   = income - expense;
    const netEl = document.getElementById('cal-net');
    netEl.textContent = (net >= 0 ? '+' : '-') + fmt(net);
    netEl.className   = 'sum-val ' + (net >= 0 ? 'net' : 'expense');
}

// ── 날짜 선택 ─────────────────────────────────────────────
/**
 * 달력 날짜 셀 클릭 시 호출.
 * 같은 날짜 재클릭 → selectedDate 해제 (이달 전체 보기로 복귀).
 * @param {string} dateStr "YYYY-MM-DD"
 */
function selectDate(dateStr) {
    selectedDate = (selectedDate === dateStr) ? null : dateStr;
    renderCalendar();
    renderDetailList();
}

// ── 월 이동 ──────────────────────────────────────────────
function prevMonth() {
    calState.month--;
    if (calState.month < 0) { calState.month = 11; calState.year--; }
    selectedDate = null;
    renderCalendar();
    renderDetailList();
}

function nextMonth() {
    calState.month++;
    if (calState.month > 11) { calState.month = 0; calState.year++; }
    selectedDate = null;
    renderCalendar();
    renderDetailList();
}

// ══════════════════════════════════════════════════════════
//  내역 목록 렌더링 (오른쪽 패널)
// ══════════════════════════════════════════════════════════
/**
 * selectedDate 또는 calState 기준으로 allEntries를 필터링하여 테이블을 렌더링.
 * - selectedDate 있으면 해당일 내역만
 * - selectedDate 없으면 calState(year, month) 이달 전체 내역
 */
function renderDetailList() {
    const { year, month } = calState;
    const labelEl = document.getElementById('detail-label');

    let filtered;
    if (selectedDate) {
        filtered = allEntries.filter(e => e.transaction_date === selectedDate);
        const d  = new Date(selectedDate);
        labelEl.textContent = `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일의 내역`;
    } else {
        filtered = allEntries.filter(e => {
            const d = new Date(e.transaction_date);
            return d.getFullYear() === year && d.getMonth() === month;
        });
        labelEl.textContent = `${year}년 ${month + 1}월 전체 내역`;
    }

    const tbody = document.getElementById('ledger-body');
    if (!filtered || filtered.length === 0) {
        const msg = selectedDate ? '이 날의 내역이 없습니다.' : '이달의 내역이 없습니다.';
        tbody.innerHTML = `<tr class="empty-row"><td colspan="5">${msg}</td></tr>`;
        return;
    }

    // 최신 날짜 → 오래된 날짜 순 정렬
    filtered.sort((a, b) => b.transaction_date.localeCompare(a.transaction_date));

    tbody.innerHTML = filtered.map(item => {
        const isIncome = item.amount > 0;
        const badge    = isIncome
            ? '<span class="badge badge-income">수입</span>'
            : '<span class="badge badge-expense">지출</span>';
        const amtCls   = isIncome ? 'amount-income' : 'amount-expense';
        const amtText  = (isIncome ? '+' : '-') + fmt(item.amount);
        const assetTd  = item.asset_name
            ? item.asset_name
            : '<span style="color:#374151">연동 없음</span>';

        return `
            <tr data-id="${item.ledger_id}"
                oncontextmenu="showCtxMenu(event, ${item.ledger_id})">
                <td>${item.transaction_date}</td>
                <td>${badge}</td>
                <td>${item.category_name || '-'}</td>
                <td>${assetTd}</td>
                <td style="text-align:right" class="${amtCls}">${amtText}</td>
            </tr>`;
    }).join('');
}

// ══════════════════════════════════════════════════════════
//  모달 / 폼 제어
// ══════════════════════════════════════════════════════════

/**
 * 수입 / 지출 토글 버튼 상태 변경.
 * @param {'IN'|'OUT'} dir
 */
function setDirection(dir) {
    document.getElementById('direction-val').value = dir;
    document.getElementById('btn-in').className  = 'dir-btn' + (dir === 'IN'  ? ' active-in'  : '');
    document.getElementById('btn-out').className = 'dir-btn' + (dir === 'OUT' ? ' active-out' : '');
}

/** 내역 추가 모달 열기. 선택된 날짜가 있으면 거래일에 자동 세팅. */
function openAddModal() {
    const dateInput = document.getElementById('inp-date');
    dateInput.value = selectedDate || new Date().toISOString().split('T')[0];
    document.getElementById('add-modal').classList.add('active');
}

/** 내역 추가 모달 닫기 + 입력값 초기화. */
function closeAddModal() {
    document.getElementById('add-modal').classList.remove('active');
    document.getElementById('inp-amount').value = '';
    setDirection('IN');
}

/** 내역 등록 POST 요청. 성공 시 캐시 갱신 → 달력 + 목록 리렌더링. */
async function submitLedger() {
    const amount  = parseInt(document.getElementById('inp-amount').value);
    const dateVal = document.getElementById('inp-date').value;
    if (!amount || amount <= 0) { alert('금액을 올바르게 입력하세요.'); return; }
    if (!dateVal)               { alert('거래일을 선택하세요.'); return; }

    const payload = {
        direction:        document.getElementById('direction-val').value,
        category_id:      parseInt(document.getElementById('sel-category').value),
        asset_id:         parseInt(document.getElementById('sel-asset').value),
        amount:           amount,
        transaction_date: dateVal
    };

    const res    = await fetch('/api/ledger', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(payload)
    });
    const result = await res.json();

    if (res.ok) {
        closeAddModal();
        await loadAllEntries();  // 캐시 갱신
        loadSummaryFromAPI();
        renderCalendar();
        renderDetailList();
    } else {
        alert('등록 실패: ' + result.message);
    }
}

// ══════════════════════════════════════════════════════════
//  우클릭 컨텍스트 메뉴 + 삭제
// ══════════════════════════════════════════════════════════

/**
 * 테이블 행 우클릭 시 컨텍스트 메뉴 표시.
 * @param {MouseEvent} event
 * @param {number} ledgerId
 */
function showCtxMenu(event, ledgerId) {
    event.preventDefault();
    selectedLedgerId = ledgerId;
    const menu = document.getElementById('context-menu');
    menu.style.display = 'block';
    menu.style.left    = event.clientX + 'px';
    menu.style.top     = event.clientY + 'px';
}

// 다른 곳 클릭 시 컨텍스트 메뉴 닫기
document.addEventListener('click', () => {
    document.getElementById('context-menu').style.display = 'none';
});

/**
 * 컨텍스트 메뉴 "삭제" 클릭 시 호출.
 * DELETE /api/ledger/{ledgerId} → 성공 시 캐시 갱신 + 리렌더링.
 */
async function deleteSelected() {
    if (!selectedLedgerId) return;
    if (!confirm('이 내역을 삭제하면 연동 자산 잔액도 복원됩니다.\n삭제하시겠습니까?')) return;

    const res    = await fetch(`/api/ledger/${selectedLedgerId}`, { method: 'DELETE' });
    const result = await res.json();

    if (res.ok) {
        await loadAllEntries();
        loadSummaryFromAPI();
        renderCalendar();
        renderDetailList();
    } else {
        alert('삭제 실패: ' + result.message);
    }
    selectedLedgerId = null;
}

// ══════════════════════════════════════════════════════════
//  카테고리 드롭다운 채우기 (스케줄 모달용)
// ══════════════════════════════════════════════════════════

/**
 * 스케줄 탭 모달의 카테고리 드롭다운을 /api/ledger/categories 로 채운다.
 * DOMContentLoaded 이후에 각 모달 ID를 대상으로 호출.
 */
async function fillScheduleCategorySelects() {
    try {
        const res  = await fetch('/api/ledger/categories');
        const list = await res.json();
        const ids  = ['fr-category', 'ir-category', 'vr-category'];
        const emptyOpt = '<option value="0">-- 없음 --</option>';
        const opts = emptyOpt + list.map(c =>
            `<option value="${c.category_id}">${c.category_name}</option>`
        ).join('');
        ids.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerHTML = opts;
        });
    } catch (e) {
        console.error('[Ledger] 스케줄 카테고리 로드 실패', e);
    }
}

// DOMContentLoaded 이후 추가 초기화
document.addEventListener('DOMContentLoaded', () => {
    fillScheduleCategorySelects();
});

// ══════════════════════════════════════════════════════════
//  고정지출 규칙
// ══════════════════════════════════════════════════════════

/** 고정지출 규칙 목록 로드. */
async function loadFixedRules() {
    try {
        const res  = await fetch('/api/ledger/fixed-rules');
        const list = await res.json();
        renderFixedRules(list);
    } catch (e) {
        console.error('[Ledger] 고정지출 규칙 로드 실패', e);
    }
}

/** 고정지출 규칙 테이블 렌더링. */
function renderFixedRules(list) {
    const tbody = document.getElementById('fixed-rules-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">등록된 규칙이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => `
        <tr>
            <td>${r.name || '-'}</td>
            <td>${r.category_name || '-'}</td>
            <td style="text-align:right" class="amount-expense">-${fmt(r.amount)}</td>
            <td>${r.p_value}${pUnitLabel(r.p_unit)}마다</td>
            <td>${r.base_date || '-'}</td>
            <td style="text-align:center">
                <button class="exec-btn" onclick="openFixedExecModal(${r.rule_id},'${(r.name||'').replace(/'/g,'\\'')}',${r.amount})">▶ 실행</button>
                <button class="del-btn" onclick="deleteFixedRule(${r.rule_id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

/** 고정지출 규칙 등록 모달 열기. */
function openFixedRuleModal() {
    document.getElementById('fr-name').value    = '';
    document.getElementById('fr-amount').value  = '';
    document.getElementById('fr-base-date').valueAsDate = new Date();
    document.getElementById('fr-p-value').value = '1';
    openModal('fixed-rule-modal');
}

/** 고정지출 규칙 등록 제출. */
async function submitFixedRule() {
    const name     = document.getElementById('fr-name').value.trim();
    const amount   = parseInt(document.getElementById('fr-amount').value);
    const baseDate = document.getElementById('fr-base-date').value;
    const pValue   = parseInt(document.getElementById('fr-p-value').value);
    const pUnit    = document.getElementById('fr-p-unit').value;
    const catId    = parseInt(document.getElementById('fr-category').value) || 0;

    if (!name)           { alert('지출명을 입력하세요.'); return; }
    if (!amount || amount <= 0) { alert('금액을 올바르게 입력하세요.'); return; }
    if (!baseDate)       { alert('기준일을 선택하세요.'); return; }

    const res    = await fetch('/api/ledger/fixed-rules', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ name, category_id: catId, amount, base_date: baseDate, p_value: pValue, p_unit: pUnit })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('fixed-rule-modal');
        loadFixedRules();
    } else {
        alert('등록 실패: ' + result.message);
    }
}

/** 고정지출 실행 모달 열기. */
function openFixedExecModal(ruleId, ruleName, amount) {
    document.getElementById('fe-rule-id').value         = ruleId;
    document.getElementById('fe-rule-name').value       = ruleName;
    document.getElementById('fe-amount-display').value  = fmt(amount);
    document.getElementById('fe-date').valueAsDate      = new Date();
    document.getElementById('fe-asset').innerHTML       = buildAssetOptions();
    openModal('fixed-exec-modal');
}

/** 고정지출 실행 제출. */
async function submitFixedExec() {
    const ruleId  = document.getElementById('fe-rule-id').value;
    const assetId = document.getElementById('fe-asset').value;
    const txDate  = document.getElementById('fe-date').value;
    if (!txDate) { alert('거래일을 선택하세요.'); return; }

    const res    = await fetch(`/api/ledger/fixed-rules/${ruleId}/execute`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ asset_id: assetId, transaction_date: txDate })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('fixed-exec-modal');
        loadFixedReceipts();
    } else {
        alert('실행 실패: ' + result.message);
    }
}

/** 고정지출 규칙 삭제. */
async function deleteFixedRule(ruleId) {
    if (!confirm('이 규칙을 삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/ledger/fixed-rules/${ruleId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadFixedRules();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}

// ──────────────────────────────────────────────────────────
//  고정지출 영수증
// ──────────────────────────────────────────────────────────

/** 고정지출 영수증 목록 로드. */
async function loadFixedReceipts() {
    try {
        const res  = await fetch('/api/ledger/fixed-receipts');
        const list = await res.json();
        renderFixedReceipts(list);
    } catch (e) {
        console.error('[Ledger] 고정지출 영수증 로드 실패', e);
    }
}

/** 고정지출 영수증 테이블 렌더링. */
function renderFixedReceipts(list) {
    const tbody = document.getElementById('fixed-receipts-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">실행 이력이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => `
        <tr>
            <td>${r.transaction_date || '-'}</td>
            <td>${r.rule_name || '-'}</td>
            <td>${r.category_name || '-'}</td>
            <td>${r.asset_name || '<span style="color:#374151">연동 없음</span>'}</td>
            <td style="text-align:right" class="amount-expense">${fmt(r.amount)}</td>
            <td style="text-align:center">
                <button class="del-btn" onclick="deleteFixedReceipt(${r.ledger_id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

/** 고정지출 영수증 삭제 + 자산 잔액 복원. */
async function deleteFixedReceipt(ledgerId) {
    if (!confirm('이 내역을 삭제하면 연동 자산 잔액이 복원됩니다.\n삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/ledger/fixed-receipts/${ledgerId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadFixedReceipts();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}

// ══════════════════════════════════════════════════════════
//  정기수입 규칙
// ══════════════════════════════════════════════════════════

/** 정기수입 규칙 목록 로드. */
async function loadIncomeRules() {
    try {
        const res  = await fetch('/api/ledger/income-rules');
        const list = await res.json();
        renderIncomeRules(list);
    } catch (e) {
        console.error('[Ledger] 정기수입 규칙 로드 실패', e);
    }
}

/** 정기수입 규칙 테이블 렌더링. */
function renderIncomeRules(list) {
    const tbody = document.getElementById('income-rules-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">등록된 규칙이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => `
        <tr>
            <td>${r.name || '-'}</td>
            <td>${r.category_name || '-'}</td>
            <td style="text-align:right" class="amount-income">+${fmt(r.amount)}</td>
            <td>${r.p_value}${pUnitLabel(r.p_unit)}마다</td>
            <td>${r.base_date || '-'}</td>
            <td style="text-align:center">
                <button class="exec-btn income-exec-btn" onclick="openIncomeExecModal(${r.rule_id},'${(r.name||'').replace(/'/g,'\\'')}',${r.amount})">▶ 실행</button>
                <button class="del-btn" onclick="deleteIncomeRule(${r.rule_id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

/** 정기수입 규칙 등록 모달 열기. */
function openIncomeRuleModal() {
    document.getElementById('ir-name').value    = '';
    document.getElementById('ir-amount').value  = '';
    document.getElementById('ir-base-date').valueAsDate = new Date();
    document.getElementById('ir-p-value').value = '1';
    openModal('income-rule-modal');
}

/** 정기수입 규칙 등록 제출. */
async function submitIncomeRule() {
    const name     = document.getElementById('ir-name').value.trim();
    const amount   = parseInt(document.getElementById('ir-amount').value);
    const baseDate = document.getElementById('ir-base-date').value;
    const pValue   = parseInt(document.getElementById('ir-p-value').value);
    const pUnit    = document.getElementById('ir-p-unit').value;
    const catId    = parseInt(document.getElementById('ir-category').value) || 0;

    if (!name)           { alert('수입명을 입력하세요.'); return; }
    if (!amount || amount <= 0) { alert('금액을 올바르게 입력하세요.'); return; }
    if (!baseDate)       { alert('기준일을 선택하세요.'); return; }

    const res    = await fetch('/api/ledger/income-rules', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ name, category_id: catId, amount, base_date: baseDate, p_value: pValue, p_unit: pUnit })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('income-rule-modal');
        loadIncomeRules();
    } else {
        alert('등록 실패: ' + result.message);
    }
}

/** 정기수입 실행 모달 열기. */
function openIncomeExecModal(ruleId, ruleName, amount) {
    document.getElementById('ie-rule-id').value         = ruleId;
    document.getElementById('ie-rule-name').value       = ruleName;
    document.getElementById('ie-amount-display').value  = fmt(amount);
    document.getElementById('ie-date').valueAsDate      = new Date();
    document.getElementById('ie-asset').innerHTML       = buildAssetOptions();
    openModal('income-exec-modal');
}

/** 정기수입 실행 제출. */
async function submitIncomeExec() {
    const ruleId  = document.getElementById('ie-rule-id').value;
    const assetId = document.getElementById('ie-asset').value;
    const txDate  = document.getElementById('ie-date').value;
    if (!txDate) { alert('거래일을 선택하세요.'); return; }

    const res    = await fetch(`/api/ledger/income-rules/${ruleId}/execute`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ asset_id: assetId, transaction_date: txDate })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('income-exec-modal');
        loadIncomeReceipts();
    } else {
        alert('실행 실패: ' + result.message);
    }
}

/** 정기수입 규칙 삭제. */
async function deleteIncomeRule(ruleId) {
    if (!confirm('이 규칙을 삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/ledger/income-rules/${ruleId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadIncomeRules();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}

// ──────────────────────────────────────────────────────────
//  정기수입 영수증
// ──────────────────────────────────────────────────────────

/** 정기수입 영수증 목록 로드. */
async function loadIncomeReceipts() {
    try {
        const res  = await fetch('/api/ledger/income-receipts');
        const list = await res.json();
        renderIncomeReceipts(list);
    } catch (e) {
        console.error('[Ledger] 정기수입 영수증 로드 실패', e);
    }
}

/** 정기수입 영수증 테이블 렌더링. */
function renderIncomeReceipts(list) {
    const tbody = document.getElementById('income-receipts-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="6">실행 이력이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => `
        <tr>
            <td>${r.transaction_date || '-'}</td>
            <td>${r.rule_name || '-'}</td>
            <td>${r.category_name || '-'}</td>
            <td>${r.asset_name || '<span style="color:#374151">연동 없음</span>'}</td>
            <td style="text-align:right" class="amount-income">+${fmt(r.amount)}</td>
            <td style="text-align:center">
                <button class="del-btn" onclick="deleteIncomeReceipt(${r.ledger_id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

/** 정기수입 영수증 삭제 + 자산 잔액 복원. */
async function deleteIncomeReceipt(ledgerId) {
    if (!confirm('이 내역을 삭제하면 연동 자산 잔액이 복원됩니다.\n삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/ledger/income-receipts/${ledgerId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadIncomeReceipts();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}

// ══════════════════════════════════════════════════════════
//  변동지출 규칙
// ══════════════════════════════════════════════════════════

/** 변동지출 규칙 목록 로드. */
async function loadVariableRules() {
    try {
        const res  = await fetch('/api/ledger/variable-rules');
        const list = await res.json();
        renderVariableRules(list);
    } catch (e) {
        console.error('[Ledger] 변동지출 규칙 로드 실패', e);
    }
}

/** 변동지출 규칙 테이블 렌더링 (amount 없음). */
function renderVariableRules(list) {
    const tbody = document.getElementById('variable-rules-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="5">등록된 규칙이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => `
        <tr>
            <td>${r.name || '-'}</td>
            <td>${r.category_name || '-'}</td>
            <td>${r.p_value}${pUnitLabel(r.p_unit)}마다</td>
            <td>${r.base_date || '-'}</td>
            <td style="text-align:center">
                <button class="exec-btn" onclick="openVariableTriggerModal(${r.rule_id},'${(r.name||'').replace(/'/g,'\\'')}')">▶ 발생</button>
                <button class="del-btn" onclick="deleteVariableRule(${r.rule_id})">🗑</button>
            </td>
        </tr>
    `).join('');
}

/** 변동지출 규칙 등록 모달 열기. */
function openVariableRuleModal() {
    document.getElementById('vr-name').value    = '';
    document.getElementById('vr-base-date').valueAsDate = new Date();
    document.getElementById('vr-p-value').value = '1';
    openModal('variable-rule-modal');
}

/** 변동지출 규칙 등록 제출. */
async function submitVariableRule() {
    const name     = document.getElementById('vr-name').value.trim();
    const baseDate = document.getElementById('vr-base-date').value;
    const pValue   = parseInt(document.getElementById('vr-p-value').value);
    const pUnit    = document.getElementById('vr-p-unit').value;
    const catId    = parseInt(document.getElementById('vr-category').value) || 0;

    if (!name)     { alert('지출명을 입력하세요.'); return; }
    if (!baseDate) { alert('기준일을 선택하세요.'); return; }

    const res    = await fetch('/api/ledger/variable-rules', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ name, category_id: catId, base_date: baseDate, p_value: pValue, p_unit: pUnit })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('variable-rule-modal');
        loadVariableRules();
    } else {
        alert('등록 실패: ' + result.message);
    }
}

/** 변동지출 발생 모달 열기. */
function openVariableTriggerModal(ruleId, ruleName) {
    document.getElementById('vt-rule-id').value    = ruleId;
    document.getElementById('vt-rule-name').value  = ruleName;
    document.getElementById('vt-date').valueAsDate = new Date();
    document.getElementById('vt-asset').innerHTML  = buildAssetOptions();
    openModal('variable-trigger-modal');
}

/** 변동지출 발생 제출 (PENDING 영수증 생성). */
async function submitVariableTrigger() {
    const ruleId  = document.getElementById('vt-rule-id').value;
    const assetId = document.getElementById('vt-asset').value;
    const txDate  = document.getElementById('vt-date').value;
    if (!txDate) { alert('발생일을 선택하세요.'); return; }

    const res    = await fetch(`/api/ledger/variable-rules/${ruleId}/trigger`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ asset_id: assetId, transaction_date: txDate })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('variable-trigger-modal');
        loadVariableReceipts();
        alert(result.message);
    } else {
        alert('발생 실패: ' + result.message);
    }
}

/** 변동지출 규칙 삭제. */
async function deleteVariableRule(ruleId) {
    if (!confirm('이 규칙을 삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/ledger/variable-rules/${ruleId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadVariableRules();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}

// ──────────────────────────────────────────────────────────
//  변동지출 영수증
// ──────────────────────────────────────────────────────────

/** 변동지출 영수증 목록 로드. */
async function loadVariableReceipts() {
    try {
        const res  = await fetch('/api/ledger/variable-receipts');
        const list = await res.json();
        renderVariableReceipts(list);
    } catch (e) {
        console.error('[Ledger] 변동지출 영수증 로드 실패', e);
    }
}

/** 변동지출 영수증 테이블 렌더링 (status 컬럼 포함). */
function renderVariableReceipts(list) {
    const tbody = document.getElementById('variable-receipts-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="7">발생 이력이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(r => {
        const isPending   = r.status === 'PENDING';
        const statusBadge = isPending
            ? '<span class="badge badge-pending">미확정</span>'
            : '<span class="badge badge-confirmed">확정</span>';
        const amtText = isPending
            ? '<span style="color:#64748b">미확정</span>'
            : `<span class="amount-expense">${fmt(r.amount)}</span>`;
        const confirmBtn = isPending
            ? `<button class="confirm-btn" onclick="openConfirmModal(${r.ledger_id})">✓ 확정</button>`
            : '';
        return `
            <tr>
                <td>${r.transaction_date || '-'}</td>
                <td>${r.rule_name || '-'}</td>
                <td>${r.category_name || '-'}</td>
                <td>${r.asset_name || '<span style="color:#374151">연동 없음</span>'}</td>
                <td style="text-align:right">${amtText}</td>
                <td style="text-align:center">${statusBadge}</td>
                <td style="text-align:center">
                    ${confirmBtn}
                    <button class="del-btn" onclick="deleteVariableReceipt(${r.ledger_id})">🗑</button>
                </td>
            </tr>`;
    }).join('');
}

/** 변동지출 확정 모달 열기. */
function openConfirmModal(ledgerId) {
    document.getElementById('vc-ledger-id').value = ledgerId;
    document.getElementById('vc-amount').value    = '';
    openModal('variable-confirm-modal');
}

/** 변동지출 확정 제출 (PENDING → CONFIRMED + 자산 잔액 차감). */
async function submitConfirmVariable() {
    const ledgerId = document.getElementById('vc-ledger-id').value;
    const amount   = parseInt(document.getElementById('vc-amount').value);
    if (!amount || amount <= 0) { alert('실제 금액을 올바르게 입력하세요.'); return; }

    const res    = await fetch(`/api/ledger/variable-receipts/${ledgerId}/confirm`, {
        method:  'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ amount })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal('variable-confirm-modal');
        loadVariableReceipts();
    } else {
        alert('확정 실패: ' + result.message);
    }
}

/** 변동지출 영수증 삭제. */
async function deleteVariableReceipt(ledgerId) {
    if (!confirm('이 내역을 삭제하시겠습니까?\n(확정된 내역은 연동 자산 잔액이 복원됩니다.)')) return;
    const res    = await fetch(`/api/ledger/variable-receipts/${ledgerId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        loadVariableReceipts();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}
