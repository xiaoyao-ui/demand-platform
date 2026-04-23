package com.demand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.demand.**.mapper")
@EnableCaching // 开启缓存支持
public class DemandApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemandApplication.class, args);
    }
}
