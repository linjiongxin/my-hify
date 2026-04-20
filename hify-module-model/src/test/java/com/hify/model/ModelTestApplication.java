package com.hify.model;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hify.model")
@MapperScan("com.hify.model.mapper")
public class ModelTestApplication {
}
