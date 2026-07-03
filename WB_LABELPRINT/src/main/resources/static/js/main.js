/* =====================================================
 * ENTRY — 진입점
 * ===================================================== */

$(document).ready(function () {
    init();
    bindEvents();
});


/* =====================================================
 * INIT — 페이지 초기화
 * ===================================================== */

function init() {
    // 상단바 - 현재 연결된 DB 표시
    loadCurrentCountry();

    // 고객사 선택
    loadSuppliers();

    // 오늘 날짜 선택
    $('#lotDate').val(getTodayString());
    //$('#lotDate').attr('min', getTodayString());   // 오늘 이전 날짜 선택 불가

    // 페이지 진입 시 자동 검색
    search();

    // 초기 TOTAL QTY 계산
    updateTotalQty();

    // 쿠키에 저장된 가이드 옵션 복원 후 강조 반영
    const savedGuide = getCookie('guideOption');
    if (savedGuide !== null) {
        $('#guideOption').val(savedGuide);
    }
    updateGuideHighlight();
}


/* =====================================================
 * EVENT BINDINGS — 이벤트 바인딩
 * ===================================================== */

function bindEvents() {
    // 로그아웃
    $('#btnLogout').on('click', logout);

    // 검색
    $('#btnSearch').on('click', search);

    $('#itemcode, #spec, #itemname').on('keydown', function (e) {
        if (e.key === 'Enter') {
            search();
        }
    });

    // 발행
    $('#btnPrint').on('click', print);

    // LOT DATE 변경 시 선택된 행 기준으로 LOTNO 재조회
    $('#lotDate').on('change', function () {
        const row = $('.data-table tr.row-selected');
        if (row.length === 0) {
            return;  // 선택된 행 없으면 아무것도 안 함
        }
        fetchLotno(row.find('.col-itemcode').text());
    });

    // LOT QTY / PRINT QTY — 입력 중 실시간 계산
    $('#lotQty, #printQty').on('input', updateTotalQty);

    // LOT QTY / PRINT QTY — 포커스 떠날 때 값 검증 및 보정
    $('#lotQty').on('blur', function () {
        validateQty($(this), 1, null);   // 최소 1, 최대 제한 없음
        updateTotalQty();
    });

    $('#printQty').on('blur', function () {
        validateQty($(this), 1, 100);    // 최소 1, 최대 100
        updateTotalQty();
    });

    // LOT QTY / PRINT QTY — 음수 부호, 'e' 등 숫자 외 키 차단
    $('#lotQty, #printQty').on('keydown', function (e) {
        if (['e', 'E', '+', '-', '.'].includes(e.key)) {
            e.preventDefault();
        }
    });

    // 가이드 옵션 — 선택 시 쿠키 저장 + 강조 갱신
    $('#guideOption').on('change', function () {
        setCookie('guideOption', $(this).val());
        updateGuideHighlight();
    });

    // LABEL TYPE(PT) 선택 시 양식 안내 갱신
    $('#ptExtra').on('change', updateLabelTypeExample);

    // 출력 창 닫기 모달
    $('#printModalClose').on('click', closePrintModal);

    // 폐기 토글 버튼
    $('#scrapToggle').on('click', function () {
        const on = $(this).attr('aria-pressed') === 'true';
        $(this).attr('aria-pressed', String(!on));
        $(this).find('.toggle-text').text(on ? 'EXCLUDE' : 'INCLUDE');
    });

    // 엔터키 검색
    $('input[type="text"]').off('keypress').on('keypress', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            search();
        }
    });
}

/* =====================================================
 * TOPBAR — 상단바 (DB 표시 / 로그아웃)
 * ===================================================== */

