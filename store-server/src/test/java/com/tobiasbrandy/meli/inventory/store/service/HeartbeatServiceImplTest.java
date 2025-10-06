package com.tobiasbrandy.meli.inventory.store.service;

import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import com.tobiasbrandy.meli.inventory.store.service.impl.HeartbeatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HeartbeatServiceImplTest {
    @Test
    void emitHeartbeat_setsTimestamp() {
        var appConfig = new AppConfig("store-1");
        var redis = mock(StringRedisTemplate.class);
        var valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        var svc = new HeartbeatServiceImpl(appConfig, redis);
        svc.emitHeartbeat();

        verify(valueOps).set(eq("store:store-1:heartbeat"), anyString());
    }

    @Test
    void scheduledHeartbeat_skipsWhenDisconnected() {
        var appConfig = new AppConfig("store-1");
        var redis = mock(StringRedisTemplate.class);
        var svc = new HeartbeatServiceImpl(appConfig, redis);
        svc.setDisconnected(true);
        svc.scheduledHeartbeat();
        verify(redis, never()).opsForValue();
    }
}
