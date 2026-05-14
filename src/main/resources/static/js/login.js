async function login() {
    const passwordValue = document.getElementById('loginPassword').value;

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ "password": passwordValue })
        });

        const result = await response.json();

        if (response.ok && result.success) {
            alert("로그인 성공!");
            window.location.href = "/main.html"; // 메인 페이지로 이동
        } else {
            alert("비밀번호가 일치하지 않습니다.");
        }
    } catch (error) {
        console.error("Error:", error);
    }
}