function loadCurrentCountry() {
    $.ajax({
        url: '/session/country',
        type: 'GET',
        dataType: 'text',
        success: function (country) {
            $('#dbName').text(country === "PT" ? "KOR" : country);
            guideFormats = buildGuideFormats(country);
            updateGuideHighlight();                       // 포맷 반영

            setupCountryExtras(country);   // ← PT 전용 영역 처리
        },
        error: function () {
            $('#dbName').text('-');
            guideFormats = buildGuideFormats('USA');
        }
    });
}

function logout() {
    Modal.confirm({
        title: 'LOGOUT',
        message: 'Are you sure you want to log out?',
        okText: 'LOGOUT',
        cancelText: 'CANCEL'
    }).then(function (ok) {
        if (!ok) return;

        $.ajax({
            url: '/logout',
            type: 'POST',
            complete: function () {
                location.href = '/login';
            }
        });
    });
}


/* =====================================================
 * SEARCH & TABLE — 검색 / 테이블 렌더링
 * ===================================================== */

function search() {
    const requestBody = {
        itemtype: $('#itemtype').val().trim(),
        car: $('#car').val().trim(),
        itemcode: $('#itemcode').val().trim(),
        spec: $('#spec').val().trim(),
        itemname: $('#itemname').val().trim(),
        scrap: $('#scrapToggle').attr('aria-pressed') === 'true'
    };

    showLoading();

    $.ajax({
        url: '/items/search',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestBody),
        dataType: 'json',
        success: function (items) {
            console.log(items);
            renderTable(items);
        },
        error: function (xhr, status, error) {
            console.error('Search failed:', status, error, xhr.responseText);
            Modal.alert('Search failed. Please check the console.');
        },
        complete: hideLoading
    });
}

function renderTable(items) {
    const $tbody = $('#itemTableBody');
    $tbody.empty();

    if (!items || items.length === 0) {
        $tbody.append(`<tr><td colspan="6" class="empty-row">No data found</td></tr>`);
        return;
    }

    items.forEach(function (item, index) {
        // 폐기 여부
        const isDeleted = item.deldate != null && String(item.deldate).trim() !== '';

        const rowHtml = `
        <tr class="${isDeleted ? 'row-deleted' : ''}">
            <td class="col-no">${(index + 1).toLocaleString()}</td>
            <td class="col-itemtype">${escapeHtml(item.itemtype)}</td>
            <td class="col-car">${escapeHtml(item.car)}</td>
            <td class="col-itemcode">${escapeHtml(item.itemcode)}</td>
            <td class="col-spec">${escapeHtml(item.spec)}</td>
            <td class="col-itemname text-left">${escapeHtml(item.itemname)}</td>
            <td class="col-unit">${escapeHtml(item.unit)}</td>
        </tr>
        `;

        const $row = $(rowHtml);

        $row.on('click', function () {
            $('.data-table tr.row-selected').removeClass('row-selected');
            $row.addClass('row-selected');
            fetchLotno(item.itemcode);
        });

        $tbody.append($row);
    });
}

/* =====================================================
 * SUPPLIER — 고객사 조회
 * ===================================================== */

function loadSuppliers() {
    $.ajax({
        url: '/suppliers',
        type: 'GET',
        dataType: 'json',
        success: function (list) {
            const $sel = $('#supplier').empty();
            list.forEach(function (s) {
                $sel.append($('<option>').val(s).text(s));
            });
        }
    });
}

// LABEL TYPE(PT)별 양식 — 실제 규칙으로 교체
function buildLabelTypeFormats() {
    return {
        CART_OUT: `CAR,CUSTOMERCODE,ITEMCODE,LOTQTY,LOTNO,WBT`,        // 대차
        CART_IN: `CAR,CUSTOMERCODE,ITEMCODE,LOTQTY,LOTNO,WBT`,        // 대차
        CART_SMALL: `CAR,CUSTOMERCODE,ITEMCODE,LOTQTY,LOTNO,WBT`,        // 대차
        HEADREST: `HKMC 2D 바코드 - 업체코드, 품번, 추적번호, 수량`
    };
}

