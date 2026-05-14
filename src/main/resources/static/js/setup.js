async function savePassword() {
    const passwordElement = document.getElementById('password');
    const passwordValue = passwordElement.value;

    if (!passwordValue) {
        alert("비밀번호를 입력해주세요.");
        return;
    }

    try {
        const response = await fetch('/api/setup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ "password": passwordValue })
        });

        if (response.ok) {
            alert("비밀번호가 저장되었습니다.");
            window.location.href = "/login.html"; // 가입 완료 후 로그인창으로 이동
        } else {
            alert("저장 실패. 서버 콘솔을 확인하세요.");
        }
    } catch (error) {
        console.error("Error:", error);
        alert("네트워크 오류 발생");
    }
}