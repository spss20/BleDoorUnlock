package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class LogDetails {
    @SerializedName("macAddress")
    private String macAddress;
    
    @SerializedName("userEmail")
    private String userEmail;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("activityType")
    private String activityType;

    public LogDetails() {
    }

    public LogDetails(String macAddress, String userEmail, double latitude, double longitude, String activityType) {
        this.macAddress = macAddress;
        this.userEmail = userEmail;
        this.latitude = latitude;
        this.longitude = longitude;
        this.activityType = activityType;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
}

