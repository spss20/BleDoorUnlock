package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class ApprovalRequest {
    @SerializedName("macAddress")
    private String macAddress;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;

    public ApprovalRequest() {
    }

    public ApprovalRequest(String macAddress, double latitude, double longitude) {
        this.macAddress = macAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

