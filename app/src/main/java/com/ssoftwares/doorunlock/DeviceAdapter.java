package com.ssoftwares.doorunlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ncorti.slidetoact.SlideToActView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private static final String TAG = "BleAdapter";
    private final Context context;
    private final List<BluetoothDevice> bleDeviceList = new ArrayList<>();
    private BluetoothGatt bluetoothGatt;
    private static final int MAX_MTU = 512; // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
    private static final int DEFAULT_MTU = 23;
    private int payloadSize = DEFAULT_MTU - 3;
    private int selectedDevicePosition = -1;
    private static final int VIEW_TYPE_SELECTED = 100;

    private static final UUID BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private android.os.Handler handler = new Handler();
    private BluetoothGattCharacteristic readCharacteristic, writeCharacteristic; // Characteristic for writing data

    public DeviceAdapter(Context context) {
        this.context = context;
    }

    public void addDevice(BluetoothDevice bleDevice) {
        if (bleDeviceList.contains(bleDevice)) {
            Log.v(TAG, "Device already exists");
        } else {
            bleDeviceList.add(bleDevice);
            Log.d("DEVICE", "Name: " + bleDevice.getName() + " \n" +
                    "Mac: " + bleDevice.getAddress() + " \n"
            );
            Log.d(TAG, "Size of list now " + bleDeviceList.size());
            notifyDataSetChanged();
        }
    }

    public void reset() {
        bleDeviceList.clear();
    }

//    public void removeDevice(BluetoothDevice bleDevice) {
//        for (int i = 0; i < bleDeviceList.size(); i++) {
//            BluetoothDevice device = bleDeviceList.get(i);
//            if (bleDevice.getAddress().equals(device.getAddress())) {
//                bleDeviceList.remove(i);
//            }
//        }
//    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SELECTED) {
            view = LayoutInflater.from(context).inflate(R.layout.device_item_selected, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        }
        return new MyViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == selectedDevicePosition) {
            return VIEW_TYPE_SELECTED;
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        BluetoothDevice device = bleDeviceList.get(position);
        if (device.getName() != null)
            holder.deviceName.setText(device.getName());
        else holder.deviceName.setText("- - - - - -");
        holder.deviceMac.setText(device.getAddress());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDevicePosition = holder.getAdapterPosition();
                notifyItemChanged(holder.getAdapterPosition());
//                Toast.makeText(context, "Ble Device  Clicked " + device.getName(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void openGate(SlideToActView slideToActView) {
        Log.v(TAG, "Opening THE GATE");
        if (selectedDevicePosition == -1) {
            Toast.makeText(context, "Please select any device first", Toast.LENGTH_SHORT).show();
            slideToActView.setCompleted(false, true);
            return;
        }
        BluetoothDevice device = bleDeviceList.get(selectedDevicePosition);

        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.v(TAG, "Device Connected");
                bluetoothGatt.discoverServices();
                // Device is connected, you can now discover services, read/write characteristics, etc.
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // Device is disconnected, you can handle reconnection or other actions here
                Log.v(TAG, "Device Disconnected");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicWrite: Write successful");
            } else {
                Log.e(TAG, "onCharacteristicWrite: Write failed with status: " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "Service Discovered");

                BluetoothGattService service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
                if (service != null) {
                    readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                    writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                    gatt.requestMtu(MAX_MTU);

                } else {
                    // Service with the specified UUID not found

                }
            } else {
                Log.v(TAG, "Service Failed to Discover");
                // Service discovery failed, handle the failure here
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "mtu size " + mtu + ", status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                payloadSize = mtu - 3;
                Log.d(TAG, "payload size " + payloadSize);
            }
            if (writeCharacteristic != null) {
                int writeProperties = writeCharacteristic.getProperties();
                if ((writeProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE +     // Microbit,HM10-clone have WRITE
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) { // HM10,TI uart,Telit have only WRITE_NO_RESPONSE
                    Log.v(TAG, "write characteristic not writable");
                    return;
                }

//                BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//                if (descriptor != null) {
//                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    bluetoothGatt.writeDescriptor(descriptor);
//                }
            }

            if (readCharacteristic != null) {
                if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
                    Log.v(TAG, "no notification for read characteristic");
                    return;
                }

                BluetoothGattDescriptor readDescriptor = readCharacteristic.getDescriptor(BLUETOOTH_LE_CCCD);
                if (readDescriptor == null) {
                    Log.v(TAG, "no CCCD descriptor for read characteristic");
                    return;
                }

                int readProperties = readCharacteristic.getProperties();
                if ((readProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    Log.d(TAG, "enable read indication");
                    readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                } else if ((readProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    Log.d(TAG, "enable read notification");
                    readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    Log.v(TAG, "no indication/notification for read characteristic (" + readProperties + ")");
                    return;
                }

                Log.d(TAG, "writing read characteristic descriptor");

                if (!gatt.writeDescriptor(readDescriptor)) {
                    Log.v(TAG, "read characteristic CCCD descriptor not writable");
                }
                // continues asynchronously in onDescriptorWrite()
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (descriptor.getCharacteristic() == readCharacteristic) {
                Log.d(TAG, "writing read characteristic descriptor finished, status=" + status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Now you can send the command to the characteristic
                    Log.v(TAG, "Descriptor Set Successful");
                    sendCommandToCharacteristic("{P:12345678}");
                } else {
                    Log.v(TAG, "Failed to set descriptor");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            // Handle the notification (response) here
            Log.v(TAG, "onCharacteristicChanged called Response: ");
            if (characteristic == readCharacteristic) { // NOPMD - test object identity
                byte[] data = readCharacteristic.getValue();
                Log.d(TAG, "read, len=" + data.length);
                onResponseReceived(new String(data));
            }
        }
        // Other callback methods to handle characteristics read/write, notifications, etc.
    };

    private void onResponseReceived(String data){
        Log.v(TAG, "Reply Received: " + data);
        switch (data) {
            case "{P:OK}":
                handler.postDelayed(()->sendCommandToCharacteristic("{I:355578432795322}"), 500 );
                break;
            case "{I:OK}":
                handler.postDelayed(()->sendCommandToCharacteristic("{G:1}"), 500 );
//                sendCommandToCharacteristic("{G:1}");
                break;
            case "{I:ERROR}":
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Your phone imei is not registered to use this app", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case "{G:OK}":
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Gate Opened Successfully", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void sendCommandToCharacteristic(String command) {
        byte[] commandBytes = command.getBytes();
        writeCharacteristic.setValue(commandBytes);
        if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
            Log.v(TAG, "WRITE FAILED");
        } else {
            Log.d(TAG, "write started, len=" + commandBytes.length);
        }
        Log.v(TAG, "Command Sent: " + command);
    }

    @Override
    public int getItemCount() {
        return bleDeviceList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceMac;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceMac = itemView.findViewById(R.id.device_mac);
        }
    }
}
