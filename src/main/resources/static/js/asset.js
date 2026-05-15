/* ================================================================
   ALM - Asset.js
   자산 관리 페이지 전체 로직
   - 4개 타입(ACC / REA / PHY / CSH) 등록·수정·삭제
   - 은행·계좌종류 드롭다운을 서버 DB에서 동적 로딩
   ================================================================ */

let selectedAssetId   = null;   // 우클릭으로 선택된 자산 ID
let selectedTypeCode  = null;   // 선택된 자산의 타입 코드
let isUpdateMode      = false;  // true: 수정 모드 / false: 등록 모드

// ─────────────────────────────────────────────────────────────────
// 초기 로드
// ─────────────────────────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', () => {
    fetchTotalAsset();
    fetchAllAssets();
});

// ─────────────────────────────────────────────────────────────────
// 총자산 조회
// ─────────────────────────────────────────────────────────────────
async function fetchTotalAsset() {
    try {
        const res  = await fetch('/api/asset/total');
        const data = await res.json();
        document.getElementById('total-asset-amount').textContent =
            data.totalAmount.toLocaleString() + ' 원';
    } catch (err) {
        console.error('총자산 조회 실패:', err);
    }
}

// ─────────────────────────────────────────────────────────────────
// 전체 자산 목록 조회 → 4개 테이블에 분배 렌더링
// ─────────────────────────────────────────────────────────────────
async function fetchAllAssets() {
    try {
        const res    = await fetch('/api/asset/list');
        const assets = await res.json();

        // 테이블 초기화
        ['account-table', 'realestate-table', 'physical-table', 'cash-table']
            .forEach(id => document.querySelector(`#${id} tbody`).innerHTML = '');

        assets.forEach(asset => {
            const tr = document.createElement('tr');
            tr.dataset.id       = asset.asset_id;
            tr.dataset.typeCode = asset.type_code;

            if (asset.type_code === 'ACC') {
                const interest = asset.account_interest
                    ? asset.account_interest.toFixed(2) + '%'
                    : '0.00%';
                tr.innerHTML = `
                    <td>${asset.bank_name  || '-'}</td>
                    <td>${asset.type_name  || '-'}</td>
                    <td>${asset.acc_number || '-'}</td>
                    <td>${asset.balance.toLocaleString()} 원</td>
                    <td>${interest}</td>`;
                document.querySelector('#account-table tbody').appendChild(tr);

            } else if (asset.type_code === 'REA') {
                tr.innerHTML = `
                    <td>${asset.contract_type || '-'}</td>
                    <td>${asset.address       || '-'}</td>
                    <td>${asset.price.toLocaleString()} 원</td>`;
                document.querySelector('#realestate-table tbody').appendChild(tr);

            } else if (asset.type_code === 'PHY') {
                tr.innerHTML = `
                    <td>${asset.item_name || '-'}</td>
                    <td>${asset.purchase_price.toLocaleString()} 원</td>
                    <td>${asset.current_value.toLocaleString()} 원</td>`;
                document.querySelector('#physical-table tbody').appendChild(tr);

            } else if (asset.type_code === 'CSH') {
                tr.innerHTML = `
                    <td>${asset.name || '-'}</td>
                    <td>${asset.balance.toLocaleString()} 원</td>`;
                document.querySelector('#cash-table tbody').appendChild(tr);
            }
        });
    } catch (err) {
        console.error('자산 목록 조회 에러:', err);
    }
}

// ─────────────────────────────────────────────────────────────────
// 모달 열기 (등록)
// ─────────────────────────────────────────────────────────────────
function handleInsert(typeCode) {
    isUpdateMode = false;
    openModal(typeCode, null);
}

// ─────────────────────────────────────────────────────────────────
// 모달 열기 (수정) - 서버에서 기존 데이터 조회 후 pre-fill
// ─────────────────────────────────────────────────────────────────
async function handleUpdate(assetId, typeCode) {
    isUpdateMode = true;
    try {
        const res = await fetch(`/api/asset/${assetId}?typeCode=${typeCode}`);
        if (!res.ok) { alert('자산 정보를 불러오지 못했습니다.'); return; }
        const data = await res.json();
        openModal(typeCode, data);
    } catch (err) {
        alert('서버 통신 오류가 발생했습니다.');
    }
}

