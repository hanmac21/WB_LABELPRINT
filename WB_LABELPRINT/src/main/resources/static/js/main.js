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

    // 출력 창 닫기 모달
    $('#printModalClose').on('click', closePrintModal);
}


/* =====================================================
 * SEARCH & TABLE — 검색 / 테이블 렌더링
 * ===================================================== */

function search() {
    const requestBody = {
        itemcode: $('#itemcode').val().trim(),
        spec: $('#spec').val().trim(),
        itemname: $('#itemname').val().trim()
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
        const rowHtml = `
        <tr>
            <td class="col-no">${(index + 1).toLocaleString()}</td>
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
const GUIDE_FORMATS = {
    OFF:    'ITEMCODE,YYMMDD,LOTNO,LOTQTY,WMSUSA',
    PALLET: 'P+SERIAL,CUSTOMERCODE,TOTALQTY,WMSUSA',
    BOX:    'DD_MM_YYYY_CUSTOMERCODE_LOTQTY_LOTNO'
};

function updateGuideHighlight() {
    const selected = $('#guideOption').val();

    $('.guide-line').removeClass('guide-active');
    $('.guide-line[data-option="' + selected + '"]').addClass('guide-active');

    // 바코드 양식 표시
    $('#guideExample').text(GUIDE_FORMATS[selected] || '');
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
        guide:     $('#guideOption').val()
    };

    // 확인 모달용 표 HTML
    const data = [
        ['ITEM CODE', printData.itemcode],
        ['ITEM NAME', printData.itemname],
        ['LOT DATE',  printData.lotDate],
        ['LOTNO',     printData.lotno],
        ['LOT QTY',   printData.lotQty.toLocaleString('en-US')],
        ['PRINT QTY', printData.printQty.toLocaleString('en-US')],
        ['TOTAL QTY', printData.totalQty.toLocaleString('en-US')],
        ['GUIDE',     printData.guide]
    ];

    const tableHtml =
        '<table class="cmodal-table">' +
        data.map(function (r) {
            return '<tr><th>' + r[0] + '</th><td>' + escapeHtml(r[1]) + '</td></tr>';
        }).join('') +
        '</table>';

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
    if (result.part)   items.push({ label: 'PRINT PART LABEL',   barcodes: result.part,   type: 'part' });
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
        type: type
    });

    const url = '/label/print?' + params.toString();

    const features = [
        'width=900',
        'height=800',
        'resizable=yes',
        'scrollbars=yes',
        'toolbar=no',
        'menubar=no',
        'location=no',
        'status=no'
    ].join(',');

    // 창 이름을 type별로 다르게 → 파트/팔레트가 서로 다른 창에 뜸
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