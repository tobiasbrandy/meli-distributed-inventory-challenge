package com.tobiasbrandy.meli.inventory.central.service;

import com.tobiasbrandy.meli.inventory.central.service.impl.HeartbeatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HeartbeatServiceImplTest {

    @Test
    void isAlive_trueWhenRecentTimestamp() {
        var redis = mock(StringRedisTemplate.class);
        var valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(eq("store:store-1:heartbeat"))).thenReturn(String.valueOf(System.currentTimeMillis()));

        var svc = new HeartbeatServiceImpl(redis);
        assertTrue(svc.isAlive("store-1"));
    }

    @Test
    void isAlive_falseWhenMissingOrStale() {
        var redis = mock(StringRedisTemplate.class);
        var valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(eq("store:store-1:heartbeat"))).thenReturn(null);

        var svc = new HeartbeatServiceImpl(redis);
        assertFalse(svc.isAlive("store-1"));
    }
}
