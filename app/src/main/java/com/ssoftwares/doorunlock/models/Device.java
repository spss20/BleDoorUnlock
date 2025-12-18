package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("id")
    private String id;
    
    @SerializedName("macAddress")
    private String macAddress;
    
    @SerializedName("status")
    private String status;

    public Device() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

