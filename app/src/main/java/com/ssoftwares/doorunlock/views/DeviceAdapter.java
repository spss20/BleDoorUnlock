package com.ssoftwares.doorunlock.views;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ssoftwares.doorunlock.R;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {

    private static final String TAG = "BleAdapter";
    private final Context context;
    private final List<BluetoothDevice> bleDeviceList = new ArrayList<>();

    private int selectedDevicePosition = -1;
    private static final int VIEW_TYPE_SELECTED = 100;
    private static final int VIEW_TYPE_NORMAL = 200;

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

    public BluetoothDevice getSelectedDevice(){
        if(selectedDevicePosition == -1){
            return null;
        } else return bleDeviceList.get(selectedDevicePosition);
    }
    public void reset() {
        bleDeviceList.clear();
        selectedDevicePosition = -1;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_NORMAL) {
            view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.device_item_selected, parent, false);
        }
        return new MyViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == selectedDevicePosition) {
            return VIEW_TYPE_SELECTED;
        } else return VIEW_TYPE_NORMAL;
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
