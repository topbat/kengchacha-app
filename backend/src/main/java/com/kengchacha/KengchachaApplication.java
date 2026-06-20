package com.kengchacha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 坑查查（KENG CHACHA）后端启动类 —— 模块化单体。
 * 模块边界：content / quiz / ugc / assistant / refresh，
 * 即未来拆分微服务的切割线（对齐技术方案 §2.6）。
 */
@SpringBootApplication
public class KengchachaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KengchachaApplication.class, args);
    }
}
