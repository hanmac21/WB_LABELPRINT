package com.example.wb_labelprint.service;

import com.example.wb_labelprint.vo.ItemVO;
import com.example.wb_labelprint.vo.PrintVO;

import java.util.List;
import java.util.Map;

public interface ItemService {
    List<ItemVO> search(ItemVO itemVO);

    String getNextLotNo(ItemVO param);

    Map<String, List<String>> createBarcodes(PrintVO param);

    Map<String, String> getItemInfo(ItemVO param);
}