// ─────────────────────────────────────────────────────────────────
// 공통 모달 렌더링 함수
// existingData 가 null 이면 빈 폼(등록), 값이 있으면 pre-fill(수정)
// ─────────────────────────────────────────────────────────────────
async function openModal(typeCode, existingData) {
    document.getElementById('asset-modal').style.display  = 'block';
    document.getElementById('modal-overlay').style.display = 'block';
    document.getElementById('modal-type-code').value = typeCode;

    const title  = document.getElementById('modal-title');
    const inputs = document.getElementById('modal-inputs');

    const label = { ACC: '금융 계좌', REA: '부동산', PHY: '실물 자산', CSH: '현금' };
    title.textContent = (isUpdateMode ? '' : '') + (label[typeCode] || '자산') +
                        (isUpdateMode ? ' 수정' : ' 추가');

    if (typeCode === 'ACC') {
        // 드롭다운 데이터를 DB에서 동적 로딩
        let banks = [], types = [];
        try {
            const [br, tr] = await Promise.all([
                fetch('/api/asset/banks'),
                fetch('/api/asset/account-types')
            ]);
            banks = await br.json();
            types = await tr.json();
        } catch (e) { console.error('마스터 데이터 로딩 실패:', e); }

        const bankOptions = banks.map(b =>
            `<option value="${b.bank_id}" ${existingData && existingData.bank_id == b.bank_id ? 'selected' : ''}>${b.bank_name}</option>`
        ).join('');
        const typeOptions = types.map(t =>
            `<option value="${t.type_id}" ${existingData && existingData.type_id == t.type_id ? 'selected' : ''}>${t.type_name}</option>`
        ).join('');

        inputs.innerHTML = `
            <label>은행 선택:</label><br>
            <select id="bank_id" style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px;">
                ${bankOptions}
            </select>

            <label>계좌 종류:</label><br>
            <select id="type_id" style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px;">
                ${typeOptions}
            </select>

            <label>계좌번호:</label><br>
            <input type="text" id="acc_number" placeholder="예: 123-456-789012"
                   value="${existingData?.acc_number || ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>잔액 (원):</label><br>
            <input type="number" id="balance" placeholder="0" required
                   value="${existingData?.balance ?? ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>이율 (%):</label><br>
            <input type="number" id="account_interest" step="0.01" placeholder="0.00"
                   value="${existingData?.account_interest ?? '0'}"
                   style="width:100%; padding:6px; margin-bottom:0; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;">
        `;

    } else if (typeCode === 'REA') {
        inputs.innerHTML = `
            <label>계약 형태:</label><br>
            <select id="contract_type" style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px;">
                <option value="자가" ${existingData?.contract_type === '자가' ? 'selected' : ''}>자가</option>
                <option value="전세" ${existingData?.contract_type === '전세' ? 'selected' : ''}>전세</option>
                <option value="월세" ${existingData?.contract_type === '월세' ? 'selected' : ''}>월세</option>
                <option value="분양권" ${existingData?.contract_type === '분양권' ? 'selected' : ''}>분양권</option>
            </select>

            <label>주소:</label><br>
            <input type="text" id="address" placeholder="예: 서울시 강남구 ..."
                   value="${existingData?.address || ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>자산 금액 (원):</label><br>
            <input type="number" id="price" placeholder="0" required
                   value="${existingData?.price ?? ''}"
                   style="width:100%; padding:6px; margin-bottom:0; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;">
        `;

    } else if (typeCode === 'PHY') {
        inputs.innerHTML = `
            <label>품목명:</label><br>
            <input type="text" id="item_name" placeholder="예: 차량, 귀금속 ..."
                   value="${existingData?.item_name || ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>구입가 (원):</label><br>
            <input type="number" id="purchase_price" placeholder="0"
                   value="${existingData?.purchase_price ?? ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>현재 평가액 (원):</label><br>
            <input type="number" id="current_value" placeholder="0" required
                   value="${existingData?.current_value ?? ''}"
                   style="width:100%; padding:6px; margin-bottom:0; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;">
        `;

    } else if (typeCode === 'CSH') {
        inputs.innerHTML = `
            <label>보관처 (이름):</label><br>
            <input type="text" id="name" placeholder="예: 지갑, 서랍 ..."
                   value="${existingData?.name || ''}"
                   style="width:100%; padding:6px; margin-bottom:10px; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;"><br>

            <label>보유 금액 (원):</label><br>
            <input type="number" id="balance" placeholder="0" required
                   value="${existingData?.balance ?? ''}"
                   style="width:100%; padding:6px; margin-bottom:0; border:1px solid #ddd; border-radius:5px; box-sizing:border-box;">
        `;
    }
}

