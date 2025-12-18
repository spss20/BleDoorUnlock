package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class LogResponse {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("activityType")
    private String activityType;
    
    @SerializedName("details")
    private String details;
    
    @SerializedName("timestamp")
    private String timestamp;

    public LogResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

