package com.example.wb_usa_labelprint.controller;

import com.example.wb_usa_labelprint.service.ItemService;
import com.example.wb_usa_labelprint.vo.ItemVO;
import com.example.wb_usa_labelprint.vo.PrintVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public ItemService itemService;


    @PostMapping("/items/search")
    public List<ItemVO> search(@RequestBody ItemVO itemVO){
        return itemService.search(itemVO);
    }

    @PostMapping("/lot/next")
    public String getNextLotNo(@RequestBody ItemVO param){
        return itemService.getNextLotNo(param);
    }

    @PostMapping("/barcode/create")
    public List<String> createBarcodes(@RequestBody PrintVO param){
        return itemService.createBarcodes(param);
    }

    @GetMapping("/label/print")
    @ResponseBody
    public void labelPrint(HttpServletResponse response, HttpServletRequest request, @RequestParam Map<String, String> param) {
        // 파일 있는 곳에 pdf 파일 만들어줌
        String templatePath = "";
        String destPath = "";
        templatePath = "C:/reportUSA/WB_Label_10x8.jrxml";
        destPath = "/reportUSA/WB_Label_10x8.pdf";
        System.out.println("templatePath : "+templatePath);

        // 바코드 파라미터 파싱
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
            // jrxml → JasperReport 컴파일
            JasperReport jasperReport = JasperCompileManager.compileReport(templatePath);

            // Jasper에 전달할 파라미터
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("barcode", barcodeForSql);
            // paramMap.put("qmemo", URLDecoder.decode(qmemo, "utf-8"));

            System.out.println("@@@@@@@@@@여기@@@@@@@@@@@@@");
            System.out.println(paramMap.toString());

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
                if (conn != null && !conn.isClosed()) {   // ← null 체크 먼저
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
