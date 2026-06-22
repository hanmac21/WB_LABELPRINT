package com.example.wb_labelprint.service;

import com.example.wb_labelprint.config.datasource.DbContextHolder;
import com.example.wb_labelprint.config.datasource.DbType;
import com.example.wb_labelprint.mapper.ItemMapper;
import com.example.wb_labelprint.mapper.kor.ItemKorMapper;
import com.example.wb_labelprint.mapper.mex.ItemMexMapper;
import com.example.wb_labelprint.mapper.usa.ItemUsaMapper;
import com.example.wb_labelprint.vo.ItemVO;
import com.example.wb_labelprint.vo.PrintVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.BooleanUtils.OFF;

@Service
public class ItemServiceImpl implements ItemService {

    private final Map<DbType, ItemMapper> mapperRegistry;

    public ItemServiceImpl(ItemUsaMapper usaMapper, ItemMexMapper mexMapper, ItemKorMapper korMapper) {
        this.mapperRegistry = Map.of(
                DbType.USA, usaMapper,
                DbType.MEX, mexMapper,
                DbType.PT, korMapper
        );
    }

    // 현재 컨텍스트에 맞는 Mapper 반환
    private ItemMapper mapper() {
        DbType type = DbContextHolder.get();
        ItemMapper m = mapperRegistry.get(type);
        if (m == null) {
            throw new IllegalStateException("지원하지 않는 DbType: " + type);
        }
        return m;
    }

    @Override
    public List<ItemVO> search(ItemVO itemVO) {
        return mapper().search(itemVO);
    }

    @Override
    public String getNextLotNo(ItemVO param) {
        System.out.println(param);
        String maxLotno = mapper().getNextLotNo(param);

        if (maxLotno == null || maxLotno.isBlank()){
            return "00001";
        }

        int next = Integer.parseInt(maxLotno) + 1;
        return String.format("%05d", next);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> createBarcodes(PrintVO param) {
        ItemMapper mapper = mapper();

        System.out.println("바코드 생성");
        System.out.println(param);

        String guide = param.getGuide();

        switch(guide) {
            case "OFF":
                return createPart(param, mapper);
            case "PALLET":
                return createPallet(param, mapper);
            case "BOX":
                return createBox(param, mapper);
            default:
                throw new IllegalStateException("지원하지 않는 바코드 양식: " + guide);
        }
    }

    // OFF : 파트라벨 생성
    private List<String> createPart(PrintVO param, ItemMapper mapper) {
        List<String> barcodeList = new ArrayList<>();
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

            mapper.insertBarcode(map);
            barcodeList.add(barcode);
        }

        // t_scm_barcode_max 값 업데이트
        System.out.println(currentLot);
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("itemcode", itemcode);
        itemInfo.put("lotno", currentLot);
        itemInfo.put("sdate", param.getLotDate());

        mapper.mergeBarcodeMax(itemInfo);

        return barcodeList;
    }


    // PALLET : 팔레트 라벨 생성
    private List<String> createPallet(PrintVO param, ItemMapper mapper) {
        List<String> barcodeList = createPart(param, mapper);   // 기존 처리 재사용

        //A001252110101,260422,00004,00001.00,WMSUSA
        //A001252110101,260422,00003,00001.00,WMSUSA
        //A001252110101,260422,00002,00001.00,WMSUSA
        //A001252110101,260422,00001,00001.00,WMSUSA



        return barcodeList;
    }

    // BOX : 박스 라벨 생성
    private List<String> createBox(PrintVO param, ItemMapper mapper) {
        List<String> barcodeList = new ArrayList<>();
        // BOX 전용 로직
        return barcodeList;
    }
}
