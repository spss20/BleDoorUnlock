package com.ssoftwares.doorunlock.api;

import com.ssoftwares.doorunlock.models.ApprovalRequest;
import com.ssoftwares.doorunlock.models.ApprovalResponse;
import com.ssoftwares.doorunlock.models.ApprovalStatusResponse;
import com.ssoftwares.doorunlock.models.LoginRequest;
import com.ssoftwares.doorunlock.models.LoginResponse;
import com.ssoftwares.doorunlock.models.LogRequest;
import com.ssoftwares.doorunlock.models.LogResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiInterface {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    
    @POST("api/logs/create")
    Call<LogResponse> createLog(@Body LogRequest logRequest);
    
    @POST("api/approvals/create")
    Call<ApprovalResponse> createApprovalRequest(@Body ApprovalRequest approvalRequest);
    
    @GET("api/approvals/request/{approvalId}")
    Call<ApprovalStatusResponse> getApprovalStatus(@Path("approvalId") String approvalId);
}
