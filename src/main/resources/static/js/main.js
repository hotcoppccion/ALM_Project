/* ================================================================
   ALM - main.js
   메인 대시보드: 총자산 표시 + 대표 계좌 Top 2 로딩
   ================================================================ */

window.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch('/api/auth/status');
        const data = await res.json();
        if (!data.hasPassword) { window.location.href = '/'; return; }
    } catch (e) { window.location.href = '/'; return; }

    fetchTotalAsset();
    fetchRepresentativeAccounts();

    // 시장 지수: 서버가 1초마다 KIS API를 호출해 캐싱하므로, 브라우저도 1초마다 폴링한다.
    // KIS 모의투자 REST API 제한이 TR_ID당 초당 1건이라 이게 최대 갱신 주기다.
    fetchMarketIndices();
    setInterval(fetchMarketIndices, 1000);
});

// 총자산 합계 표시
async function fetchTotalAsset() {
    try {
        const res  = await fetch('/api/asset/total');
        const data = await res.json();
        document.getElementById('total-amount').textContent =
            data.totalAmount.toLocaleString() + ' 원';
    } catch (err) {
        document.getElementById('total-amount').textContent = '조회 실패';
        console.error('총자산 조회 실패:', err);
    }
}

// ── 시장 지수 (코스피) ────────────────────────────────────────────
// 나스닥·원달러는 KIS 모의투자 환경이 해외 API를 지원하지 않아 제거했다.
async function fetchMarketIndices() {
    try {
        const res  = await fetch('/api/dashboard/market');
        const data = await res.json();

        setIndex('kospi', data.kospi, fmtIndex);

        const now = new Date();
        const hms = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`;
        document.getElementById('m-updated').textContent = `갱신 ${hms}`;
    } catch (e) {
        console.error('시장 지수 조회 실패', e);
    }
}

// 지수 값·등락률 DOM 업데이트 헬퍼
function setIndex(key, item, formatter) {
    if (!item) return;
    const valEl = document.getElementById(`${key}-val`);
    const chgEl = document.getElementById(`${key}-chg`);
    if (!valEl || !chgEl) return;

    if (item.error || item.value === '--') {
        valEl.textContent = '--';
        chgEl.textContent = '--%';
        chgEl.className   = 'idx-chg zero';
        return;
    }

    const val    = parseFloat(item.value)  || 0;
    const change = parseFloat(item.change) || 0;
    const sign   = change >= 0 ? '+' : '';

    valEl.textContent = formatter(val);
    chgEl.textContent = `${sign}${change.toFixed(2)}%`;
    chgEl.className   = `idx-chg ${change > 0 ? 'up' : change < 0 ? 'down' : 'zero'}`;
}

// 지수 포맷: 소수점 2자리, 천단위 콤마 (예: 2,755.93)
function fmtIndex(n) {
    return new Intl.NumberFormat('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n);
}



// 금융 계좌 중 잔액 상위 2개를 대표 계좌로 표시
async function fetchRepresentativeAccounts() {
    const tbody = document.getElementById('rep-asset-body');
    try {
        const res    = await fetch('/api/asset/list');
        const assets = await res.json();

        // ACC 타입만 필터 → 잔액 내림차순 정렬 → Top 2
        const accounts = assets
            .filter(a => a.type_code === 'ACC')
            .sort((a, b) => b.balance - a.balance)
            .slice(0, 2);

        if (accounts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#999;">등록된 계좌가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = accounts.map(a => {
            const interest = a.account_interest
                ? a.account_interest.toFixed(2) + '%'
                : '0.00%';
            return `<tr>
                <td>${a.bank_name  || '-'}</td>
                <td>${a.type_name  || '-'}</td>
                <td>${a.acc_number || '-'}</td>
                <td><strong>${a.balance.toLocaleString()} 원</strong></td>
                <td>${interest}</td>
            </tr>`;
        }).join('');

    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#e53935;">데이터 로딩 실패</td></tr>';
        console.error('대표 계좌 조회 실패:', err);
    }
}
