package com.ssoftwares.doorunlock.utils;

public interface BleComActions {
    void onResponseReceived(String data);
    void onDeviceConnected();
    void onDeviceDisconnected();
}
