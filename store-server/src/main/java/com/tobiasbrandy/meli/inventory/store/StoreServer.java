package com.tobiasbrandy.meli.inventory.store;

import com.tobiasbrandy.meli.inventory.messaging.EventStreams;
import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication(scanBasePackages = "com.tobiasbrandy.meli.inventory")
@EntityScan(basePackages = "com.tobiasbrandy.meli.inventory")
@EnableJpaRepositories(basePackages = "com.tobiasbrandy.meli.inventory")
@ConfigurationPropertiesScan(basePackages = "com.tobiasbrandy.meli.inventory")
@EnableScheduling
public class StoreServer {
    @SuppressWarnings("UnnecessaryModifier")
    public static void main(final String[] args) {
        SpringApplication.run(StoreServer.class, args);
    }

    @Bean
    public String consumerGroup(final AppConfig appConfig) {
        return "cg-" + appConfig.storeId();
    }

    @Bean
    public List<String> consumerStreams(final AppConfig appConfig, final EventStreams streams) {
        return List.of(
            streams.centralToStore(appConfig.storeId()),
            streams.storeToStore(appConfig.storeId(), appConfig.storeId())
        );
    }
}
