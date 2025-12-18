package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class ApprovalRequestData {
    @SerializedName("id")
    private String id;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("requestedAt")
    private String requestedAt;
    
    @SerializedName("user")
    private User user;
    
    @SerializedName("device")
    private Device device;

    public ApprovalRequestData() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}

