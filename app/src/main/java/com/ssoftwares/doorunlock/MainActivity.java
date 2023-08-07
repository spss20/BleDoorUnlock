package com.ssoftwares.doorunlock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ncorti.slidetoact.SlideToActView;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private RecyclerView deviceRecycler;
    private DeviceAdapter adapter;
    private Button scanButton;
    private RelativeLayout noViewLayout;
    private SlideToActView slideToUnlock;

    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler = new Handler();

    private static final long SCAN_PERIOD = 2000; // Scan for 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceRecycler = findViewById(R.id.device_recycler);
        scanButton = findViewById(R.id.scan);
        noViewLayout = findViewById(R.id.no_view_ly);
        slideToUnlock = findViewById(R.id.slide_to_unlock);
        deviceRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this);
        deviceRecycler.setAdapter(adapter);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanButton.setOnClickListener(view -> {
            startScan();
        });

        slideToUnlock.setOnSlideCompleteListener(slideToActView -> {
            if (adapter != null) {
                adapter.openGate(slideToActView);
            }
        });
    }

    private void startScan() {
        scanButton.setEnabled(false);
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        noViewLayout.setVisibility(View.GONE);
        deviceRecycler.setVisibility(View.VISIBLE);
        adapter.reset();

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);

        // Stop scanning after SCAN_PERIOD milliseconds
        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            scanButton.setEnabled(true);
        }, SCAN_PERIOD);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Process scan result here
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            if (deviceName != null)
                adapter.addDevice(device);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(MainActivity.this, "Scan Failed", Toast.LENGTH_SHORT).show();
            // Handle scan failure here
            // You can check the errorCode to determine the reason for the failure
        }
    };

}