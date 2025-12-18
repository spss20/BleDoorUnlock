package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LogHistoryResponse {
    @SerializedName("logs")
    private List<LogHistoryItem> logs;
    
    @SerializedName("pagination")
    private Pagination pagination;

    public LogHistoryResponse() {
    }

    public List<LogHistoryItem> getLogs() {
        return logs;
    }

    public void setLogs(List<LogHistoryItem> logs) {
        this.logs = logs;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}

