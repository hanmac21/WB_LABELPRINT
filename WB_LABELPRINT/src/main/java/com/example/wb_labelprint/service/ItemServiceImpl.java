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

import java.util.*;

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
    public Map<String, String> getItemInfo(ItemVO param){
        return mapper().getItemInfo(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, List<String>> createBarcodes(PrintVO param) {
        ItemMapper mapper = mapper();
        String guide = param.getGuide();

        Map<String, List<String>> result = new LinkedHashMap<>();

        switch (guide) {
            case "OFF":
                result.put("part", createPart(param, mapper));
                break;
            case "PALLET": {
                List<String> partBarcodes = createPart(param, mapper);
                result.put("part", partBarcodes);
                result.put("pallet", createPallet(param, mapper, partBarcodes));
                break;
            }
            case "BOX":
                List<String> partBarcodes = createPart(param, mapper);
                result.put("part", partBarcodes);
                result.put("box", partBarcodes);
                break;
            default:
                throw new IllegalStateException("지원하지 않는 바코드 양식: " + guide);
        }

        return result;
    }

    // OFF : 파트라벨 생성
    private List<String> createPart(PrintVO param, ItemMapper mapper) {
        List<String> barcodeList = new ArrayList<>();

        // 접속 DB정보 가져오기
        String dbType = String.valueOf(DbContextHolder.get());

        // 날짜 추출
        String date = param.getLotDate();
        String[] dateParts = date.split("-");
        String year = dateParts[0];
        String month = dateParts[1];
        String day = dateParts[2];
        String yymmdd = date.replace("-", "").substring(2);

        int printQty = param.getPrintQty();
        int lotQty = param.getLotQty();
        String itemcode = param.getItemcode();
        String spec = param.getSpec();
        String labelType = param.getLabelType();
        String car = param.getCar();
        String loginid = param.getLoginid();

        // 00001 => 1
        int startLot = Integer.parseInt(param.getLotno());
        int currentLot = 0;

        for (int i = 0; i < printQty; i++){
            currentLot = startLot + i;

            String barcode = "";
            // 미국
            if ("USA".equals(dbType)) {
                barcode = String.join("_", day, month, year, spec, String.valueOf(lotQty), String.valueOf(currentLot));
            }
            // 멕시코
            else if ("MEX".equals(dbType)) {
                barcode = String.join(",", itemcode, yymmdd, String.format("%05d", currentLot), String.format("%08.2f", (double)lotQty), "WMSMEX");
            }
            // 한국
            else if ("PT".equals(dbType)) {
                barcode = switch (labelType) {
                    case "CART_OUT", "CART_IN", "CART_SMALL"
                            -> String.join(",", car, spec, itemcode, String.format("%05d", lotQty), "P" + yymmdd + String.format("%05d", currentLot), "WBT");
                    case "LEAR"
                            -> "TEST";
                    default -> barcode;
                };
            }

            Map<String, Object> map = new HashMap<>();
            map.put("barcode", barcode);
            map.put("sdate", date);
            map.put("itemcode", itemcode);
            map.put("itemname", param.getItemname());
            map.put("qty", lotQty);
            map.put("totalqty", param.getTotalQty());
            map.put("factory", "WBTA");
            map.put("custname", param.getSupplier());
            map.put("lotno", "PT".equals(dbType) ? "P" + yymmdd + String.format("%05d", currentLot) : currentLot);
            map.put("spec", spec);
            map.put("loginid", loginid);

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
    private List<String> createPallet(PrintVO param, ItemMapper mapper, List<String> partBarcodes) {
        String date = param.getLotDate();
        String bdate = date.replace("-", "").substring(2);
        String itemcode = param.getItemcode();
        String custname = param.getSupplier();
        String qty = String.valueOf(param.getLotQty());
        double totalqty = param.getTotalQty();

        String lastPalletBarcode = mapper.selectPalletSeq(date);
        int palletSeq = 0;
        if (lastPalletBarcode != null && !lastPalletBarcode.isBlank()) {
            String[] barcodeParts = lastPalletBarcode.split(",");
            if (barcodeParts.length == 4) {
                String palletNo = barcodeParts[0];
                palletSeq = Integer.parseInt(palletNo.substring(palletNo.length() - 5));
            }
        }
        palletSeq++;

        String pbarcode = "P" + date.replace("-", "").substring(2)
                + String.format("%05d", palletSeq) + ","
                + itemcode + "," + String.format("%08.2f", totalqty) + ",WMSUSA";


        for (String barcode : partBarcodes) {   // 넘겨받은 파트라벨 사용
            String barcodeSeq = barcode.split("_")[5];

            Map<String, Object> map = new HashMap<>();
            map.put("pbarcode", pbarcode);
            map.put("barcode", barcode);
            map.put("loginid", "woobo");
            map.put("seq", barcodeSeq);
            map.put("sdate", date);
            map.put("bdate", bdate);
            map.put("itemcode", itemcode);
            map.put("custcode", "A021");
            map.put("custname", custname);
            map.put("qty", qty);
            map.put("scmmex", "WMSUSA");
            map.put("labelType", "EXCEPTIONIN");
            map.put("factory", "WBTA");
            map.put("laststatus", "1");

            mapper.insertPalletBarcode(map);
        }

        // 팔레트 바코드 하나만 리스트로 반환
        List<String> palletList = new ArrayList<>();
        palletList.add(pbarcode);
        return palletList;
    }

    // BOX : 박스 라벨 생성
    private List<String> createBox(PrintVO param, ItemMapper mapper) {
        List<String> barcodeList = new ArrayList<>();
        // BOX 전용 로직
        return barcodeList;
    }
}
