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

    loadPortfolio();
    loadLogs();

    // 모달 배경 클릭 시 닫기
    document.getElementById('buy-modal').addEventListener('click', e => {
        if (e.target.id === 'buy-modal') closeBuyModal();
    });
    document.getElementById('sell-modal').addEventListener('click', e => {
        if (e.target.id === 'sell-modal') closeSellModal();
    });


});

// ══════════════════════════════════════════════════════════
//  포맷 유틸
// ══════════════════════════════════════════════════════════
const fmt    = n => new Intl.NumberFormat('ko-KR').format(n) + '원';
const fmtNum = n => new Intl.NumberFormat('ko-KR').format(n);

function signClass(n)  { return n > 0 ? 'up' : n < 0 ? 'down' : 'zero'; }
function signStr(n)    { return n > 0 ? '+' : ''; }

// ══════════════════════════════════════════════════════════
//  데이터 로드
// ══════════════════════════════════════════════════════════

async function loadPortfolio() {
    try {
        const res  = await fetch('/api/invest/portfolio');
        const list = await res.json();
        renderPortfolio(list);
        renderSummary(list);
        fillSellSelect(list);
    } catch (e) {
        console.error('[Invest] 포트폴리오 로드 실패', e);
        document.getElementById('portfolio-body').innerHTML =
            '<tr><td colspan="10" class="empty-cell">데이터를 불러올 수 없습니다.</td></tr>';
    }
}

async function loadLogs() {
    try {
        const res  = await fetch('/api/invest/logs');
        const list = await res.json();
        renderLogs(list);
    } catch (e) {
        console.error('[Invest] 이력 로드 실패', e);
    }
}

// ══════════════════════════════════════════════════════════
//  렌더링
// ══════════════════════════════════════════════════════════

function renderSummary(list) {
    const valid = list.filter(p => !p.api_error);
    const book    = valid.reduce((s, p) => s + p.book_value,    0);
    const current = valid.reduce((s, p) => s + p.current_value, 0);
    const profit  = current - book;
    const rate    = book > 0 ? (profit / book * 100).toFixed(2) : '0.00';

    document.getElementById('sum-count').textContent   = list.length + '종목';
    document.getElementById('sum-book').textContent    = fmt(book);
    document.getElementById('sum-current').textContent = fmt(current);

    const profitEl = document.getElementById('sum-profit');
    profitEl.textContent = signStr(profit) + fmt(profit);
    profitEl.className   = 'sum-val ' + (profit > 0 ? 'profit' : profit < 0 ? 'loss' : '');

    const rateEl = document.getElementById('sum-rate');
    rateEl.textContent = signStr(profit) + rate + '%';
    rateEl.className   = 'sum-val ' + signClass(profit);
}

function renderPortfolio(list) {
    const tbody = document.getElementById('portfolio-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" class="empty-cell">보유 종목이 없습니다.<br>매수 버튼으로 첫 종목을 추가해 보세요!</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(p => {
        if (p.api_error) {
            return `<tr>
                <td class="stock-name">${p.ticker_name || p.ticker_code}</td>
                <td class="stock-code">${p.ticker_code}</td>
                <td>${fmtNum(p.quantity)}</td>
                <td>${fmt(p.purchase_price)}</td>
                <td colspan="5" class="api-err">⚠ 시세 조회 실패</td>
            </tr>`;
        }
        const profitCls = signClass(p.profit_loss);
        const changeCls = parseFloat(p.price_change) >= 0 ? 'up' : 'down';
        return `<tr>
            <td class="stock-name">${p.ticker_name}</td>
            <td class="stock-code">${p.ticker_code}</td>
            <td>${fmtNum(p.quantity)}</td>
            <td>${fmt(p.purchase_price)}</td>
            <td class="${changeCls}">${fmt(p.current_price)}</td>
            <td>${fmt(p.current_value)}</td>
            <td class="${profitCls}">${signStr(p.profit_loss)}${fmt(p.profit_loss)}</td>
            <td class="${profitCls}">${signStr(p.profit_rate)}${p.profit_rate.toFixed(2)}%</td>
            <td class="${changeCls}">${signStr(parseFloat(p.price_change))}${p.price_change}%</td>
        </tr>`;
    }).join('');
}

function renderLogs(list) {
    const tbody = document.getElementById('log-body');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty-cell">매매 이력이 없습니다.</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(l => {
        const isBuy    = l.transaction_type === 'BUY';
        const typeCls  = isBuy ? 'type-buy' : 'type-sell';
        const typeStr  = isBuy ? '매수' : '매도';
        const total    = l.quantity * l.price;
        const profitHtml = isBuy ? '-'
            : `<span class="${signClass(l.realized_profit)}">${signStr(l.realized_profit)}${fmt(l.realized_profit)}</span>`;
        return `<tr>
            <td>${l.trade_date}</td>
            <td>${l.ticker_name} <span style="color:#4a5568;font-size:11px">${l.ticker_code}</span></td>
            <td><span class="${typeCls}">${typeStr}</span></td>
            <td>${fmtNum(l.quantity)}</td>
            <td>${fmt(l.price)}</td>
            <td>${fmt(total)}</td>
            <td>${profitHtml}</td>
            <td style="color:#718096;font-size:12px">${l.reason_basis || '-'}</td>
            <td><button class="del-btn" onclick="deleteLog(${l.invest_log_id})">🗑</button></td>
        </tr>`;
    }).join('');
}

