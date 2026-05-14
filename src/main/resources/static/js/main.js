// 우클릭 이벤트 제어
const contextMenu = document.getElementById('context-menu');

window.addEventListener('contextmenu', (e) => {
    e.preventDefault(); // 기본 우클릭 메뉴 차단

    // 마우스 위치에 메뉴 배치
    contextMenu.style.top = `${e.pageY}px`;
    contextMenu.style.left = `${e.pageX}px`;
    contextMenu.style.display = 'block';
});

// 클릭 시 메뉴 닫기
window.addEventListener('click', () => {
    contextMenu.style.display = 'none';
});

// 메뉴 항목 클릭 처리
function handleAction(type) {
    if (type === 'insert') alert('새로운 데이터를 삽입합니다.');
    else if (type === 'update') alert('기존 데이터를 수정합니다.');
    else if (type === 'delete') alert('데이터를 삭제합니다.');
}