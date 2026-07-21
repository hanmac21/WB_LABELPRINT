package com.example.wb_labelprint.service;

import com.example.wb_labelprint.config.datasource.DbContextHolder;
import com.example.wb_labelprint.config.datasource.DbType;
import com.example.wb_labelprint.mapper.ItemMapper;
import com.example.wb_labelprint.mapper.kor.ItemKorMapper;
import com.example.wb_labelprint.mapper.mex.ItemMexMapper;
import com.example.wb_labelprint.mapper.pol.ItemPolMapper;
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

    public ItemServiceImpl(ItemUsaMapper usaMapper, ItemMexMapper mexMapper, ItemPolMapper polMapper, ItemKorMapper korMapper) {
        this.mapperRegistry = Map.of(
                DbType.USA, usaMapper,
                DbType.MEX, mexMapper,
                DbType.POL, polMapper,
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
        String yymmdd = date.replace("-", "").substring(2);

        // 00001 => 1
        int startLot = Integer.parseInt(param.getLotno());
        int currentLot = 0;

        for (int i = 0; i < param.getPrintQty(); i++){
            currentLot = startLot + i;

            String lotno   = buildLotno(dbType, param.getLabelType(), date, yymmdd, currentLot);
            String barcode = buildBarcode(dbType, param, yymmdd, lotno);

            mapper.insertBarcode(toBarcodeMap(param, date, barcode, lotno));
            barcodeList.add(barcode);
        }

        // t_scm_barcode_max 값 업데이트
        System.out.println(currentLot);
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("itemcode", param.getItemcode());
        itemInfo.put("lotno", currentLot);
        itemInfo.put("sdate", param.getLotDate());

        mapper.mergeBarcodeMax(itemInfo);

        return barcodeList;
    }

    // LOTNO 생성 로직
    private String buildLotno(String dbType, String labelType, String date, String yymmdd, int currentLot) {
        return switch (dbType) {
            case "USA" -> String.valueOf(currentLot);
            case "MEX" -> String.format("%05d", currentLot);
            case "PT"  -> switch (labelType) {
                case "HEADREST" -> String.format("%07d", currentLot);
                case "CUST"     -> date.replace("-", "") + String.format("%05d", currentLot);
                case "CART_IN", "CART_OUT", "CART_SMALL"
                                -> "P" + yymmdd + String.format("%05d", currentLot);
                default         -> String.valueOf(currentLot);
            };
            default -> "";
        };
    }

    // 바코드 생성 로직
    private String buildBarcode(String dbType, PrintVO param, String yymmdd, String lotno) {
        String spec   = param.getSpec();
        int    lotQty = param.getLotQty();

        return switch (dbType) {
            case "USA" -> {
                String[] date = param.getLotDate().split("-");
                yield String.join("_", date[2], date[1], date[0], spec, String.valueOf(lotQty), lotno);
            }
            case "MEX" -> String.join(",", param.getItemcode(), yymmdd, lotno, String.format("%08.2f", (double) lotQty), "WMSMEX");
            case "PT" -> switch (param.getLabelType()) {
                case "CART_OUT", "CART_IN", "CART_SMALL"
                        -> String.join(",", param.getCar(), spec, param.getItemcode(), String.format("%05d", lotQty), lotno, "WBT");
                case "HEADREST"
                        -> "[)>\u001E06" + "\u001DV" + "SLBJ" + "\u001DP" + spec + "\u001DS" + "\u001DE"+ "\u001DT" + yymmdd + "LX31" + "A" + lotno
                        + "\u001DM" + "N" + "\u001DC" + "W0001"  + "\u001D" + "\u001E" + "\u0004";
                case "CUST"
                        -> String.join(",", param.getCar(), param.getItemcode(), lotno, String.valueOf(lotQty), "WBT");
                default -> "";
            };
            default -> "";
        };
    }

    // 바코드 생성용 map
    private Map<String, Object> toBarcodeMap(PrintVO param, String date, String barcode, String lotno) {
        Map<String, Object> map = new HashMap<>();
        map.put("barcode", barcode);
        map.put("sdate", date);
        map.put("itemcode", param.getItemcode());
        map.put("itemname", param.getItemname());
        map.put("qty", param.getLotQty());
        map.put("totalqty", param.getTotalQty());
        map.put("factory", "WBTA");
        map.put("custname", param.getSupplier());
        map.put("lotno", lotno);
        map.put("spec", param.getSpec());
        map.put("loginid", param.getWorker() == null ? param.getLoginid() : param.getWorker());
        map.put("indate", param.getIndate() == null || param.getIndate().isEmpty() ? date : param.getIndate());
        return map;
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
