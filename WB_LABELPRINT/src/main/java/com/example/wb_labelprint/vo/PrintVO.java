package com.example.wb_labelprint.vo;

import lombok.Data;

import java.util.List;

@Data
public class PrintVO {
    // 품번 정보
    private String car;
    private String itemcode;
    private String itemname;
    private String unit;
    private String spec;

    // 발행 정보
    private String lotDate;
    private String lotno;
    private int lotQty;
    private int printQty;
    private int totalQty;
    private String supplier;

    // 바코드 타입
    private String labelType;
    private String guide;

    // 로그인 아이디
    private String loginid;

    // 작업자
    private String worker;

    // 라벨 출력 시 전달되는 바코드 리스트
    private List<String> barcodes;
}
