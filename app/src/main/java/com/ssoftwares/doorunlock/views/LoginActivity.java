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
import com.ssoftwares.doorunlock.api.StrapiApiService;
import com.ssoftwares.doorunlock.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private StrapiApiService strapiApiService;
    private EditText username;
    private EditText password;
    private Button login;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        strapiApiService = new StrapiApiService(this);
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

        strapiApiService.login(user, pass, new StrapiApiService.OnLoginResultListener() {
            @Override
            public void onLoginSuccess(String jwtToken) {
                sessionManager.saveUserId(user);
                Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onLoginFailure(String errorMessage) {
                Log.v("ApiError" , " " + errorMessage);
                Toast.makeText(LoginActivity.this, "Login Failed: Check user id or password", Toast.LENGTH_SHORT).show();
                login.setEnabled(true);
            }
        });
    }
}