let labelTypeFormats = null;

// country별 추가 입력 영역 제어
function setupCountryExtras(country) {
    const $wrap = $('#ptExtraWrap');
    const $ptExtra = $('#ptExtra');

    if (country === 'PT') {
        $ptExtra.empty()
            .append($('<option>').val('').text('선택'))
            .append($('<option>').val('CART_OUT').text('대차(출고 일반)'))
            .append($('<option>').val('CART_IN').text('대차(내부 일반)'))
            .append($('<option>').val('CART_SMALL').text('대차(내부 소형)'))
            .append($('<option>').val('HEADREST').text('부품'))
        $wrap.show();

        labelTypeFormats = buildLabelTypeFormats();   // ← 양식 준비
        updateLabelTypeExample();                     // ← 초기 표시
    } else {
        $wrap.hide();
        $ptExtra.empty();
    }
}

// LABEL TYPE 선택값에 따라 안내창 갱신
function updateLabelTypeExample() {
    const selected = $('#ptExtra').val();
    const formats = labelTypeFormats || {};

    // 미선택('')이면 숨김
    if (!selected) {
        // 공간은 유지하고 내용만 비움 → 아래가 안 밀림
        $('#ptExample').css('visibility', 'hidden').text('');
    } else {
        $('#ptExample').css('visibility', 'visible').text(formats[selected] || '');
    }
}

/* =====================================================
 * LOTNO — 로트번호 조회
 * ===================================================== */

function fetchLotno(itemcode) {
    if (!itemcode) return;

    const requestBody = {
        date: $('#lotDate').val(),
        itemcode: itemcode
    };

    // 스피너 표시 + input 비우기
    $('#lotnoSpinner').addClass('active');
    $('#lotno').val('');

    $.ajax({
        url: '/lot/next',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestBody),
        dataType: 'text',
        success: function (result) {
            $('#lotno').val(result);
        },
        error: function (xhr, status, error) {
            console.error('LOTNO fetch failed:', status, error, xhr.responseText);
            Modal.alert('Failed to fetch LOTNO.');
        },
        complete: function () {
            $('#lotnoSpinner').removeClass('active');  // 항상 스피너 숨김
        }
    });
}

// 선택된 품번 기준으로 lotno 다시 조회
function refreshLotno() {
    const row = $('.data-table tr.row-selected');
    if (row.length === 0) return;
    fetchLotno(row.find('.col-itemcode').text());
}

/* =====================================================
 * QTY — 수량 계산 / 검증
 * ===================================================== */

// TOTAL QTY = LOT QTY × PRINT QTY
function updateTotalQty() {
    const lotQty = parseInt($('#lotQty').val(), 10) || 0;
    const printQty = parseInt($('#printQty').val(), 10) || 0;
    const total = lotQty * printQty;

    // 천 단위 콤마 포함해서 표시
    $('#totalQty').text(total.toLocaleString('en-US'));
}

// 값 검증/보정 (min 미만 → min, max 초과 → max)
function validateQty($input, min, max) {
    let value = parseInt($input.val(), 10);

    if (isNaN(value) || value < min) {
        value = min;
    } else if (max !== null && value > max) {
        value = max;
    }

    $input.val(value);
}


/* =====================================================
 * GUIDE — 가이드 옵션 강조
 * ===================================================== */

// 가이드별 바코드 양식 (고정 텍스트 — 실제 값 아님, 생성 형식 안내)
// country → WMS 접미사
const WMS_SUFFIX = { USA: 'WMSUSA', PT: 'WMSKOR', MEX: 'WMSMEX' };

// country 확정되면 채워짐 (응답 전엔 USA 기준 기본값 사용)
let guideFormats = null;

function buildGuideFormats(country) {
    const wms = WMS_SUFFIX[country] || 'WMSUSA';
    return {
        OFF:    ``,
        PALLET: `P+SERIAL,ITEMCODE,TOTALQTY,${wms}`,
        BOX:    'DD_MM_YYYY_CUSTOMERCODE_LOTQTY_LOTNO'   // WMS 미포함
    };
}

