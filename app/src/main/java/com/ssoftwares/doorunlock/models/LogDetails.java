package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class LogDetails {
    @SerializedName("deviceId")
    private String macAddress;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("remark")
    private String activityType;

    private String timestamp;

    public LogDetails() {
    }

    public LogDetails(String macAddress, double latitude, double longitude, String activityType, String timestamp) {
        this.macAddress = macAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.activityType = activityType;
        this.timestamp = timestamp;
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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

