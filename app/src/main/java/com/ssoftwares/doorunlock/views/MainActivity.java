package com.ssoftwares.doorunlock.views;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ncorti.slidetoact.SlideToActView;
import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.api.ApiInterface;
import com.ssoftwares.doorunlock.api.ApiService;
import com.ssoftwares.doorunlock.ble.MyBleGattCallback;
import com.google.gson.Gson;
import com.ssoftwares.doorunlock.models.ApprovalRequest;
import com.ssoftwares.doorunlock.models.ApprovalResponse;
import com.ssoftwares.doorunlock.models.ApprovalStatusResponse;
import com.ssoftwares.doorunlock.models.ErrorResponse;
import com.ssoftwares.doorunlock.models.LogData;
import com.ssoftwares.doorunlock.models.LogDetails;
import com.ssoftwares.doorunlock.models.LogRequest;
import com.ssoftwares.doorunlock.models.LogResponse;

import java.io.IOException;
import com.ssoftwares.doorunlock.utils.BleComActions;
import com.ssoftwares.doorunlock.utils.Commands;
import com.ssoftwares.doorunlock.utils.DateTimeUtils;
import com.ssoftwares.doorunlock.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private RecyclerView deviceRecycler;
    private DeviceAdapter adapter;
    private Button scanButton;
    private RelativeLayout noViewLayout;
    private SlideToActView slideToUnlock;
    private Button requestApprovalBtn;
    private LinearLayout approvalStatusContainer;
    private TextView approvalStatusText;
    private TextView approvalStatusMessage;

    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler = new Handler();

    private static final long SCAN_PERIOD = 5000; // Scan for 10 seconds
    private final int delay = 500;

    //Conn Variables
    private MyBleGattCallback gatt;
    private SessionManager sessionManager;
    private ApiInterface apiService;

    private List<LogData> pendingLogList;

    private String lastCommand = null;
    private StringBuilder messageBuffer = new StringBuilder();
    //    private boolean isGateOpened = false;
    private boolean isTransaction = false;
    
    // Location variables
    private LocationManager locationManager;
    private Location currentLocation;
    private LocationListener locationListener;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    
    // Approval flow variables
    private String currentApprovalRequestId = null;
    private boolean isApprovalApproved = false;
    private Handler pollingHandler = new Handler();
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 3000; // Poll every 3 seconds
    private static final long APPROVED_MESSAGE_DISPLAY_TIME = 3000; // Show "Approved" message for 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceRecycler = findViewById(R.id.device_recycler);
        sessionManager = new SessionManager(this);
        scanButton = findViewById(R.id.scan);
        noViewLayout = findViewById(R.id.no_view_ly);
        slideToUnlock = findViewById(R.id.slide_to_unlock);
        requestApprovalBtn = findViewById(R.id.request_approval_btn);
        approvalStatusContainer = findViewById(R.id.approval_status_container);
        approvalStatusText = findViewById(R.id.approval_status_text);
        approvalStatusMessage = findViewById(R.id.approval_status_message);
        
        deviceRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this);
        deviceRecycler.setAdapter(adapter);
        
        // Set up device selection listener
        adapter.setOnDeviceSelectedListener(new DeviceAdapter.OnDeviceSelectedListener() {
            @Override
            public void onDeviceSelected(BluetoothDevice device) {
                if (device != null) {
                    // Reset approval state when new device is selected - user must go through approval again
                    Log.v(TAG, "Device selected: " + device.getAddress());
                    resetApprovalState();
                    // Force show the request approval button after reset
                    runOnUiThread(() -> {
                        showRequestApprovalButton();
                        Log.v(TAG, "Request approval button visibility: " + (requestApprovalBtn.getVisibility() == View.VISIBLE));
                    });
                } else {
                    hideAllApprovalUI();
                }
            }
        });

        pendingLogList = new ArrayList<>();
        ApiService.initialize(this);
        apiService = ApiService.getApiService();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        
        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocationUpdates();

        scanButton.setOnClickListener(view -> {
            if (gatt != null)
                gatt.close();
            resetApprovalState();
            startScan();
        });

        slideToUnlock.setOnSlideCompleteListener(slideToActView -> {
            if (isApprovalApproved) {
                openGate();
            } else {
                Toast.makeText(this, "Please wait for approval", Toast.LENGTH_SHORT).show();
                slideToActView.setCompleted(false, true);
            }
        });
        
        // Request approval button click listener
        requestApprovalBtn.setOnClickListener(view -> {
            BluetoothDevice selectedDevice = adapter.getSelectedDevice();
            if (selectedDevice == null) {
                Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show();
                return;
            }
            requestApproval(selectedDevice);
        });

        findViewById(R.id.connect).setOnClickListener(view -> {
            Log.v(TAG, "Connecting device");
//                BluetoothDevice device = adapter.getSelectedDevice();
            byte[] deviceMac = new byte[]{0x00, 0x0B, 0x57, 0x5A, (byte) 0xD7, (byte) 0xC6};
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMac);

            if (device == null) {
                Toast.makeText(MainActivity.this, "Device doesn't exist", Toast.LENGTH_SHORT).show();
                slideToUnlock.setCompleted(false, true);
                return;
            }

            gatt = new MyBleGattCallback(MainActivity.this, device, receiver);
        });

        findViewById(R.id.disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] deviceMac = new byte[]{0x00, 0x0B, 0x57, 0x5A, (byte) 0xD7, (byte) 0xC6};
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMac);

                gatt = new MyBleGattCallback(MainActivity.this, device, receiver);

                gatt.close();
            }
        });

        findViewById(R.id.send_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLogCommand(500);
            }
        });

        findViewById(R.id.gate_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                gatt.write("{P:12345678}");
                parseLogs("{L:BLELTELOCK,4537,Testing,09102023,142000,OPEN,ble_open}" +
//                        "{L:BLELTELOCK,4537,Testing,07082023,165039,CLOSE,ble_open}" +
//                        "{L:BLELTELOCK,4537,surya,07082023,165103,CLOSE,ble_open}" +
//                        "{L:BLELTELOCK,4537,Testing,07082023,165105,CLOSE,ble_open}" +
//                        "{L:BLELTELOCK,4537,Testing,07082023,165107,CLOSE,ble_open}" +
//                        "{L:BLELTELOCK,4537,abhi,07082023,174303,CLOSE,ble_open}" +
                        "{L:BLELTELOCK,4537,abhi,10082023,175215,CLOSE,ble_open}");
            }
        });

    }

    private void parseLogs(String logs) {
        Log.v(TAG, "Log: " + logs);
        if (logs.isEmpty())
            return;
        int cursor = 0;

        while (true) {
            int startIndex = logs.indexOf("{", cursor);
            if (startIndex == -1) {
                break;
            }
            int endIndex = logs.indexOf("}", startIndex) + 1;

            String record = logs.substring(startIndex, endIndex);
            Log.v(TAG, "Record: " + record);

            String[] values = record.split(",");
            if (values.length >= 7) {
                String boardName = values[0].substring(3); // Removing "L:" from the beginning
                String macAddress = values[1];
                String userId = values[2];
                String date = values[3];
                String time = values[4];
                String gateStatus = values[5];
                String openMethodTemp = values[6];
                String openMethod = openMethodTemp.substring(0, openMethodTemp.length() - 1);

                // Do whatever you want with the parsed data
                Log.d(TAG, "Board Name: " + boardName);
                Log.d(TAG, "MAC Address: " + macAddress);
                Log.d(TAG, "User ID: " + userId);
                Log.d(TAG, "Date: " + date);
                Log.d(TAG, "Time: " + time);
                Log.d(TAG, "Gate Status: " + gateStatus);
                Log.d(TAG, "Open Method: " + openMethod);

                String isoDate = DateTimeUtils.combineDateTime(date + time);
                Log.d(TAG, "ISODate: " + isoDate);
                String gateStatusFi = null;

                if (gateStatus.equals("OPEN")) {
                    gateStatusFi = "opened";
                } else if (gateStatus.equals("CLOSE")) {
                    gateStatusFi = "closed";
                }

                LogData logData = new LogData(macAddress, userId, boardName, gateStatusFi, isoDate, openMethod);
                pendingLogList.add(logData);
            } else {
                // Handle incorrect format
                Log.e(TAG, "Invalid record format: " + record);
            }
            cursor = endIndex;
        }

        uploadLogs();
    }

    private void uploadLogs() {
        if (pendingLogList.isEmpty()) {
            return;
        }
        
        LogData logData = pendingLogList.get(0);
        
        // Combine board, gate_status, and open_method into activityType message
        // Format: "G19 Gate Opened by BLE method"
        String gateStatusText = logData.getGateStatus();
        if (gateStatusText != null) {
            gateStatusText = gateStatusText.substring(0, 1).toUpperCase() + gateStatusText.substring(1);
        }
        String openMethodText = logData.getOpenMethod();
        if (openMethodText != null && !openMethodText.isEmpty()) {
            openMethodText = openMethodText.toUpperCase();
        } else {
            openMethodText = "BLE";
        }
        String activityType = logData.getBoard() + " Gate " + gateStatusText + " by " + openMethodText + " method";
        
        // Get location (use default if not available)
        double latitude = currentLocation != null ? currentLocation.getLatitude() : 0.0;
        double longitude = currentLocation != null ? currentLocation.getLongitude() : 0.0;
        
        // Get user email from session
        String userEmail = sessionManager.getUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = sessionManager.getUserId(); // Fallback to userId if email not available
        }
        
        // Create log details
        LogDetails logDetails = new LogDetails(
            logData.getMac(),
            userEmail,
            latitude,
            longitude,
            activityType
        );
        
        // Create log request
        LogRequest logRequest = new LogRequest("idle", logDetails);
        
        apiService.createLog(logRequest).enqueue(new Callback<LogResponse>() {
            @Override
            public void onResponse(Call<LogResponse> call, Response<LogResponse> response) {
                pendingLogList.remove(0);
                int size = pendingLogList.size();
                Log.v(TAG, "Success, Api Size Left: " + size);
                if (size != 0) {
                    uploadLogs();
                }
            }

            @Override
            public void onFailure(Call<LogResponse> call, Throwable t) {
                Log.v(TAG, "Failed: " + t.getLocalizedMessage());
                pendingLogList.remove(0);
                int size = pendingLogList.size();
                if (size != 0) {
                    uploadLogs();
                }
            }
        });
    }

    public void sendLogCommand(int delay) {
        handler.postDelayed(() -> {
            messageBuffer = new StringBuilder();
            gatt.write(Commands.COMMAND_LOG);
            lastCommand = Commands.COMMAND_LOG;
        }, delay);
    }

    public void openGate() {
        Log.v(TAG, "Opening THE GATE");
        
        // Check if approval is granted
        if (!isApprovalApproved) {
            Toast.makeText(this, "Please wait for approval before unlocking", Toast.LENGTH_SHORT).show();
            slideToUnlock.setCompleted(false, true);
            return;
        }
        
        BluetoothDevice device = adapter.getSelectedDevice();

        if (device == null) {
            Toast.makeText(this, "Please select any device first", Toast.LENGTH_SHORT).show();
            slideToUnlock.setCompleted(false, true);
            return;
        }

        isTransaction = true;
        if (gatt == null) {
            gatt = new MyBleGattCallback(this, device, receiver);
        } else {
            if (!gatt.connected) {
                gatt.connect();
            }
        }
    }

    private BleComActions receiver = new BleComActions() {
        @Override
        public void onResponseReceived(String data) {
            Log.v(TAG, "Reply Received: " + data);
            switch (data) {
                case Commands.RES_PIN_OK:
                    handler.postDelayed(() -> gatt.sendTimeCommand(), delay);
                    break;
                case Commands.RES_TIME_OK:
                    handler.postDelayed(() -> gatt.write("{I:" + sessionManager.getUserId() + "}"), delay);
                    break;
                case Commands.RES_IMEI_OK:
                    Log.v(TAG, "Authenticated");
//                    handler.postDelayed(() -> gatt.write(Commands.COMMAND_GATE_OPEN), delay);
                    lastCommand = Commands.COMMAND_LOG;
                    handler.postDelayed(() -> gatt.write(Commands.COMMAND_LOG), delay);
                    break;
                case Commands.RES_IMEI_ERROR:
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Your phone imei is not registered to use this app", Toast.LENGTH_SHORT).show());
                    break;
                case Commands.RES_GATE_OK:
                    isTransaction = false;
                    sendLogCommand(3000);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Gate Opened Successfully", Toast.LENGTH_SHORT).show();
                        slideToUnlock.setCompleted(false, true);
                    });
                    break;
                case Commands.RES_LOG_OK:
                    String logs = messageBuffer.toString();
                    if (isTransaction) {
                        handler.postDelayed(() -> gatt.write(Commands.COMMAND_GATE_OPEN), delay);
                    } else {
                        sendLogCommand(3000);
                    }
                    parseLogs(logs);
                    break;
                default:
                    if (lastCommand != null && messageBuffer != null) {
                        if (lastCommand.equals(Commands.COMMAND_LOG)) {
                            messageBuffer.append(data);
                        }
                    }
            }
        }

        @Override
        public void onDeviceConnected() {
            if (isTransaction) {
//                gatt.write(Commands.COMMAND_ENTER_PIN);

                gatt.sendPinCommand();
            }
        }

        @Override
        public void onDeviceDisconnected() {
            Log.v(TAG, "Device Disconnected");
            if (isTransaction) {
                gatt.connect();
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (locationManager == null) {
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Try to get last known location first
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (currentLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        
        // Request location updates
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
            }
        };
        
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            }
        }
    }

    // Approval flow methods
    private void requestApproval(BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get location
        double latitude = currentLocation != null ? currentLocation.getLatitude() : 0.0;
        double longitude = currentLocation != null ? currentLocation.getLongitude() : 0.0;
        
        // Create approval request
        ApprovalRequest approvalRequest = new ApprovalRequest(
            device.getAddress(),
            latitude,
            longitude
        );
        
        requestApprovalBtn.setEnabled(false);
        showApprovalStatus("Requesting Approval...", "Please wait");
        
        apiService.createApprovalRequest(approvalRequest).enqueue(new Callback<ApprovalResponse>() {
            @Override
            public void onResponse(Call<ApprovalResponse> call, Response<ApprovalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApprovalResponse approvalResponse = response.body();
                    if (approvalResponse.isSuccess() && approvalResponse.getApprovalRequest() != null) {
                        currentApprovalRequestId = approvalResponse.getApprovalRequest().getId();
                        Log.v(TAG, "Approval request created: " + currentApprovalRequestId);
                        startPollingApprovalStatus();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to create approval request", Toast.LENGTH_SHORT).show();
                        requestApprovalBtn.setEnabled(true);
                        hideApprovalStatus();
                    }
                } else {
                    // Handle error response
                    String errorMessage = "Failed to create approval request";
                    if (response.errorBody() != null) {
                        try {
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getError() != null) {
                                errorMessage = errorResponse.getError();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }
                    }
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    requestApprovalBtn.setEnabled(true);
                    hideApprovalStatus();
                }
            }
            
            @Override
            public void onFailure(Call<ApprovalResponse> call, Throwable t) {
                Log.e(TAG, "Error creating approval request: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                requestApprovalBtn.setEnabled(true);
                hideApprovalStatus();
            }
        });
    }
    
    private void startPollingApprovalStatus() {
        if (currentApprovalRequestId == null) {
            return;
        }
        
        showApprovalStatus("Approval Under Process", "Waiting for admin approval...");
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentApprovalRequestId != null) {
                    checkApprovalStatus();
                    if (!isApprovalApproved && currentApprovalRequestId != null) {
                        pollingHandler.postDelayed(this, POLLING_INTERVAL);
                    }
                }
            }
        };
        
        pollingHandler.post(pollingRunnable);
    }
    
    private void checkApprovalStatus() {
        if (currentApprovalRequestId == null) {
            return;
        }
        
        apiService.getApprovalStatus(currentApprovalRequestId).enqueue(new Callback<ApprovalStatusResponse>() {
            @Override
            public void onResponse(Call<ApprovalStatusResponse> call, Response<ApprovalStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApprovalStatusResponse statusResponse = response.body();
                    String status = statusResponse.getStatus();
                    
                    if ("APPROVED".equals(status)) {
                        handleApprovalApproved();
                    } else if ("REJECTED".equals(status)) {
                        handleApprovalRejected();
                    } else if ("PENDING".equals(status)) {
                        // Continue polling
                        Log.v(TAG, "Approval still pending...");
                    }
                } else {
                    Log.e(TAG, "Failed to get approval status: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApprovalStatusResponse> call, Throwable t) {
                Log.e(TAG, "Error checking approval status: " + t.getMessage());
            }
        });
    }
    
    private void handleApprovalApproved() {
        isApprovalApproved = true;
        stopPolling();
        
        showApprovalStatus("Request Approved", "You can now unlock the door");
        requestApprovalBtn.setVisibility(View.GONE);
        
        // Show slide to unlock while keeping approval status visible
        slideToUnlock.setVisibility(View.VISIBLE);
    }
    
    private void handleApprovalRejected() {
        isApprovalApproved = false;
        stopPolling();
        currentApprovalRequestId = null;
        
        showApprovalStatus("Request Rejected", "You cannot proceed with this device");
        requestApprovalBtn.setVisibility(View.GONE);
        slideToUnlock.setVisibility(View.GONE);
        
        // Reset after showing rejection message
        handler.postDelayed(() -> {
            resetApprovalState();
            Toast.makeText(MainActivity.this, "Approval request was rejected", Toast.LENGTH_LONG).show();
        }, 5000);
    }
    
    private void stopPolling() {
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }
    
    private void resetApprovalState() {
        isApprovalApproved = false;
        currentApprovalRequestId = null;
        stopPolling();
        // Reset all approval-related UI and state
        hideAllApprovalUI();
        // Make sure slide to unlock is hidden and reset when resetting
        slideToUnlock.setVisibility(View.GONE);
        slideToUnlock.setCompleted(false, true);
    }
    
    private void showRequestApprovalButton() {
        requestApprovalBtn.setVisibility(View.VISIBLE);
        requestApprovalBtn.setEnabled(true);
        approvalStatusContainer.setVisibility(View.GONE);
        slideToUnlock.setVisibility(View.GONE);
    }
    
    private void showApprovalStatus(String status, String message) {
        approvalStatusText.setText(status);
        approvalStatusMessage.setText(message);
        approvalStatusContainer.setVisibility(View.VISIBLE);
        requestApprovalBtn.setVisibility(View.GONE);
        // Don't hide slideToUnlock here - it will be shown separately when approved
        if (!"Request Approved".equals(status)) {
            slideToUnlock.setVisibility(View.GONE);
        }
    }
    
    private void hideApprovalStatus() {
        approvalStatusContainer.setVisibility(View.GONE);
    }
    
    private void hideAllApprovalUI() {
        requestApprovalBtn.setVisibility(View.GONE);
        requestApprovalBtn.setEnabled(true); // Re-enable button when hiding
        approvalStatusContainer.setVisibility(View.GONE);
        slideToUnlock.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        if (gatt != null)
            gatt.close();
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.e(TAG, "Error removing location updates", e);
            }
        }
        super.onDestroy();
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
            if (adapter.getBleList().isEmpty()) {
                Toast.makeText(this, "No Device Found", Toast.LENGTH_SHORT).show();
                noViewLayout.setVisibility(View.VISIBLE);
                deviceRecycler.setVisibility(View.GONE);
            }
        }, SCAN_PERIOD);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Process scan result here
            Log.v(TAG, "ON Scan Result");
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            if (deviceName != null && deviceName.length() > 3) {
                Log.v(TAG, "Device " + deviceName);
//                String sub = deviceName.substring(0, 3);
//                boolean isValid = sub.equals("LSG") || sub.equals("LvS");
//                if (isValid)
                    adapter.addDevice(device);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.v(TAG, "ON Batch Result");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(MainActivity.this, "Scan Failed", Toast.LENGTH_SHORT).show();
            // Handle scan failure here
            // You can check the errorCode to determine the reason for the failure
        }
    };

}