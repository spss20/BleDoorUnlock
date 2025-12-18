package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class ApprovalResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("approvalRequest")
    private ApprovalRequestData approvalRequest;

    public ApprovalResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApprovalRequestData getApprovalRequest() {
        return approvalRequest;
    }

    public void setApprovalRequest(ApprovalRequestData approvalRequest) {
        this.approvalRequest = approvalRequest;
    }
}

