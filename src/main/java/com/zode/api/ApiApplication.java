package com.zode.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@SpringBootApplication
@ComponentScan(basePackages ={"com.zode","com.jfinal","com.db","com.common","com.file"})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
        System.out.println("启动成功(●'◡'●)");
    }
}