function fillSellSelect(list) {
    const sel = document.getElementById('sell-stock');
    sel.innerHTML = '<option value="">-- 보유 종목 선택 --</option>';
    list.forEach(p => {
        const opt = document.createElement('option');
        opt.value       = p.ticker_code;
        opt.textContent = `[${p.ticker_code}] ${p.ticker_name} (${fmtNum(p.quantity)}주)`;
        sel.appendChild(opt);
    });
}

// ══════════════════════════════════════════════════════════
//  종목 코드 입력 시 KIS API로 종목명 자동완성
// ══════════════════════════════════════════════════════════
let lookupTimer = null;

function lookupCode(prefix) {
    clearTimeout(lookupTimer);
    const code = document.getElementById(`${prefix}-code`).value.trim().toUpperCase();
    document.getElementById(`${prefix}-code`).value = code;

    const isCode6    = /^\d{6}$/.test(code);
    const isOverseas = /^[A-Za-z]{1,10}$/.test(code);
    if (!isCode6 && !isOverseas) return;

    lookupTimer = setTimeout(async () => {
        try {
            const res  = await fetch(`/api/invest/stocks/lookup?code=${encodeURIComponent(code)}`);
            const data = await res.json();
            if (res.ok && data.ticker_name) {
                const nameEl = document.getElementById(`${prefix}-name`);
                if (!nameEl.value.trim()) nameEl.value = data.ticker_name;
            }
        } catch (e) { /* 조회 실패 시 무시 — 수동 입력 가능 */ }
    }, 500);
}

// ══════════════════════════════════════════════════════════
//  모달
// ══════════════════════════════════════════════════════════

async function openBuyModal() {
    // 증권 계좌 목록 로드 (매번 최신화)
    try {
        const res  = await fetch('/api/invest/brokerages');
        const list = await res.json();
        const sel  = document.getElementById('buy-asset-id');
        sel.innerHTML = '<option value="">-- 계좌 선택 --</option>';
        list.forEach(b => {
            const opt = document.createElement('option');
            opt.value       = b.asset_id;
            opt.textContent = `${b.bank_name} (${b.acc_number})`;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error('계좌 목록 로드 실패', e);
    }

    document.getElementById('buy-code').value   = '';
    document.getElementById('buy-name').value   = '';
    document.getElementById('buy-qty').value    = '';
    document.getElementById('buy-price').value  = '';
    document.getElementById('buy-reason').value = '';
    document.getElementById('buy-date').value   = '';
    document.getElementById('buy-modal').classList.add('active');
}
function closeBuyModal()  { document.getElementById('buy-modal').classList.remove('active'); }

async function openSellModal() {
    // 보유 종목 최신화
    const res  = await fetch('/api/invest/portfolio');
    const list = await res.json();
    fillSellSelect(list);
    document.getElementById('sell-qty').value    = '';
    document.getElementById('sell-price').value  = '';
    document.getElementById('sell-reason').value = '';
    document.getElementById('sell-date').value   = '';
    document.getElementById('sell-modal').classList.add('active');
}
function closeSellModal() { document.getElementById('sell-modal').classList.remove('active'); }

// ══════════════════════════════════════════════════════════
//  CRUD
// ══════════════════════════════════════════════════════════

async function submitBuy() {
    const assetId = document.getElementById('buy-asset-id').value;
    const code    = document.getElementById('buy-code').value.trim();
    const name    = document.getElementById('buy-name').value.trim();
    const qty     = document.getElementById('buy-qty').value;
    const price   = document.getElementById('buy-price').value;
    const date    = document.getElementById('buy-date').value;
    const reason  = document.getElementById('buy-reason').value.trim();

    if (!assetId) { alert('증권 계좌를 선택하세요.'); return; }
    if (!code)    { alert('종목을 검색하여 선택하세요.'); return; }
    if (!qty   || Number(qty)   <= 0) { alert('수량을 입력하세요.'); return; }
    if (!price || Number(price) <= 0) { alert('매수가를 입력하세요.'); return; }
    if (!date) { alert('거래일을 입력하세요.'); return; }

    const res = await fetch('/api/invest/buy', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            asset_id:     Number(assetId),
            ticker_code:  code,
            ticker_name:  name,
            quantity:     Number(qty),
            price:        Number(price),
            trade_date:   date,
            reason:       reason
        })
    });
    const result = await res.json();
    if (res.ok) {
        closeBuyModal();
        loadPortfolio();
        loadLogs();
    } else {
        alert('매수 실패: ' + result.message);
    }
}

async function submitSell() {
    const code   = document.getElementById('sell-stock').value;
    const qty    = document.getElementById('sell-qty').value;
    const price  = document.getElementById('sell-price').value;
    const date   = document.getElementById('sell-date').value;
    const reason = document.getElementById('sell-reason').value.trim();

    if (!code)  { alert('종목을 선택하세요.'); return; }
    if (!qty || Number(qty) <= 0)   { alert('수량을 입력하세요.'); return; }
    if (!price || Number(price) <= 0) { alert('매도가를 입력하세요.'); return; }
    if (!date) { alert('거래일을 입력하세요.'); return; }

    const res = await fetch('/api/invest/sell', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            ticker_code:  code,
            quantity:     Number(qty),
            price:        Number(price),
            trade_date:   date,
            reason:       reason
        })
    });
    const result = await res.json();
    if (res.ok) {
        closeSellModal();
        loadPortfolio();
        loadLogs();
    } else {
        alert('매도 실패: ' + result.message);
    }
}

async function deleteLog(logId) {
    if (!confirm('이 이력을 삭제하시겠습니까?')) return;
    const res = await fetch(`/api/invest/logs/${logId}`, { method: 'DELETE' });
    if (res.ok) loadLogs();
    else alert('삭제 실패');
}
