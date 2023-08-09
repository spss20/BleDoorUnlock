package com.ssoftwares.doorunlock.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ssoftwares.doorunlock.utils.BleComActions;
import com.ssoftwares.doorunlock.utils.Commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class MyBleGattCallback extends BluetoothGattCallback {

    private static final String TAG = "BleGattCallback";
    private BluetoothGatt bluetoothGatt;
    private Context mContext;

    public boolean connected = false;

    private final UUID BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final UUID BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final UUID BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private final int MAX_MTU = 512; // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
    private final int DEFAULT_MTU = 23;

    private int payloadSize = DEFAULT_MTU - 3;

    private BleComActions commandInterface;
    BluetoothGattService service;
    private BluetoothGattCharacteristic readCharacteristic, writeCharacteristic; // Characteristic for writing data
    private BluetoothDevice device;

    public MyBleGattCallback(Context context, BluetoothDevice device, BleComActions commandInterface) {
        this.mContext = context;
        this.commandInterface = commandInterface;
        if (this.device == device) {
            Log.v(TAG, "Device is same as before");
            if (!connected) {
                Log.v(TAG, "Initiating New Connection");
                bluetoothGatt = device.connectGatt(mContext, false, this);
            } else {
                Log.v(TAG, "Already connected");
            }
        } else {
            Log.v(TAG, "Device is new, so initiating new conn");
            if (connected) {
                close();
            }
            bluetoothGatt = device.connectGatt(mContext, false, this);
            this.device = device;
        }

    }

    public void connect() {
        if (!connected) {
            bluetoothGatt = device.connectGatt(mContext, false, this);
        } else {
            Log.v(TAG, "Already connected, just sending pin");
//            write(Commands.COMMAND_ENTER_PIN);
            sendPinCommand();
        }
    }

    public void close() {
        if (bluetoothGatt != null) {
            Log.d(TAG, "gatt.disconnect");
            bluetoothGatt.disconnect();
            Log.d(TAG, "gatt.close");
            try {
                bluetoothGatt.close();
            } catch (Exception ignored) {
            }
            bluetoothGatt = null;
            connected = false;
            Log.v(TAG, "GATT Closed");
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            Log.v(TAG, "Device Connected");
            bluetoothGatt.discoverServices();
            // Device is connected, you can now discover services, read/write characteristics, etc.
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // Device is disconnected, you can handle reconnection or other actions here
            Log.v(TAG, "Device Disconnected");
            commandInterface.onDeviceDisconnected();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onCharacteristicWrite: Write successful");
        } else {
            connected = false;
            Log.e(TAG, "onCharacteristicWrite: Write failed with status: " + status);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.v(TAG, "Service Discovered");

            service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            if (service != null) {
                readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                gatt.requestMtu(MAX_MTU);
            } else {
                // Service with the specified UUID not found
                Log.v(TAG, "Service with above UUID doesn't exist");
                connected = false;
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
                connected = true;
                commandInterface.onDeviceConnected();
//                write(Commands.COMMAND_ENTER_PIN);
//                sendPinCommand();
            } else {
                connected = false;
                Log.v(TAG, "Failed to set descriptor");
            }
        }
    }

    public void sendPinCommand(){
        write(Commands.COMMAND_ENTER_PIN);
    }
    public void sendTimeCommand() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyHHmmss");

        Calendar calendar = Calendar.getInstance();


        String date = dateFormat.format(calendar.getTime());

        Log.v(TAG , "Date " + date);
        Log.v(TAG , "Day " + calendar.get(Calendar.DAY_OF_WEEK));

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            dayOfWeek = 7;
        } else {
            dayOfWeek -= 1;
        }

        write(Commands.COMMAND_TIME + date + dayOfWeek +  "}");
    }

    public void write(String command) {
        if (!connected) {
            Log.v(TAG, "WriteErr, BLE not connected");
            return;
        }
        if (service == null) {
            Log.v(TAG, "Service not found");
            bluetoothGatt.discoverServices();
            return;
        }
        if (readCharacteristic == null || writeCharacteristic == null) {
            Log.v(TAG, "Read or Write characteristic is null");
            return;
        }
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
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
        // Handle the notification (response) here
        Log.v(TAG, "onCharacteristicChanged called Response: ");
        if (characteristic == readCharacteristic) { // NOPMD - test object identity
            byte[] data = readCharacteristic.getValue();
            Log.d(TAG, "read, len=" + data.length);
            commandInterface.onResponseReceived(new String(data));
        }
    }
    // Other callback methods to handle characteristics read/write, notifications, etc.
}
