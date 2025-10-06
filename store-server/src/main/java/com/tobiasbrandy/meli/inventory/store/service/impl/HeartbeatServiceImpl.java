package com.tobiasbrandy.meli.inventory.store.service.impl;

import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import com.tobiasbrandy.meli.inventory.store.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatServiceImpl implements HeartbeatService {
    private final AppConfig appConfig;
    private final StringRedisTemplate redis;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    @Scheduled(fixedRate = 30_000)
    public void scheduledHeartbeat() {
        if (disconnected.get()) return;
        emitHeartbeat();
    }

    public void emitHeartbeat() {
        val key = "store:" + appConfig.storeId() + ":heartbeat";
        redis.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected.set(disconnected);
    }
}
