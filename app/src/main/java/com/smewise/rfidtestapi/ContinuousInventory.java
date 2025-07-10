package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.ContinuousInventoryTime;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfidapi.RfidManager;

public class ContinuousInventory extends AppCompatActivity {

    private EditText etInventoryTime, etDelayTime;
    private TextView tvSettingResult, tvTagContent;
    private Button btnStart, btnStop, btnClear, btnGetInventoryTime;
    private RfidManager mRfidManager; // 假設這是你的RFID管理器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.continuous_inventory);
        getSupportActionBar().setTitle("ContinuousInventory");

        // 初始化UI
        etInventoryTime = findViewById(R.id.et_inventory_time);
        etDelayTime = findViewById(R.id.et_delay_time);
        tvSettingResult = findViewById(R.id.tv_setting_result);
        tvTagContent = findViewById(R.id.tv_tag_content);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnGetInventoryTime = findViewById(R.id.btn_get_inventory_time);
        btnClear = findViewById(R.id.btn_clear);
        // 初始化RFID管理器（假設已經實現）
        mRfidManager = RfidManager.InitInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_CONNECTED);
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        //registerReceiver(myDataReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(myDataReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(myDataReceiver, filter);
        }

        // 啟用 TextView 的滾動功能
        tvTagContent.setMovementMethod(new ScrollingMovementMethod());

        // 開始按鈕點擊事件
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInventory();
            }
        });

        // 結束按鈕點擊事件
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopInventory();
            }
        });

        //清理資料
        btnClear.setOnClickListener(v -> {
            tvTagContent.setText("   ");

        });

        // 獲取連續盤點時間按鈕點擊事件
        btnGetInventoryTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContinuousInventoryTime();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消註冊BroadcastReceiver
        unregisterReceiver(myDataReceiver);
    }

    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_CONNECTED)) {
                // 處理RFID服務連接事件
                String PackageName = intent.getStringExtra("PackageName");
                String ver = mRfidManager.GetServiceVersion();
                String api_ver = mRfidManager.GetAPIVersion();
                //Toast.makeText(InventoryActivity.this, "Intent_RFIDSERVICE_CONNECTED", Toast.LENGTH_SHORT).show();
            } else if (intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
                // 處理Tag數據事件
                String EPC = intent.getStringExtra(GeneralString.EXTRA_EPC);
                String TID = intent.getStringExtra(GeneralString.EXTRA_TID);
                String ReadData = intent.getStringExtra(GeneralString.EXTRA_ReadData);

                // 更新UI顯示Tag內容
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTagContent("EPC: " + EPC + "\nTID: " + TID + "\nReadData: " + ReadData);
                    }
                });
            }
        }
    };

    /**
     * 更新Tag內容顯示
     * @param tagContent 讀取的Tag內容
     */
    private void updateTagContent(String tagContent) {
        String currentText = tvTagContent.getText().toString();
        tvTagContent.setText(currentText + "\n" + tagContent);
    }

    /**
     * 啟動連續盤點
     */
    private void startInventory() {
        // 獲取輸入值
        String inventoryTimeStr = etInventoryTime.getText().toString();
        String delayTimeStr = etDelayTime.getText().toString();

        // 檢查輸入是否為空
        if (TextUtils.isEmpty(inventoryTimeStr)) {
            tvSettingResult.setText("Please enter inventory time");
            return;
        }
        if (TextUtils.isEmpty(delayTimeStr)) {
            tvSettingResult.setText("Please enter delay time");
            return;
        }

        // 檢查輸入範圍
        int inventoryTime = Integer.parseInt(inventoryTimeStr);
        int delayTime = Integer.parseInt(delayTimeStr);
        if (inventoryTime < 0 || inventoryTime > 1000 || delayTime < 0 || delayTime > 1000) {
            tvSettingResult.setText("Input range: 0-1000 ms");
            return;
        }

        // 設置連續盤點時間
        ContinuousInventoryTime time = new ContinuousInventoryTime();
        time.InventoryTime = inventoryTime;
        time.DelayTime = delayTime;
        int result = mRfidManager.SetContinuousInventoryTime(time);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            tvSettingResult.setText("Failed to set inventory time. Error: " + error);
            Log.e("InventoryActivity", "SetContinuousInventoryTime failed: " + error);
            return;
        }

        // 啟動掃描
        result = mRfidManager.SoftScanTrigger(true);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            tvSettingResult.setText("Failed to start scan. Error: " + error);
            Log.e("InventoryActivity", "SoftScanTrigger(true) failed: " + error);
        } else {
            tvSettingResult.setText("Scan started successfully");
        }
    }

    /**
     * 停止連續盤點
     */
    private void stopInventory() {
        // 停止掃描
        int result = mRfidManager.SoftScanTrigger(false);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            tvSettingResult.setText("Failed to stop scan. Error: " + error);
            Log.e("InventoryActivity", "SoftScanTrigger(false) failed: " + error);
        } else {
            tvSettingResult.setText("Scan stopped successfully");
        }
    }

    /**
     * 獲取連續盤點時間
     */
    private void getContinuousInventoryTime() {
        ContinuousInventoryTime time = new ContinuousInventoryTime();
        int result = mRfidManager.GetContinuousInventoryTime(time);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            tvSettingResult.setText("Failed to get inventory time. Error: " + error);
            Log.e("InventoryActivity", "GetContinuousInventoryTime failed: " + error);
        } else {
            String message = "InventoryTime: " + time.InventoryTime + " ms, DelayTime: " + time.DelayTime + " ms";
            tvSettingResult.setText(message);
            Log.i("InventoryActivity", "GetContinuousInventoryTime: " + message);
        }
    }
}