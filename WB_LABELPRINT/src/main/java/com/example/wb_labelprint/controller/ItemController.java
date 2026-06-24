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
    public void labelPrint(HttpServletResponse response, HttpServletRequest request,
                           @RequestParam Map<String, String> param) {

        // 라벨 양식 종류 (part / pallet / box)
        String type = param.getOrDefault("type", "part");

        // 종류별 템플릿 선택
        String templatePath;
        switch (type) {
            case "pallet":
                templatePath = "C:/reportILPS/WB_Label_Pallet.jrxml";   // 팔레트 양식
                break;
            case "box":
                templatePath = "C:/reportILPS/WB_Label_Boxlabel.jrxml";      // 박스 양식
                break;
            case "part":
            default:
                templatePath = "C:/reportILPS/WB_Label_10x8.jrxml";     // 기존 파트 양식
                break;
        }
        System.out.println("type : " + type + ", templatePath : " + templatePath);

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
            if ("pallet".equals(type)) {
                paramMap.put("pbarcode", barcodeForSql);
            } else {
                paramMap.put("barcode", barcodeForSql);
            }

            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@45.58.2.218:1521:WBUSA", "wbusa", "woobo23300usa");
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

}
