/* =====================================================
 * modal.js — 커스텀 alert / confirm (통합)
 * ===================================================== */

const Modal = (function () {

    let $overlay, $box, $header, $body, $footer;

    // 모달 DOM을 처음 한 번만 생성
    function ensureDom() {
        if ($overlay) return;

        $overlay = $('<div class="cmodal-overlay"></div>');
        $box     = $('<div class="cmodal-box"></div>');
        $header  = $('<div class="cmodal-header"></div>');
        $body    = $('<div class="cmodal-body"></div>');
        $footer  = $('<div class="cmodal-footer"></div>');

        $box.append($header).append($body).append($footer);
        $overlay.append($box);
        $('body').append($overlay);
    }

    function open() {
        $overlay.addClass('active');
    }

    function close() {
        $overlay.removeClass('active');
    }

    // 제목 영역: title 있으면 표시, 없으면 숨김
    function setHeader(title) {
        if (title) {
            $header.text(title).show();
        } else {
            $header.empty().hide();
        }
    }

    // 본문 영역: html 우선, 없으면 message(텍스트)
    function setBody(options) {
        if (options.html) {
            $body.removeClass('cmodal-text').html(options.html);
        } else {
            $body.addClass('cmodal-text').text(options.message || '');
        }
    }

    // 커스텀 alert — 확인 버튼 하나
    function alert(messageOrOptions) {
        const options = normalize(messageOrOptions);
        ensureDom();

        return new Promise(function (resolve) {
            setHeader(options.title);
            setBody(options);
            $footer.empty();

            const $ok = $('<button class="cmodal-btn cmodal-confirm"></button>')
                .text(options.okText || 'OK')
                .on('click', function () {
                    close();
                    resolve();
                });

            $footer.append($ok);
            open();
            $ok.focus();
        });
    }

    // 커스텀 confirm — 확인/취소 → true/false 반환
    function confirm(messageOrOptions) {
        const options = normalize(messageOrOptions);
        ensureDom();

        return new Promise(function (resolve) {
            setHeader(options.title);
            setBody(options);
            $footer.empty();

            const $cancel = $('<button class="cmodal-btn cmodal-cancel"></button>')
                .text(options.cancelText || 'CANCEL')
                .on('click', function () {
                    close();
                    resolve(false);
                });

            const $ok = $('<button class="cmodal-btn cmodal-confirm"></button>')
                .text(options.okText || 'OK')
                .on('click', function () {
                    close();
                    resolve(true);
                });

            $footer.append($cancel).append($ok);
            open();
            $ok.focus();
        });
    }

    // 문자열로 부르면 {message: ...}로, 객체면 그대로
    function normalize(arg) {
        if (typeof arg === 'string') {
            return { message: arg };
        }
        return arg || {};
    }

    return { alert, confirm };
})();