package com.example.wb_labelprint.mapper.usa;

import com.example.wb_labelprint.vo.ItemVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemUsaMapper {
    List<ItemVO> search(ItemVO itemVO);

    String getNextLotNo(ItemVO param);

    void insertBarcode(Map<String, Object> map);

    void mergeBarcodeMax(Map<String, Object> itemInfo);
}
