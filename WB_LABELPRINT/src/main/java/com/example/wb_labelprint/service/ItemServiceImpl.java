package com.example.wb_labelprint.service;

import com.example.wb_labelprint.config.datasource.DbContextHolder;
import com.example.wb_labelprint.config.datasource.DbType;
import com.example.wb_labelprint.mapper.mex.ItemMexMapper;
import com.example.wb_labelprint.mapper.usa.ItemUsaMapper;
import com.example.wb_labelprint.vo.ItemVO;
import com.example.wb_labelprint.vo.PrintVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemUsaMapper itemUsaMapper;
    private final ItemMexMapper itemMexMapper;

    private boolean isMex() {
        return DbContextHolder.get() == DbType.MEX;
    }

    @Override
    public List<ItemVO> search(ItemVO itemVO) {
        if (isMex()){
            return itemMexMapper.search(itemVO);
        }
        return itemUsaMapper.search(itemVO);
    }

    @Override
    public String getNextLotNo(ItemVO param) {
        System.out.println(param);
        String maxLotno = isMex() ? itemMexMapper.getNextLotNo(param) : itemUsaMapper.getNextLotNo(param);

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
        String spec = param.getSpec();

        // 00001 => 1
        int startLot = Integer.parseInt(param.getLotno());
        int currentLot = 0;

    // 현재 요청에 사용할 Mapper 한 번만 판별
        boolean mex = isMex();

        for (int i = 0; i < printQty; i++){
            currentLot = startLot + i;
            String barcode = String.join("_", day, month, year, spec,
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
            map.put("spec", spec);

            if (mex) {
                itemMexMapper.insertBarcode(map);
            } else {
                itemUsaMapper.insertBarcode(map);
            }

            barcodeList.add(barcode);
        }

        // t_scm_barcode_max 값 업데이트
        System.out.println(currentLot);
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("itemcode", itemcode);
        itemInfo.put("lotno", currentLot);
        itemInfo.put("sdate", param.getLotDate());

        if (mex) {
            itemMexMapper.mergeBarcodeMax(itemInfo);
        } else {
            itemUsaMapper.mergeBarcodeMax(itemInfo);
        }

        return barcodeList;
    }
}
