package com.example.wb_usa_labelprint.service;

import com.example.wb_usa_labelprint.mapper.ItemMapper;
import com.example.wb_usa_labelprint.vo.ItemVO;
import com.example.wb_usa_labelprint.vo.PrintVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemService implements ItemServiceImpl {

    @Autowired
    public ItemMapper itemMapper;

    @Override
    public List<ItemVO> search(ItemVO itemVO) {
        return itemMapper.search(itemVO);
    }

    @Override
    public String getNextLotNo(ItemVO param) {
        System.out.println(param);
        String maxLotno = itemMapper.getNextLotNo(param);

        if (maxLotno == null || maxLotno.isBlank()){
            return "00001";
        }

        int next = Integer.parseInt(maxLotno) + 1;
        return String.format("%05d", next);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> createBarcodes(PrintVO param) {
        List<String> barcodeList = new ArrayList<>();

        System.out.println("바코드 생성");
        System.out.println(param);

        // 날짜 추출
        String[] dateParts = param.getLotDate().split("-");
        String year = dateParts[0];
        String month = dateParts[1];
        String day = dateParts[2];

        int printQty = param.getPrintQty();
        int lotQty = param.getLotQty();
        String itemcode = param.getItemcode();

        // 00001 => 1
        int startLot = Integer.parseInt(param.getLotno());
        int currentLot = 0;

        for (int i = 0; i < printQty; i++){
            currentLot = startLot + i;
            String barcode = String.join("_", day, month, year, itemcode,
                    String.valueOf(lotQty), String.valueOf(currentLot));

            Map<String, Object> map = new HashMap<>();
            map.put("barcode", barcode);
            map.put("sdate", param.getLotDate());
            map.put("itemcode", itemcode);
            map.put("itemname", param.getItemname());
            map.put("qty", lotQty);
            map.put("totalqty", param.getTotalQty());
            map.put("factory", "WBTA");
            map.put("custname", param.getSupplier());
            map.put("lotno", currentLot);
            map.put("spec", param.getSpec());

            itemMapper.insertBarcode(map);

            barcodeList.add(barcode);
        }

        // t_scm_barcode_max 값 업데이트
        System.out.println(currentLot);
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("itemcode", itemcode);
        itemInfo.put("lotno", currentLot);
        itemInfo.put("sdate", param.getLotDate());

        itemMapper.mergeBarcodeMax(itemInfo);

        throw new RuntimeException("롤백");
//        return barcodeList;
    }
}
