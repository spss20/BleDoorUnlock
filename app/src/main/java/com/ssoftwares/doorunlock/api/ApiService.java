package com.ssoftwares.doorunlock.api;

import android.content.Context;

import com.ssoftwares.doorunlock.utils.SessionManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {
    private static final String BASE_URL = "http://122.180.241.64:5001/";

    private static Retrofit retrofit;
    private static Context applicationContext;

    public static void initialize(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static ApiInterface getApiService() {
        if (retrofit == null) {
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            
            // Add logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClientBuilder.addInterceptor(loggingInterceptor);
            
            // Add authentication interceptor
            httpClientBuilder.addInterceptor(new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();
                    
                    // Add Authorization header if token exists
                    if (applicationContext != null) {
                        SessionManager sessionManager = new SessionManager(applicationContext);
                        String token = sessionManager.getToken();
                        if (token != null && !token.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }
                    }
                    
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClientBuilder.build())
                    .build();
        }
        return retrofit.create(ApiInterface.class);
    }
}
