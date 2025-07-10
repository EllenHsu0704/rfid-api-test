package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDReaderType;
import com.cipherlab.rfidapi.RfidManager;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

//package com.example.rfidscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDReaderType;
import com.cipherlab.rfidapi.RfidManager;

import java.util.HashSet;
import java.util.Set;

public class ScanTest extends AppCompatActivity {

    private RfidManager mRfidManager;
    private TextView tvEpcList, tvTotalReads, tvUniqueTags, tvReadRate;
    private boolean isScanning = false;
    private int totalReads = 0;
    private Set<String> uniqueTags = new HashSet<>();
    private long scanStartTime = 0;

    private Handler triggerHandler = new Handler();
    private Runnable triggerRunnable;

    private boolean allowTriggerScan = false; // 控制是否允許偵測實體 Trigger

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_test);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("ScanTest");


        // 初始化 UI 控件
        tvEpcList = findViewById(R.id.tvEpcList);
        tvTotalReads = findViewById(R.id.tvTotalReads);
        tvUniqueTags = findViewById(R.id.tvUniqueTags);
        tvReadRate = findViewById(R.id.tvReadRate);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnClear = findViewById(R.id.btnClear);

        // 初始化 RFID 管理器
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "RFID 模組初始化失敗", Toast.LENGTH_SHORT).show();
            return;
        }

        // 註冊 RFID 廣播接收器，接收標籤數據
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        //registerReceiver(mMessageReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(mMessageReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mMessageReceiver, filter);
        }

        // 軟體按鈕觸發掃描
        btnStart.setOnClickListener(v -> {
            allowTriggerScan = true;  // 開始允許實體 trigger 掃描
            startScan();
        });

        // 停止掃描
        btnStop.setOnClickListener(v -> {
            allowTriggerScan = false; // 停止實體 trigger 掃描
            stopScan();
        });

        // 清除顯示的 EPC 清單
        btnClear.setOnClickListener(v -> {
            tvEpcList.setText("");
            totalReads = 0;
            uniqueTags.clear();
            updateStats();
        });

        // 啟動輪詢實體Trigger
        startTriggerPolling();
    }

    // 開始掃描標籤
    private void startScan() {
        if (!isScanning) {
            int result = mRfidManager.SoftScanTrigger(true);
            if (result == 0) {
                isScanning = true;
                scanStartTime = System.currentTimeMillis();
                totalReads = 0;
                uniqueTags.clear();
                updateStats();
            }
        }
    }

    // 停止掃描標籤
    private void stopScan() {
        if (isScanning) {
            int result = mRfidManager.SoftScanTrigger(false);
            if (result == 0) {
                isScanning = false;
                updateStats();
            }
        }
    }

    // 更新統計數據
    private void updateStats() {
        tvTotalReads.setText(String.valueOf(totalReads));
        tvUniqueTags.setText(String.valueOf(uniqueTags.size()));
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - scanStartTime) / 1000.0;
        if (elapsedSeconds > 0) {
            double rate = totalReads / elapsedSeconds;
            tvReadRate.setText(String.format("%.2f /s", rate));
        } else {
            tvReadRate.setText("0 /s");
        }
    }

    // 廣播接收器，處理接收到的 RFID 標籤數據
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GeneralString.Intent_RFIDSERVICE_TAG_DATA.equals(intent.getAction())) {
                int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
                if (response == GeneralString.RESPONSE_OPERATION_SUCCESS) {
                    String epc = intent.getStringExtra(GeneralString.EXTRA_EPC);
                    double data_rssi = intent.getDoubleExtra(GeneralString.EXTRA_DATA_RSSI, 0);
                    totalReads++;
                    if (epc != null) {
                        uniqueTags.add(epc);
                        tvEpcList.append("EPC: " + epc + " . " + data_rssi + "\n");
                        // 自動滾動顯示清單
                        ScrollView scrollView = findViewById(R.id.scrollView);
                        if (scrollView != null) {
                            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        }
                    }
                    updateStats();
                }
            }
        }
    };

    // 啟動輪詢實體Trigger狀態
    private void startTriggerPolling() {
        triggerRunnable = new Runnable() {
            boolean wasTriggerPressed = false;

            @Override
            public void run() {
                if (!allowTriggerScan) {
                    triggerHandler.postDelayed(this, 100);
                    return;
                }

                int status = mRfidManager.DeviceTriggerStatus();
                if (status == 1 && !wasTriggerPressed) {
                    startScan();  // 開始掃描
                    wasTriggerPressed = true;
                } else if (status == 0 && wasTriggerPressed) {
                    stopScan();  // 停止掃描
                    wasTriggerPressed = false;
                }
                triggerHandler.postDelayed(this, 100);
            }
        };
        triggerHandler.post(triggerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMessageReceiver);  // 取消註冊廣播接收器
        triggerHandler.removeCallbacks(triggerRunnable);  // 停止輪詢
    }
}



