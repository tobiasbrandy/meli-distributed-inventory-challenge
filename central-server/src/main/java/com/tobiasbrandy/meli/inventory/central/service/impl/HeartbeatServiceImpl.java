package com.tobiasbrandy.meli.inventory.central.service.impl;

import com.tobiasbrandy.meli.inventory.central.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads store heartbeat timestamps from Redis to determine availability.
 * <p>
 * A store is considered alive if its last heartbeat is within
 * HEARTBEAT_TIMEOUT_MS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatServiceImpl implements HeartbeatService {
    private static final long HEARTBEAT_TIMEOUT_MS = 50_000;

    private final StringRedisTemplate redis;

    public boolean isAlive(final String storeId) {
        val key = "store:" + storeId + ":heartbeat";
        val heartbeat = redis.opsForValue().get(key);
        return heartbeat != null && (System.currentTimeMillis() - Long.parseLong(heartbeat)) <= HEARTBEAT_TIMEOUT_MS;
    }
}
