package com.ssoftwares.doorunlock.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ssoftwares.doorunlock.R;
import com.ssoftwares.doorunlock.api.ApiInterface;
import com.ssoftwares.doorunlock.api.ApiService;
import com.ssoftwares.doorunlock.models.LogHistoryItem;
import com.ssoftwares.doorunlock.models.LogHistoryResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    private RecyclerView historyRecycler;
    private HistoryAdapter adapter;
    private ProgressBar loadingProgress;
    private TextView emptyText;
    private ApiInterface apiService;
    private List<LogHistoryItem> logList;
    private int currentPage = 1;
    private static final int PAGE_LIMIT = 20;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ApiService.initialize(this);
        apiService = ApiService.getApiService();

        historyRecycler = findViewById(R.id.history_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyText = findViewById(R.id.empty_text);
        ImageButton backButton = findViewById(R.id.back_button);

        logList = new ArrayList<>();
        adapter = new HistoryAdapter(this, logList);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        historyRecycler.setAdapter(adapter);

        backButton.setOnClickListener(view -> finish());

        loadHistory(currentPage);
    }

    private void loadHistory(int page) {
        if (isLoading) {
            return;
        }

        isLoading = true;
        loadingProgress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        apiService.getDoorOpenLogs(page, PAGE_LIMIT).enqueue(new Callback<LogHistoryResponse>() {
            @Override
            public void onResponse(Call<LogHistoryResponse> call, Response<LogHistoryResponse> response) {
                isLoading = false;
                loadingProgress.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    LogHistoryResponse historyResponse = response.body();
                    List<LogHistoryItem> newLogs = historyResponse.getLogs();

                    if (newLogs != null && !newLogs.isEmpty()) {
                        if (page == 1) {
                            logList.clear();
                        }
                        logList.addAll(newLogs);
                        adapter.notifyDataSetChanged();
                        emptyText.setVisibility(View.GONE);
                    } else {
                        if (logList.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    if (logList.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<LogHistoryResponse> call, Throwable t) {
                isLoading = false;
                loadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error loading history: " + t.getMessage());
                Toast.makeText(HistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (logList.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public static String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "N/A";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing timestamp: " + timestamp, e);
        }

        return timestamp;
    }

    public static String formatLocation(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            return String.format(Locale.getDefault(), "%.4f, %.4f", latitude, longitude);
        }
        return "N/A";
    }
}

