package com.ssoftwares.doorunlock.views;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ncorti.slidetoact.SlideToActView;
import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.api.ApiInterface;
import com.ssoftwares.doorunlock.api.ApiService;
import com.ssoftwares.doorunlock.api.StrapiApiService;
import com.ssoftwares.doorunlock.ble.MyBleGattCallback;
import com.ssoftwares.doorunlock.models.LogData;
import com.ssoftwares.doorunlock.models.RequestModel;
import com.ssoftwares.doorunlock.utils.BleComActions;
import com.ssoftwares.doorunlock.utils.Commands;
import com.ssoftwares.doorunlock.utils.DateTimeUtils;
import com.ssoftwares.doorunlock.utils.SessionManager;

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

    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler = new Handler();

    private static final long SCAN_PERIOD = 5000; // Scan for 10 seconds
    private final int delay = 500;

    //Conn Variables
    private MyBleGattCallback gatt;
    private SessionManager sessionManager;
    private StrapiApiService strapiApiService;
    private ApiInterface apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceRecycler = findViewById(R.id.device_recycler);
        sessionManager = new SessionManager(this);
        scanButton = findViewById(R.id.scan);
        noViewLayout = findViewById(R.id.no_view_ly);
        slideToUnlock = findViewById(R.id.slide_to_unlock);
        deviceRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(this);
        deviceRecycler.setAdapter(adapter);

        apiService = ApiService.getLogApiService();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        strapiApiService = new StrapiApiService(this);
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanButton.setOnClickListener(view -> {
            startScan();
        });

        slideToUnlock.setOnSlideCompleteListener(slideToActView -> {
            openGate();
        });

        findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
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
                gatt.write("{P:12345678}");
//                uploadLogs("{L:BLELTELOCK,4537,Testing,07082023,162102,OPEN,ble_open}" +
////                        "{L:BLELTELOCK,4537,Testing,07082023,165039,CLOSE,ble_open}" +
////                        "{L:BLELTELOCK,4537,Testing,07082023,165103,CLOSE,ble_open}" +
////                        "{L:BLELTELOCK,4537,Testing,07082023,165105,CLOSE,ble_open}" +
////                        "{L:BLELTELOCK,4537,Testing,07082023,165107,CLOSE,ble_open}" +
////                        "{L:BLELTELOCK,4537,abhi,07082023,174303,CLOSE,ble_open}" +
//                        "{L:BLELTELOCK,4537,abhi,07082023,175215,CLOSE,ble_open}");
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
        BluetoothDevice device = adapter.getSelectedDevice();

        if (device == null) {
            Toast.makeText(this, "Please select any device first", Toast.LENGTH_SHORT).show();
            slideToUnlock.setCompleted(false, true);
            return;
        }

        gatt = new MyBleGattCallback(this, device, receiver);
    }

    private String lastCommand = null;
    private StringBuilder messageBuffer = new StringBuilder();
    private boolean isGateOpened = false;

    private BleComActions receiver = new BleComActions() {
        @Override
        public void onResponseReceived(String data) {
            Log.v(TAG, "Reply Received: " + data);
            switch (data) {
                case Commands.RES_PIN_OK:
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
                    isGateOpened = true;
                    sendLogCommand(3000);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Gate Opened Successfully", Toast.LENGTH_SHORT).show();
                        slideToUnlock.setCompleted(false, true);
                    });
                    break;
                case Commands.RES_LOG_OK:
                    String logs = messageBuffer.toString();
                    if (isGateOpened) {
                        sendLogCommand(3000);
                    } else {
                        handler.postDelayed(() -> gatt.write(Commands.COMMAND_GATE_OPEN), delay);
                    }
                    uploadLogs(logs);
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
        public void onDeviceDisconnected() {
            Log.v(TAG, "Device Disconnected");
        }
    };

    private void uploadLogs(String logs) {
        Log.v(TAG, "Log: " + logs);
        if(logs.isEmpty())
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
                String boardName = values[0].substring(2); // Removing "L:" from the beginning
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

                String isoDate = DateTimeUtils.combineDateTime(date, time);
                String gateStatusFi = null;

                if (gateStatus.equals("OPEN")) {
                    gateStatusFi = "opened";
                } else if (gateStatus.equals("CLOSE")) {
                    gateStatusFi = "closed";
                }

                LogData logData = new LogData(macAddress, userId, boardName, gateStatusFi, isoDate, openMethod);

                apiService.sendLogData(new RequestModel(logData)).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.v(TAG, "Api Success");
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.v(TAG, "Api Failed");
                    }
                });
            } else {
                // Handle incorrect format
                Log.e(TAG, "Invalid record format: " + record);
            }
            cursor = endIndex;
        }

    }

    @Override
    protected void onDestroy() {
        gatt.close();
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