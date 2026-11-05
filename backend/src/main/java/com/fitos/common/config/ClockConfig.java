package com.fitos.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Clock dùng chung → test có thể cố định thời gian. Múi giờ studio: Asia/Ho_Chi_Minh. */
@Configuration
public class ClockConfig {

    public static final ZoneId STUDIO_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.system(STUDIO_ZONE);
    }
}
