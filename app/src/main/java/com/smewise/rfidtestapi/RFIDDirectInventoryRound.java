package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Bundle;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.InventoryType;
import com.cipherlab.rfidapi.RfidManager;

public class RFIDDirectInventoryRound extends AppCompatActivity {

    private EditText etCount;
    private TextView tvResult;
    private Button btnStart, btnStop;
    private RfidManager mRfidManager; // 假設這是你的RFID管理器

    private int readCount = 0; // 記錄讀取數量
    private StringBuilder tagDataHistory = new StringBuilder(); // 記錄所有讀取資訊



    // 定義 BroadcastReceiver
    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
                // 處理 Tag 數據
                String epc = intent.getStringExtra(GeneralString.EXTRA_EPC);
                String tid = intent.getStringExtra(GeneralString.EXTRA_TID);
                double rssi = intent.getDoubleExtra(GeneralString.EXTRA_DATA_RSSI, 0);

                // 檢查資料是否為空
                if (epc != null && tid != null) { // 確保 EPC 和 TID 不為空

                // 更新讀取數量
                readCount++;

                // 將資料添加到歷史記錄
                tagDataHistory.append("已讀取數量: ").append(readCount).append("\n")
                        .append("EPC: ").append(epc).append("\n")
                        .append("TID: ").append(tid).append("\n")
                        .append("RSSI: ").append(rssi).append("\n\n");

                // 更新 UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvResult.setText(tagDataHistory.toString());
                    }
                });
            }
        }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rfiddirect_inventory_round);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("RFIDDirectInventoryRound");


        // 初始化UI
        etCount = findViewById(R.id.et_count);
        tvResult = findViewById(R.id.tv_result);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        // 啟用 TextView 的滾動功能
        tvResult.setMovementMethod(new ScrollingMovementMethod());

        // 初始化RFID管理器（假設已經實現）
        mRfidManager = RfidManager.InitInstance(this);

        // 註冊 BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        //registerReceiver(myDataReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(myDataReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(myDataReceiver, filter);
        }

        // 開始按鈕點擊事件
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInventory();
            }
        });

        // 停止按鈕點擊事件
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopInventory();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消註冊 BroadcastReceiver
        unregisterReceiver(myDataReceiver);
    }

    /**
     * 開始盤點
     */
    private void startInventory() {

        // 重置讀取數量和歷史記錄
        readCount = 0;
        tagDataHistory.setLength(0);
        tvResult.setText("");

        // 獲取輸入的盤點次數
        String countStr = etCount.getText().toString();

        // 檢查輸入是否為空
        if (TextUtils.isEmpty(countStr)) {
            tvResult.setText("請輸入盤點次數");
            return;
        }

        // 轉換為整數
        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            tvResult.setText("請輸入有效的數字");
            return;
        }

        // 檢查輸入範圍
        if (count < 1 || count > 254) {
            tvResult.setText("盤點次數範圍：1~254");
            return;
        }

        // 執行盤點操作
        int re = mRfidManager.RFIDDirectStartInventoryRound(InventoryType.EPC_AND_TID, count);
        if (re != ClResult.S_OK.ordinal()) {
            String err = mRfidManager.GetLastError();
            tvResult.setText("盤點失敗: " + err);
            Log.e("RFIDInventory", "RFIDDirectStartInventoryRound failed: " + err);
        } else {
            tvResult.setText("盤點開始，等待資料...");
        }
    }

    /**
     * 停止盤點
     */
    private void stopInventory() {
        // 執行停止盤點操作
        int re = mRfidManager.RFIDDirectCancelInventoryRound();
        if (re != ClResult.S_OK.ordinal()) {
            String err = mRfidManager.GetLastError();
            tvResult.setText("停止盤點失敗: " + err);
            Log.e("RFIDInventory", "RFIDDirectCancelInventoryRound failed: " + err);
        } else {
            tvResult.setText("盤點已停止");
        }
    }
}