package com.ssoftwares.doorunlock.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.models.LogHistoryItem;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<LogHistoryItem> logList;
    private HistoryActivity activity;

    public HistoryAdapter(HistoryActivity activity, List<LogHistoryItem> logList) {
        this.activity = activity;
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogHistoryItem logItem = logList.get(position);

        // Device MAC
        if (logItem.getDevice() != null && logItem.getDevice().getMacAddress() != null) {
            holder.deviceMac.setText(logItem.getDevice().getMacAddress());
        } else {
            holder.deviceMac.setText("N/A");
        }

        // Timestamp
        holder.timestamp.setText(HistoryActivity.formatTimestamp(logItem.getTimestamp()));

        // User name
        if (logItem.getUser() != null && logItem.getUser().getName() != null) {
            holder.userName.setText(logItem.getUser().getName());
        } else {
            holder.userName.setText("N/A");
        }

        // Remark
        if (logItem.getRemark() != null && !logItem.getRemark().isEmpty()) {
            holder.remark.setText(logItem.getRemark());
            holder.remark.setVisibility(View.VISIBLE);
        } else {
            holder.remark.setVisibility(View.GONE);
        }

        // Location
        View locationContainer = holder.itemView.findViewById(R.id.location_container);
        if (logItem.getLatitude() != null && logItem.getLongitude() != null) {
            holder.location.setText(HistoryActivity.formatLocation(logItem.getLatitude(), logItem.getLongitude()));
            if (locationContainer != null) {
                locationContainer.setVisibility(View.VISIBLE);
            }
        } else {
            if (locationContainer != null) {
                locationContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceMac;
        TextView timestamp;
        TextView userName;
        TextView remark;
        TextView location;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceMac = itemView.findViewById(R.id.device_mac);
            timestamp = itemView.findViewById(R.id.timestamp);
            userName = itemView.findViewById(R.id.user_name);
            remark = itemView.findViewById(R.id.remark);
            location = itemView.findViewById(R.id.location);
        }
    }
}

