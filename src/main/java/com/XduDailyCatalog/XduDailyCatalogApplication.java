package com.XduDailyCatalog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.XduDailyCatalog.mapper")
@SpringBootApplication
public class XduDailyCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(XduDailyCatalogApplication.class, args);
    }

}
