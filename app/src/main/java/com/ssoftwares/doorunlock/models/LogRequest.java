package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class LogRequest {
    @SerializedName("activityType")
    private String activityType;
    
    @SerializedName("details")
    private LogDetails details;

    public LogRequest() {
    }

    public LogRequest(String activityType, LogDetails details) {
        this.activityType = activityType;
        this.details = details;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public LogDetails getDetails() {
        return details;
    }

    public void setDetails(LogDetails details) {
        this.details = details;
    }
}

