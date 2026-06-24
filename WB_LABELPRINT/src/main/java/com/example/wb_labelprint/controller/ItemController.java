package com.example.wb_labelprint.controller;

import com.example.wb_labelprint.service.ItemServiceImpl;
import com.example.wb_labelprint.vo.ItemVO;
import com.example.wb_labelprint.vo.PrintVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ItemController {
    @Autowired
    public ItemServiceImpl itemService;

    private final DataSource dataSource;   // routingDataSource (@Primary)

    public ItemController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/items/search")
    public List<ItemVO> search(@RequestBody ItemVO itemVO){
        return itemService.search(itemVO);
    }

    @PostMapping("/lot/next")
    public String getNextLotNo(@RequestBody ItemVO param){
        return itemService.getNextLotNo(param);
    }

    @PostMapping("/barcode/create")
    public Map<String, List<String>> createBarcodes(@RequestBody PrintVO param){
        return itemService.createBarcodes(param);
    }

    @GetMapping("/suppliers")
    @ResponseBody
    public List<String> suppliers(HttpSession session) {
        String country = (String) session.getAttribute("country");
        return switch (country == null ? "USA" : country) {
            case "MEX" -> List.of("WBMX");
            case "PT"  -> List.of("(주)우보테크", "리어코리아(유)");
            default    -> List.of("WBTM");
        };
    }

    @GetMapping("/label/print")
    @ResponseBody
    public void labelPrint(HttpServletResponse response, HttpServletRequest request, @RequestParam Map<String, String> param) {
        // 파트 라벨 타입 (대차, 리어, WMS)
        String labelType = param.getOrDefault("labelType", "");

        // 가이드 (OFF / PALLET / BOX)
        String guide = param.getOrDefault("guide", "part");

        // 종류별 템플릿 선택
        String templatePath;
        switch (guide) {
            case "pallet":
                templatePath = "C:/reportILPS/WB_Label_Pallet.jrxml";   // 팔레트 양식
                break;
            case "box":
                templatePath = "C:/reportILPS/WB_Label_Boxlabel.jrxml";      // 박스 양식
                break;
            case "part":
            default:
                // part 안에서 PT LABEL TYPE에 따라 양식 분기
                templatePath = resolvePartTemplate(labelType);
                break;
        }
        System.out.println("guide : " + guide + ", templatePath : " + templatePath);

        String barcodesRaw = param.get("barcodes");
        if (barcodesRaw == null || barcodesRaw.isBlank()) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "barcodes parameter is missing");
            } catch (Exception ignored) {}
            return;
        }

        String[] barcodeArr = barcodesRaw.split(";");
        List<String> barcodeList = Arrays.asList(barcodeArr);
        String barcodeForSql = barcodeList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));

        Connection conn = null;
        try {
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);

            Map<String, Object> paramMap = new HashMap<>();
            if ("pallet".equals(guide)) {
                paramMap.put("pbarcode", barcodeForSql);
            } else {
                paramMap.put("barcode", barcodeForSql);
            }

            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = dataSource.getConnection();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=label.pdf");

            JasperPrint print = JasperFillManager.fillReport(jasperReport, paramMap, conn);
            JasperExportManager.exportReportToPdfStream(print, response.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String resolvePartTemplate(String labelType) {
        return switch (labelType) {
            case "CART_OUT" -> "C:/reportILPS/WB_Label_Cart_out.jrxml";   // 대차
            case "CART_IN" -> "C:/reportILPS/WB_Label_Cart_in.jrxml";   // 대차
            case "LEAR" -> "C:/reportILPS/WB_Label_Lear.jrxml";   // 리어 파트
            default -> "C:/reportILPS/WB_Label_10x8.jrxml";   // WMS 파트, 기본 파트
        };
    }

}
