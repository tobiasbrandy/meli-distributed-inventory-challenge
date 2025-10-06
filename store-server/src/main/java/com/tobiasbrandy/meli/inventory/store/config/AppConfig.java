package com.tobiasbrandy.meli.inventory.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfig(String storeId) {
}

