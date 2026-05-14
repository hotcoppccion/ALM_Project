
function openModal(typeCode) {
    document.getElementById('asset-modal').style.display = 'block';
    document.getElementById('modal-type-code').value = typeCode;

    document.getElementById('modal-inputs').innerHTML = `
        <label>잔액/금액:</label>
        <input type="number" name="amount" required>
    `;
}


function closeModal() {
    document.getElementById('asset-modal').style.display = 'none';
}


async function submitAssetForm(event) {
    event.preventDefault(); // 새로고침 방지
    const typeCode = document.getElementById('modal-type-code').value;

    const res = await fetch(`/api/asset/${typeCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ /* 필요시 상세 데이터 추가 */ })
    });

    const data = await res.json();
    alert(data.message);
    closeModal();
}

// 4. 우클릭 '삭제' 버튼 클릭 시 (삭제 테스트)
async function deleteSelectedAsset(assetId) {
    if(!confirm("정말 삭제하시겠습니까?")) return;

    const res = await fetch(`/api/asset/${assetId}`, { method: 'DELETE' });
    const data = await res.json();
    alert(data.message);
}


async function fetchTotalAsset() {
    const res = await fetch('/api/asset/total');
    const data = await res.json();
    document.getElementById('total-asset-amount').textContent = data.totalAmount + " 원";
}