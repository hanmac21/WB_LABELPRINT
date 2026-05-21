$(document).ready(function () {

    // 오늘 날짜 선택
    const today = getTodayString();
    $('#lotDate').val(today);
    //$('#lotDate').attr('min', today);   // 오늘 이전 날짜 선택 불가

    // 페이지 진입 시 자동 검색
    search();

    // 초기 TOTAL QTY 계산
    updateTotalQty();

    $('#btnSearch').on('click', function () {
        search();
    });

    $('#btnPrint').on('click', function () {
        print();
    });

    $('#itemcode, #spec, #itemname').on('keydown', function (e) {
        if (e.key === 'Enter') {
            search();
        }
    });

    // LOT DATE 변경 시 선택된 행 기준으로 LOTNO 재조회
    $('#lotDate').on('change', function () {
        const $selectedRow = $('.data-table tr.row-selected');

        if ($selectedRow.length === 0) {
            return;  // 선택된 행 없으면 아무것도 안 함
        }

        const item = $selectedRow.data('item');
        fetchLotno(item.itemcode);
    });

    // ============ LOT QTY / PRINT QTY 자동 계산 ============
    // 입력 중에도 실시간 계산
    $('#lotQty, #printQty').on('input', function () {
        updateTotalQty();
    });

    // 포커스 떠날 때 값 검증 및 보정
    $('#lotQty').on('blur', function () {
        validateQty($(this), 1, null);   // 최소 1, 최대 제한 없음
        updateTotalQty();
    });

    $('#printQty').on('blur', function () {
        validateQty($(this), 1, 100);    // 최소 1, 최대 100
        updateTotalQty();
    });

    // 음수 부호, 'e' 등 숫자 외 키 차단
    $('#lotQty, #printQty').on('keydown', function (e) {
        if (['e', 'E', '+', '-', '.'].includes(e.key)) {
            e.preventDefault();
        }
    });

    // 값 검증/보정 함수
    function validateQty($input, min, max) {
        let value = parseInt($input.val(), 10);

        if (isNaN(value) || value < min) {
            value = min;
        } else if (max !== null && value > max) {
            value = max;
        }

        $input.val(value);
    }

    // TOTAL QTY = LOT QTY × PRINT QTY
    function updateTotalQty() {
        const lotQty = parseInt($('#lotQty').val(), 10) || 0;
        const printQty = parseInt($('#printQty').val(), 10) || 0;
        const total = lotQty * printQty;
        $('#totalQty').text(total);
    }

    // ============ 검색 ============
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
                alert('Search failed. Please check the console.');
            },
            complete: function () {
                hideLoading();
            }
        });
    }

    // 테이블 렌더링
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
                <td class="col-no">${index + 1}</td>
                <td>${escapeHtml(item.car)}</td>
                <td>${escapeHtml(item.itemcode)}</td>
                <td>${escapeHtml(item.spec)}</td>
                <td class="text-left">${escapeHtml(item.itemname)}</td>
                <td>${escapeHtml(item.unit)}</td>
            </tr>
            `;

            const $row = $(rowHtml);

            // ★ 행에 원본 item 객체 저장
            $row.data('item', item);

            $row.on('click', function () {
                $('.data-table tr.row-selected').removeClass('row-selected');
                $row.addClass('row-selected');

                fetchLotno(item.itemcode);
            });

            $tbody.append($row);
        });
    }

    // 로딩 표시
    function showLoading() {
        $('#loadingOverlay').addClass('active');
    }

    function hideLoading() {
        $('#loadingOverlay').removeClass('active');
    }

    // ============ LOTNO 조회 ============
    function fetchLotno(itemcode) {
        if (!itemcode) return;

        const lotDate = $('#lotDate').val();

        const requestBody = {
            date: lotDate,
            itemcode: itemcode
        };

        // ★ 스피너 표시 + input 비우기
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
                alert('Failed to fetch LOTNO.');
            },
            complete: function () {
                // ★ 항상 스피너 숨김
                $('#lotnoSpinner').removeClass('active');
            }
        });
    }

    // ============ 라벨 발행 ============
    function print() {
        const $selectedRow = $('.data-table tr.row-selected');

        if ($selectedRow.length === 0) {
            alert('Please select an item from the table.');
            return;
        }

        const item = $selectedRow.data('item');

        const printData = {
            // 품번 정보
            car:       item.car,
            itemcode:  item.itemcode,
            itemname:  item.itemname,
            unit:      item.unit,
            spec:      item.spec,

            // 발행 정보
            lotDate:   $('#lotDate').val(),
            lotno:     $('#lotno').val(),
            lotQty:    parseInt($('#lotQty').val(), 10),
            printQty:  parseInt($('#printQty').val(), 10),
            totalQty:  parseInt($('#totalQty').text(), 10),
            supplier:  $('#supplier').val()
        };

        console.log('Print data:', printData);

        showLoading();

        // 1단계: 바코드 생성 + DB INSERT
        $.ajax({
            url: '/barcode/create',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(printData),
            dataType: 'json',
            success: function (barcodes) {
                console.log('Barcodes created:', barcodes);

                // 2단계: 새 탭에서 PDF 열기
                openLabelPdf(barcodes, printData);
            },
            error: function (xhr, status, error) {
                console.error('Barcode create failed:', status, error, xhr.responseText);
                alert('Barcode create failed: ' + (xhr.responseText || error));
            },
            complete: function () {
                hideLoading();
            }
        });
    }

    // PDF 새 탭에서 열기
    function openLabelPdf(barcodes, printData) {
        const barcodesParam = barcodes.join(';');

        const params = new URLSearchParams({
            barcodes: barcodesParam,
            qty: printData.lotQty
        });

        const url = '/label/print?' + params.toString();

        // 창 크기 설정
        const width = 900;
        const height = 800;

        // 화면 중앙에 띄우기 위한 좌표 계산
        const left = (screen.width - width) / 2;
        const top = (screen.height - height) / 2;

        // 팝업 창 옵션
        const features = [
            `width=${width}`,
            `height=${height}`,
            `left=${left}`,
            `top=${top}`,
            'resizable=yes',
            'scrollbars=yes',
            'toolbar=no',
            'menubar=no',
            'location=no',
            'status=no'
        ].join(',');

        window.open(url, 'labelPreview', features);
    }


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
});