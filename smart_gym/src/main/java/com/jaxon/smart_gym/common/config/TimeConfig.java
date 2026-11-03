package com.jaxon.smart_gym.common.config;

import java.time.Clock;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    Clock utcClock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
