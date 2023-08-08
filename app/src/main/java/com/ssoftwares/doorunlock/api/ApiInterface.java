package com.ssoftwares.doorunlock.api;

import com.ssoftwares.doorunlock.models.RequestModel;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiInterface {
    @POST("api/logs")
    Call<Void> sendLogData(@Body RequestModel logData);
}
