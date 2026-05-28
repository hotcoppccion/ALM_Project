package com.alm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling : MarketDataScheduler 의 @Scheduled(fixedRate=1000) 활성화에 필요.
 */
@SpringBootApplication
@EnableScheduling
public class AlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlmApplication.class, args);
    }
}
