'use strict';

// ══════════════════════════════════════════════════════════
//  초기화
// ══════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch('/api/auth/status');
        const data = await res.json();
        if (!data.hasPassword) { window.location.href = '/'; return; }
    } catch (e) { window.location.href = '/'; return; }

    await loadGoals();
    await loadAssetsForSelect();
});

// ══════════════════════════════════════════════════════════
//  데이터 로드
// ══════════════════════════════════════════════════════════

/** 목표 목록 로드 및 렌더링 */
async function loadGoals() {
    try {
        const res  = await fetch('/api/goals');
        const list = await res.json();
        renderGoals(list);
        renderSummary(list);
    } catch (e) {
        console.error('[Goal] 목표 로드 실패', e);
        document.getElementById('goal-grid').innerHTML =
            '<div class="empty-msg">데이터를 불러올 수 없습니다.</div>';
    }
}

/** 자산 드롭다운 채우기 */
async function loadAssetsForSelect() {
    try {
        const res  = await fetch('/api/goals/assets');
        const list = await res.json();
        const sel  = document.getElementById('inp-asset');
        list.forEach(a => {
            const opt = document.createElement('option');
            opt.value       = a.asset_id;
            opt.textContent = `[${a.type_code}] ${a.display_name}`;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error('[Goal] 자산 목록 로드 실패', e);
    }
}

// ══════════════════════════════════════════════════════════
//  렌더링
// ══════════════════════════════════════════════════════════

const fmt = n => new Intl.NumberFormat('ko-KR').format(n) + '원';

/** 요약 바 렌더링 */
function renderSummary(list) {
    const done     = list.filter(g => g.achievement_rate >= 100).length;
    const inProg   = list.length - done;
    document.getElementById('sum-total').textContent    = list.length + '개';
    document.getElementById('sum-done').textContent     = done + '개';
    document.getElementById('sum-progress').textContent = inProg + '개';
}

/** 목표 카드 그리드 렌더링 */
function renderGoals(list) {
    const grid = document.getElementById('goal-grid');
    if (!list || list.length === 0) {
        grid.innerHTML = '<div class="empty-msg">등록된 목표가 없습니다.<br>＋ 목표 추가 버튼을 눌러 첫 목표를 만들어 보세요!</div>';
        return;
    }
    grid.innerHTML = list.map(g => buildCard(g)).join('');
}

/** 개별 카드 HTML 생성 */
function buildCard(g) {
    const isComplete = g.achievement_rate >= 100;
    const cardClass  = isComplete ? 'goal-card completed'
                     : (g.days_remaining !== null && g.days_remaining < 0 && g.end_date) ? 'goal-card overdue'
                     : 'goal-card';

    const pctLabel  = isComplete ? '🏆 달성!' : g.achievement_rate + '%';
    const fillClass = isComplete ? 'progress-bar-fill full' : 'progress-bar-fill';
    const pctClass  = isComplete ? 'progress-pct full' : 'progress-pct';

    const assetBadge = g.asset_name
        ? `<div class="asset-badge">📌 ${g.asset_name}</div>`
        : '<div class="asset-badge">📊 전체 자산 합계 기준</div>';

    const ddayHtml = buildDday(g);

    return `
    <div class="${cardClass}">
        <div class="card-top">
            <div class="goal-name">🎯 ${g.goal_name}</div>
            <button class="del-btn" onclick="deleteGoal(${g.goal_id})">🗑</button>
        </div>
        ${assetBadge}
        <div class="progress-wrap">
            <div class="progress-bar-bg">
                <div class="${fillClass}" style="width:${g.achievement_rate}%"></div>
            </div>
            <div class="${pctClass}">${pctLabel}</div>
        </div>
        <div class="amount-row">
            <span class="current-amount">${fmt(g.current_amount)}</span>
            <span class="target-amount">/ 목표 ${fmt(g.target_amount)}</span>
        </div>
        <div class="card-footer">
            ${ddayHtml}
            <span>부족: ${g.current_amount >= g.target_amount ? '0원' : fmt(g.target_amount - g.current_amount)}</span>
        </div>
    </div>`;
}

/** D-day 표시 문자열 생성 */
function buildDday(g) {
    if (!g.end_date) return '<span class="dday nodate">기한 없음</span>';
    const days = g.days_remaining;
    if (g.achievement_rate >= 100)
        return `<span class="dday done">🏆 목표 달성!</span>`;
    if (days < 0)
        return `<span class="dday overdue">⚠ D+${Math.abs(days)} 기한 초과</span>`;
    if (days === 0)
        return `<span class="dday soon">🔥 D-Day!</span>`;
    if (days <= 30)
        return `<span class="dday soon">⏰ D-${days} (${g.end_date})</span>`;
    return `<span class="dday">📅 D-${days} (${g.end_date})</span>`;
}

// ══════════════════════════════════════════════════════════
//  모달
// ══════════════════════════════════════════════════════════

function openAddModal() {
    document.getElementById('inp-name').value     = '';
    document.getElementById('inp-target').value   = '';
    document.getElementById('inp-end-date').value = '';
    document.getElementById('inp-asset').value    = '0';
    document.getElementById('add-modal').classList.add('active');
}

function closeModal() {
    document.getElementById('add-modal').classList.remove('active');
}

// 모달 배경 클릭 시 닫기
document.getElementById('add-modal').addEventListener('click', e => {
    if (e.target.id === 'add-modal') closeModal();
});

// ══════════════════════════════════════════════════════════
//  CRUD
// ══════════════════════════════════════════════════════════

/** 목표 등록 */
async function submitGoal() {
    const name    = document.getElementById('inp-name').value.trim();
    const target  = document.getElementById('inp-target').value;
    const endDate = document.getElementById('inp-end-date').value;
    const assetId = document.getElementById('inp-asset').value;

    if (!name)   { alert('목표명을 입력하세요.'); return; }
    if (!target || Number(target) <= 0) { alert('목표 금액을 입력하세요.'); return; }

    const res = await fetch('/api/goals', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({
            goal_name:     name,
            target_amount: Number(target),
            end_date:      endDate || '',
            asset_id:      Number(assetId)
        })
    });
    const result = await res.json();
    if (res.ok) {
        closeModal();
        await loadGoals();
    } else {
        alert('등록 실패: ' + result.message);
    }
}

/** 목표 삭제 */
async function deleteGoal(goalId) {
    if (!confirm('이 목표를 삭제하시겠습니까?')) return;
    const res    = await fetch(`/api/goals/${goalId}`, { method: 'DELETE' });
    const result = await res.json();
    if (res.ok) {
        await loadGoals();
    } else {
        alert('삭제 실패: ' + result.message);
    }
}
