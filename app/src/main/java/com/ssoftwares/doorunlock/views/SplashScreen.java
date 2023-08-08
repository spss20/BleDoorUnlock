package com.ssoftwares.doorunlock.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class SplashScreen extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_GPS = 10;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 20;
    private static final int REQUEST_ENABLE_BT = 35;
    private static final String TAG = "SplashScreen";

    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        sessionManager = new SessionManager(this);
        checkPermissions();

    }

    private void checkPermissions() {
        //Location check
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions = new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
            return;
        }

        if (!checkGPSIsOpen()) {
            Log.v(TAG, "GPS NOT OPEN, REQUESTING");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
            return;
        }

        //Check Bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        Log.v(TAG, "Every Permission is granted");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                if (sessionManager.getUserId() != null) {
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashScreen.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, 200);
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was enabled by the user, proceed with your Bluetooth-related tasks
                checkPermissions();
            } else {
                // User declined to enable Bluetooth or an error occurred, handle this situation
                Toast.makeText(this, "Decline by user", Toast.LENGTH_SHORT).show();
                Log.v(TAG, "Declined By user");
                finish();
            }
        }

        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            // Check if GPS is enabled again, to handle the user's action
            checkPermissions();
        } else {
            checkPermissions();
//            Toast.makeText(this, "Decline by user", Toast.LENGTH_SHORT).show();
//            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
            } else {
                Toast.makeText(this, "Denied by user", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}