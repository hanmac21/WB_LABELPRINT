$(document).ready(function () {

    // 로컬 스토리지에 사용하는 변수 값
    const STORAGE_KEY = 'savedId';
    const COUNTRY_KEY = 'savedCountry';

    // 페이지 로드 시 저장된 ID, DB 불러오기
    loadSavedId();
    loadSavedCountry();

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const id = $('#id').val().trim();
        const password = $('#password').val().trim();
        const country = $('input[name="country"]:checked').val();
        const rememberId = $('#rememberId').is(':checked');

        if (rememberId) {
            localStorage.setItem(STORAGE_KEY, id);
        } else {
            localStorage.removeItem(STORAGE_KEY);
        }

        // 선택한 country 저장 (다음 로그인 때 자동 선택)
        localStorage.setItem(COUNTRY_KEY, country);

        $.ajax({
            url: '/login',
            method: 'POST',
            data: {
                username: id,
                password: password,
                country: country
            },
            success: function () {
                document.cookie = 'userId=' + encodeURIComponent(id) + '; path=/';      // 로그인 성공 시 쿠키 저장
                location.href = '/main';
            },
            error: function () {
                alert('ID or Password does not match');
                $('#id').focus();
            }
        });
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

    // 저장된 country 불러와서 라디오 자동 선택
    function loadSavedCountry() {
        const savedCountry = localStorage.getItem(COUNTRY_KEY);
        if (savedCountry) {
            const $radio = $('input[name="country"][value="' + savedCountry + '"]');
            if ($radio.length) {
                $radio.prop('checked', true);
            }
        }
    }
});