function updateGuideHighlight() {
    const selected = $('#guideOption').val();

    $('.guide-line').removeClass('guide-active');
    $('.guide-line[data-option="' + selected + '"]').addClass('guide-active');

    // country 응답 전이면 USA 기준 기본값
    const formats = guideFormats || buildGuideFormats('USA');
    // OFF면 자리는 유지하고 내용만 숨김
    if (selected === 'OFF') {
        $('#guideExample').css('visibility', 'hidden').text('');
    } else {
        $('#guideExample').css('visibility', 'visible').text(formats[selected] || '');
    }
}


/* =====================================================
 * PRINT — 라벨 발행
 * ===================================================== */
async function print() {
    const row = $('.data-table tr.row-selected');

    if (row.length === 0) {
        Modal.alert('Please select an item from the table.');
        return;
    }

    // PT(라벨 타입 영역이 보일 때)만 LABEL TYPE 필수 검증
    const $ptWrap = $('#ptExtraWrap');
    if ($ptWrap.is(':visible') && !$('#ptExtra').val()) {
        Modal.alert('Please select a label type.');
        return;
    }

    const printData = {
        // 기본 정보
        car:       row.find('.col-car').text(),
        itemcode:  row.find('.col-itemcode').text(),
        itemname:  row.find('.col-itemname').text(),
        unit:      row.find('.col-unit').text(),
        spec:      row.find('.col-spec').text(),

        // 추가 정보
        lotDate:   $('#lotDate').val(),
        lotno:     $('#lotno').val(),
        lotQty:    parseInt($('#lotQty').val(), 10),
        printQty:  parseInt($('#printQty').val(), 10),
        totalQty:  parseInt($('#totalQty').text().replace(/,/g, ''), 10),
        supplier:  $('#supplier').val(),

        // 바코드 타입
        labelType: $('#ptExtra').val() || '',
        guide:     $('#guideOption').val(),

        // 로그인한 사용자
        loginid:    getCookie('userId')
    };

    // SPEC에 한글 포함 시 중단
    if (/[ㄱ-ㅎㅏ-ㅣ가-힣]/.test(printData.spec)) {
        Modal.alert('고객사 품번에 한글이 포함되어 바코드 생성이 불가능합니다.');
        return;
    }

    // 확인 모달용 표 HTML
    const data = [
        ['ITEM CODE', printData.itemcode],
        ['ITEM NAME', printData.itemname],
        ['SUPPLIER', printData.supplier],
        ['LOT DATE',  printData.lotDate],
        ['LOTNO',     printData.lotno],
        ['LOT QTY',   printData.lotQty.toLocaleString('en-US')],
        ['PRINT QTY', printData.printQty.toLocaleString('en-US')],
        ['TOTAL QTY', printData.totalQty.toLocaleString('en-US')],
    ];

    // PT면 LABEL TYPE도 표에 추가
    if (printData.labelType) {
        data.push(['LABEL TYPE', $('#ptExtra option:selected').text()]);
    }

    data.push(['GUIDE', printData.guide]);


    const tableHtml = `
        <table class="cmodal-table">
            ${data.map(function (r) {
                return `
                    <tr>
                        <th>${r[0]}</th>
                        <td>${escapeHtml(r[1])}</td>
                    </tr>`;
            }).join('')}
        </table>
    `;

    const ok = await Modal.confirm({
        title: 'CONFIRM LABEL PRINTING',
        html: tableHtml,
        okText: 'PRINT',
        cancelText: 'CANCEL'
    });

    if (!ok) return;

    doPrint(printData);
}
function doPrint(printData) {
    if (!printData) return;

    showLoading();

    $.ajax({
        url: '/barcode/create',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(printData),
        dataType: 'json',
        success: function (result) {
            console.log('Barcodes created:', result);
            openPrintModal(result, printData);   // 출력은 모달에서
        },
        error: function (xhr, status, error) {
            console.error('Barcode create failed:', status, error, xhr.responseText);
            Modal.alert('Barcode create failed: ' + (xhr.responseText || error));
        },
        complete: hideLoading
    });
}

