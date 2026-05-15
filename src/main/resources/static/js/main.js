/* ================================================================
   ALM - main.js
   메인 대시보드: 총자산 표시 + 대표 계좌 Top 2 로딩
   ================================================================ */

window.addEventListener('DOMContentLoaded', () => {
    fetchTotalAsset();
    fetchRepresentativeAccounts();
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
