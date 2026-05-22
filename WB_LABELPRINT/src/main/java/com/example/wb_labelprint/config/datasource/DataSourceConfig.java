package com.example.wb_labelprint.config.datasource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

// 다중 DB 설정
@Configuration
public class DataSourceConfig {

    // USA DB
    @Bean(name = "usaDataSource")
    @ConfigurationProperties(prefix = "datasource.usa")
    public DataSource usaDataSource(){
        return DataSourceBuilder.create().build();
    }

    // MEX DB
    @Bean(name = "mexDataSource")
    @ConfigurationProperties(prefix = "datasource.mex")
    public DataSource mexDataSource(){
        return DataSourceBuilder.create().build();
    }

    // 두 DataSource를 묶는 라우팅 DataSource (실제로 사용되는 메인
    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource(){
        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DbType.USA, usaDataSource());
        targetDataSources.put(DbType.MEX, mexDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(usaDataSource());          // 기본값
        return routingDataSource;
    }

    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory() throws Exception{
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(routingDataSource());

        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml")
        );

        factoryBean.setTypeAliasesPackage("com.example.wb_labelprint.vo");

        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(config);

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory){
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public PlatformTransactionManager transactionManager(){
        return new DataSourceTransactionManager(routingDataSource());
    }
}
