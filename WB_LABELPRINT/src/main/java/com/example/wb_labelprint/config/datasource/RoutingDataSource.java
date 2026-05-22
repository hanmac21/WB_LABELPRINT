package com.example.wb_labelprint.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

// 매 쿼리마다 어느 DB를 사용할지 결정
// DbContextHolder에 따라 등록된 DataSource 중 하나를 선택해 사용
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected  Object determineCurrentLookupKey(){
        return DbContextHolder.get();
    }
}
