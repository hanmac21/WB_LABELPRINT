package com.example.wb_labelprint.config.datasource;

// 현재 요청이 어느 DB를 사용할지 ThreadLocal에 저장
// Interceptor가 set, RoutingDataSource가 get, 요청 종료 시 clear
public class DbContextHolder {

    // Thread 별로 DbType을 보관해 섞이지 않음
    private static final ThreadLocal<DbType> contextHolder = new ThreadLocal<>();

    public static void set(DbType dbType){
        contextHolder.set(dbType);
    }

    public static DbType get(){
        DbType dbType = contextHolder.get();
        return dbType != null ? dbType : DbType.USA;  // USA 기본값
    }

    public static void clear(){
        contextHolder.remove();
    }
}
