package com.example.wb_labelprint.mapper;

import com.example.wb_labelprint.vo.ItemVO;

import java.util.List;
import java.util.Map;

public interface ItemMapper {
    List<ItemVO> search(ItemVO itemVO);
    String getNextLotNo(ItemVO param);
    void insertBarcode(Map<String, Object> map);
    void mergeBarcodeMax(Map<String, Object> itemInfo);

    String selectPalletSeq(String date);

    void insertPalletBarcode(Map<String, Object> map);

    Map<String, String> getItemInfo(ItemVO param);
}