package com.example.wb_labelprint.service;

import com.example.wb_labelprint.vo.ItemVO;
import com.example.wb_labelprint.vo.PrintVO;

import java.util.List;

public interface ItemService {
    List<ItemVO> search(ItemVO itemVO);

    String getNextLotNo(ItemVO param);

    List<String> createBarcodes(PrintVO param);
}
