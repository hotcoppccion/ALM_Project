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
    try {
        const res = await fetch('/api/auth/status');
        const data = await res.json();
        if (!data.hasPassword) { window.location.href = '/'; return; }
    } catch (e) { window.location.href = '/'; return; }

    // ① API 대기 없이 즉시 UI 렌더링
    //    (API가 느리거나 실패해도 달력/목록이 먼저 표시됨)
    renderCalendar();
    renderDetailList();
    document.getElementById('inp-date').valueAsDate = new Date();

    // ② 일반 내역 캐시 로드 후 달력·목록 재렌더링 (allEntries 반영)
    await loadAllEntries();
    renderCalendar();
    renderDetailList();

    // ③ 나머지 데이터 로드 (UI 렌더링과 독립적)
    loadSummaryFromAPI();
    loadCategories();
    await loadLiquidAssets();       // 자산 드롭다운 캐시 (일반 지출입 모달에서 사용)
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

/** 카테고리 드롭다운 초기 로드. 맨 아래에 "+ 새 카테고리" 옵션 포함. */
async function loadCategories() {
    try {
        const res  = await fetch('/api/ledger/categories');
        const list = await res.json();
        document.getElementById('sel-category').innerHTML =
            list.map(c => `<option value="${c.category_id}">${c.category_name}</option>`).join('') +
            `<option value="__add__" style="color:#60a5fa;">＋ 새 카테고리 추가...</option>`;
    } catch (e) {
        console.error('[Ledger] 카테고리 로드 실패', e);
    }
}

/** 카테고리 선택 변경 — "__add__" 선택 시 추가 로직으로 분기. */
function onCategoryChange(sel) {
    if (sel.value === '__add__') {
        sel.value = sel.options[0]?.value || '';  // 일단 첫 항목으로 되돌림
        addCategory();
    }
}

/** 새 카테고리 추가 후 드롭다운에 즉시 반영. */
async function addCategory() {
    const name = prompt('추가할 카테고리명을 입력하세요. (예: 여행, 의료비)');
    if (!name || !name.trim()) return;
    try {
        const res  = await fetch('/api/ledger/categories', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name.trim() })
        });
        const data = await res.json();
        if (!res.ok) { alert('추가 실패: ' + data.message); return; }
        const sel  = document.getElementById('sel-category');
        // "＋ 새 카테고리" 옵션 바로 앞에 새 항목 삽입 후 자동 선택
        const addOpt = sel.querySelector('option[value="__add__"]');
        const opt  = document.createElement('option');
        opt.value  = data.category_id;
        opt.textContent = data.category_name;
        sel.insertBefore(opt, addOpt);
        sel.value  = data.category_id;
    } catch (e) { alert('서버 통신 오류가 발생했습니다.'); }
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
    const defaultOpt = '<option value="">-- 자산 없음 --</option>';
    const assetOpts  = liquidAssets.map(a =>
        `<option value="${a.asset_id}">[${a.type_code}] ${a.display_name}</option>`
    ).join('');
    return defaultOpt + assetOpts;
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

