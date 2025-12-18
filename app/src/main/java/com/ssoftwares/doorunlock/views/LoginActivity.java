package com.ssoftwares.doorunlock.views;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.api.ApiInterface;
import com.ssoftwares.doorunlock.api.ApiService;
import com.ssoftwares.doorunlock.models.LoginRequest;
import com.ssoftwares.doorunlock.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ApiInterface apiService;
    private EditText username;
    private EditText password;
    private Button login;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ApiService.initialize(this);
        apiService = ApiService.getApiService();
        sessionManager = new SessionManager(this);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login_btn);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });


    }

    private void loginUser() {
        String user = username.getText().toString();
        if(user.isEmpty()){
            username.setError("Cannot be empty");
            return;
        }

        String pass = password.getText().toString();
        if(pass.isEmpty()){
            password.setError("Password cannot be empty");
            return;
        }

        login.setEnabled(false);

        LoginRequest loginRequest = new LoginRequest(user, pass);
        apiService.login(loginRequest).enqueue(new Callback<com.ssoftwares.doorunlock.models.LoginResponse>() {
            @Override
            public void onResponse(Call<com.ssoftwares.doorunlock.models.LoginResponse> call, Response<com.ssoftwares.doorunlock.models.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.ssoftwares.doorunlock.models.LoginResponse loginResponse = response.body();
                    // Save token and user info
                    sessionManager.saveToken(loginResponse.getToken());
                    if (loginResponse.getUser() != null) {
                        sessionManager.saveUserId(loginResponse.getUser().getId());
                        sessionManager.saveUserEmail(loginResponse.getUser().getEmail());
                        if (loginResponse.getUser().getName() != null) {
                            sessionManager.saveUserName(loginResponse.getUser().getName());
                        }
                    }
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.v("ApiError", "Login failed: " + response.code());
                    Toast.makeText(LoginActivity.this, "Login Failed: Check email or password", Toast.LENGTH_SHORT).show();
                    login.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<com.ssoftwares.doorunlock.models.LoginResponse> call, Throwable t) {
                Log.v("ApiError", "Login error: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Login Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                login.setEnabled(true);
            }
        });
    }
}