// 출력 모달 열기
function openPrintModal(result, printData) {
    const items = [];
    if (result.part)   items.push({ label: 'PRINT LABEL',   barcodes: result.part,   type: 'part' });
    if (result.pallet) items.push({ label: 'PRINT PALLET LABEL', barcodes: result.pallet, type: 'pallet' });
    if (result.box)    items.push({ label: 'PRINT BOX LABEL',    barcodes: result.box,    type: 'box' });

    const $buttons = $('#printModalButtons');
    $buttons.empty();

    items.forEach(function (item) {
        const $btn = $('<button class="print-modal-btn print-label-btn"></button>')
            .text(item.label)
            .on('click', function () {
                // 사용자 클릭에서 바로 window.open → 팝업 차단 안 됨
                openLabelPdf(item.barcodes, printData, item.type);
                // 출력 완료 표시
                $btn.addClass('printed').prop('disabled', false);
            });
        $buttons.append($btn);
    });

    $('#printModal').addClass('active');
}

// PDF 새 창에서 열기
function openLabelPdf(barcodes, printData, type) {
    const params = new URLSearchParams({
        barcodes: barcodes.join(';'),
        qty: printData.lotQty,
        labelType: printData.labelType,
        guide: type
    });

    const url = '/label/print?' + params.toString();

    const width = 900;
    const height = 800;

    // part는 왼쪽, 그 외(pallet/box)는 오른쪽
    const left = (type === 'part') ? 0 : (screen.availWidth - width);
    const top = 0;

    const features = `
        width=${width},
        height=${height},
        left=${left},
        top=${top},
        resizable=yes,
        scrollbars=yes
    `;

    window.open(url, 'labelPreview_' + type, features);
}

// 출력 모달 닫기 — 미출력 안내
function closePrintModal() {
    // 아직 안 누른 출력 버튼이 있는지 확인
    const $notPrinted = $('#printModalButtons .print-label-btn').not('.printed');

    if ($notPrinted.length > 0) {
        Modal.confirm({
            title: 'UNPRINTED LABELS',
            message: 'Some labels have not been printed yet.\nClose anyway?',
            okText: 'CLOSE',
            cancelText: 'CANCEL'
        }).then(function (ok) {
            if (ok) {
                $('#printModal').removeClass('active');
                refreshLotno();   // ← 닫은 뒤 lotno 재조회
            }
        });
        return;
    }

    $('#printModal').removeClass('active');
    refreshLotno();   // ← 닫은 뒤 lotno 재조회
}

/* =====================================================
 * LOADING — 로딩 오버레이
 * ===================================================== */

function showLoading() {
    $('#loadingOverlay').addClass('active');
}

function hideLoading() {
    $('#loadingOverlay').removeClass('active');
}


/* =====================================================
 * COOKIE — 쿠키 저장 / 읽기
 * ===================================================== */

// 쿠키 저장 (기본 365일)
function setCookie(name, value, days) {
    days = days || 365;
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = name + '=' + encodeURIComponent(value) +
        '; expires=' + expires + '; path=/';
}

// 쿠키 읽기 (없으면 null)
function getCookie(name) {
    const match = document.cookie.match('(?:^|; )' + name + '=([^;]*)');
    return match ? decodeURIComponent(match[1]) : null;
}


/* =====================================================
 * UTILS — 유틸
 * ===================================================== */

// 오늘 날짜를 'YYYY-MM-DD' 형식으로 반환
function getTodayString() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

function escapeHtml(value) {
    if (value == null) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}