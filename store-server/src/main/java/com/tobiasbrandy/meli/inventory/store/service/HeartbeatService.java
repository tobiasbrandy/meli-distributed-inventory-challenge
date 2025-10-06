package com.tobiasbrandy.meli.inventory.store.service;

public interface HeartbeatService {
    void emitHeartbeat();

    void setDisconnected(boolean disconnected);
}
