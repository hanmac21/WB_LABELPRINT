$(document).ready(function () {

    const STORAGE_KEY = 'savedId';

    // 페이지 로드 시 저장된 ID 불러오기
    loadSavedId();

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const id = $('#id').val().trim();
        const password = $('#password').val().trim();
        const rememberId = $('#rememberId').is(':checked');

        if (id !== 'woobo' || password !== 'a1234') {
            alert('ID or Password does not match');
            $('#id').focus();
            return;
        }

        // ID 기억하기 처리
        if (rememberId) {
            localStorage.setItem(STORAGE_KEY, id);
        } else {
            localStorage.removeItem(STORAGE_KEY);
        }

        location.href = '/main';
    });

    // 저장된 ID 불러와서 자동 입력
    function loadSavedId() {
        const savedId = localStorage.getItem(STORAGE_KEY);
        if (savedId) {
            $('#id').val(savedId);
            $('#rememberId').prop('checked', true);
            $('#password').focus();   // 저장된 ID 있으면 비밀번호 입력칸으로 포커스
        } else {
            $('#id').focus();
        }
    }
});