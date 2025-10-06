package com.tobiasbrandy.meli.inventory.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streams")
public record EventStreams (
    String storeToCentral,
    String centralToStore,
    String centralBroadcast,
    String storeToStore,
    String storeBroadcast
) {

    public String storeToCentral(final String storeId) {
        return storeToCentral.replace("{storeId}", String.valueOf(storeId));
    }
    public String centralToStore(final String storeId) {
        return centralToStore.replace("{storeId}", String.valueOf(storeId));
    }
    public String centralBroadcast() {
        return centralBroadcast;
    }
    public String storeToStore(final String fromStoreId, final String toStoreId) {
        return storeToStore
            .replace("{fromStoreId}", String.valueOf(fromStoreId))
            .replace("{toStoreId}", String.valueOf(toStoreId));
    }
    public String storeBroadcast(final String storeId) {
        return storeBroadcast.replace("{storeId}", String.valueOf(storeId));
    }
}