function closeModal() {
    document.getElementById('asset-modal').style.display  = 'none';
    document.getElementById('modal-overlay').style.display = 'none';
    isUpdateMode     = false;
    selectedAssetId  = null;
    selectedTypeCode = null;
}

// ─────────────────────────────────────────────────────────────────
// 폼 제출 (등록 / 수정 공용)
// ─────────────────────────────────────────────────────────────────
async function submitAssetForm(event) {
    event.preventDefault();
    const typeCode = document.getElementById('modal-type-code').value;

    const formData = {
        type_code:        typeCode,
        bank_id:          parseInt(document.getElementById('bank_id')?.value        || '1'),
        type_id:          parseInt(document.getElementById('type_id')?.value        || '1'),
        acc_number:       document.getElementById('acc_number')?.value              || '',
        account_interest: document.getElementById('account_interest')?.value        || '0',
        contract_type:    document.getElementById('contract_type')?.value           || '',
        address:          document.getElementById('address')?.value                 || '',
        item_name:        document.getElementById('item_name')?.value               || '',
        name:             document.getElementById('name')?.value                    || '',
        balance:          document.getElementById('balance')?.value                 || '0',
        price:            document.getElementById('price')?.value                   || '0',
        purchase_price:   document.getElementById('purchase_price')?.value          || '0',
        current_value:    document.getElementById('current_value')?.value           || '0',
    };

    try {
        let res;
        if (isUpdateMode) {
            // 수정: PUT /api/asset/{assetId}
            res = await fetch(`/api/asset/${selectedAssetId}`, {
                method:  'PUT',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(formData)
            });
        } else {
            // 등록: POST /api/asset/{typeCode}
            res = await fetch(`/api/asset/${typeCode}`, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(formData)
            });
        }

        const data = await res.json();
        alert(data.message);
        if (res.ok) { closeModal(); location.reload(); }
    } catch (err) {
        alert('서버 통신 오류가 발생했습니다.');
    }
}

// ─────────────────────────────────────────────────────────────────
// 우클릭 컨텍스트 메뉴
// ─────────────────────────────────────────────────────────────────
document.querySelector('.tables-grid').addEventListener('contextmenu', (e) => {
    const tr = e.target.closest('tr');
    if (tr && tr.dataset.id) {
        e.preventDefault();
        selectedAssetId  = tr.dataset.id;
        selectedTypeCode = tr.dataset.typeCode;
        const menu = document.getElementById('asset-context-menu');
        menu.style.display = 'block';
        menu.style.left    = e.pageX + 'px';
        menu.style.top     = e.pageY + 'px';
    }
});

window.addEventListener('click', () => {
    document.getElementById('asset-context-menu').style.display = 'none';
});

function handleAction(action) {
    if (action === 'update') {
        if (selectedAssetId && selectedTypeCode) {
            handleUpdate(selectedAssetId, selectedTypeCode);
        }
    } else if (action === 'delete') {
        deleteSelectedAsset(selectedAssetId);
    }
}

// ─────────────────────────────────────────────────────────────────
// 삭제
// ─────────────────────────────────────────────────────────────────
async function deleteSelectedAsset(assetId) {
    if (!assetId) return;
    const userInput = prompt("삭제하려면 '삭제'를 입력하세요.");
    if (userInput === null) return;

    try {
        const res  = await fetch(
            `/api/asset/${assetId}?confirmString=${encodeURIComponent(userInput)}`,
            { method: 'DELETE' }
        );
        const data = await res.json();
        alert(data.message);
        if (res.ok) location.reload();
    } catch (err) {
        alert('서버 통신 오류가 발생했습니다.');
    }
}
