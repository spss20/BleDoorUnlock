package com.ssoftwares.doorunlock;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class StrapiApiService {

    private static final String BASE_URL = "http://122.176.51.227:1337/api/";

    private Context mContext;

    public StrapiApiService(Context context){
        mContext = context;
    }

    public void login(String email, String password, final OnLoginResultListener listener) {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);

        String url = BASE_URL + "auth/local";
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("identifier", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        String jwtToken = response.getString("jwt");
                        // Handle successful login, jwtToken contains the JWT token
                        listener.onLoginSuccess(jwtToken);
                    } catch (JSONException e) {
                        // Handle JSON parsing error
                        listener.onLoginFailure("Error parsing login response");
                    }
                },
                error -> {
                    // Handle login failure (VolleyError)
                    listener.onLoginFailure("Login failed: " + error.getMessage());
                });

        requestQueue.add(request);
    }

    public interface OnLoginResultListener {
        void onLoginSuccess(String jwtToken);
        void onLoginFailure(String errorMessage);
    }
}
