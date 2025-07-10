package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.DeviceResponse;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDMemoryBank;
import com.cipherlab.rfidapi.RfidManager;

public class LongEPCWriteTest extends AppCompatActivity {

    private RfidManager mRfidManager;
    private TextView tvMessages;
    private EditText etNewEPC, etPassword;
    private NestedScrollView nestedScrollView;
    private String lastEPC = null;
    private byte[] currentEpc = null;
    private ScrollView TopScrollView;
    private final String TAG = "RFIDWriterApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.long_epcwrite_test);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("LongEPCWriteTest");


        // 初始化UI
        tvMessages = findViewById(R.id.tvMessages);
        etNewEPC = findViewById(R.id.etNewEPC);
        etPassword = findViewById(R.id.etPassword);
        Button btnRead = findViewById(R.id.btnRead);
        Button btnWrite = findViewById(R.id.btnWrite);
        Button btnClear = findViewById(R.id.btnClear);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        TopScrollView = findViewById(R.id.TopScrollView);


        // 初始化RfidManager
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "RFID 模組初始化失敗", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 註冊接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        //registerReceiver(mMessageReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(mMessageReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mMessageReceiver, filter);
        }

        // 按鈕行為
        btnRead.setOnClickListener(v -> startReading());

        btnWrite.setOnClickListener(v -> writeNewEPC());

        //btnClear.setOnClickListener(v -> tvMessages.setText(""));
        btnClear.setOnClickListener(v -> {
            // 添加淡出动画
            tvMessages.animate().alpha(0).setDuration(200).withEndAction(() -> {
                tvMessages.setText("Messages will appear here");
                tvMessages.setAlpha(1);
            }).start();

            etNewEPC.setText("");
            etPassword.setText("");
        });

        // 使用已初始化的變量
        etNewEPC.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                nestedScrollView.post(() -> {
                    Rect rect = new Rect();
                    etNewEPC.getGlobalVisibleRect(rect);
                    nestedScrollView.smoothScrollTo(0, rect.bottom);
                });
            }
        });

    }

    private void startReading() {
        int result = mRfidManager.SoftScanTrigger(true);
        if (result != 0) {
            String error = mRfidManager.GetLastError();
            Toast.makeText(this, "啟動讀取失敗: " + error, Toast.LENGTH_SHORT).show();
        } else {
            showMessage("開始讀取 EPC...");
        }
    }

    private void writeNewEPC() {
        String newEpcStr = etNewEPC.getText().toString().trim();
        String passwordStr = etPassword.getText().toString().trim();

        if (newEpcStr.isEmpty()) {
            Toast.makeText(this, "請輸入新EPC", Toast.LENGTH_SHORT).show();
            return;
        }

        // 檢查EPC長度是否為4的倍數（每個word為2 bytes）
        if (newEpcStr.length() % 4 != 0) {
            Toast.makeText(this, "EPC長度必須為4的倍數", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] newEpcBytes = hexStringToByteArray(newEpcStr);
            byte[] passwordBytes = passwordStr.isEmpty() ? null : hexStringToByteArray(passwordStr);

            // 計算PC值 (EPC長度以word為單位)
            int epcWordLength = newEpcBytes.length / 2;
            int pcValue = (epcWordLength << 11) | 0x0000; // 假設UMI=0, XI=0

            byte[] pcWord = new byte[]{
                    (byte) ((pcValue >> 8) & 0xFF),
                    (byte) (pcValue & 0xFF)
            };

            // 整合PC + 新EPC
            byte[] fullData = new byte[pcWord.length + newEpcBytes.length];
            System.arraycopy(pcWord, 0, fullData, 0, pcWord.length);
            System.arraycopy(newEpcBytes, 0, fullData, pcWord.length, newEpcBytes.length);

            // 寫入EPC記憶體區塊，從第2個byte開始（跳過CRC）
            DeviceResponse response = mRfidManager.RFIDDirectWriteTagByEPC(
                    passwordBytes,
                    currentEpc,  // 原始EPC（包含PC和CRC）
                    RFIDMemoryBank.EPC,
                    2,  // 起始位置：2（跳過CRC 2 bytes）
                    1,  // 重試次數
                    fullData
            );

            if (response == DeviceResponse.OperationSuccess) {
                showMessage("寫入完成，正在驗證...");
                new Handler().postDelayed(this::readAndVerifyNewEPC, 300);
            } else {
                String error = mRfidManager.GetLastError();
                showMessage("寫入失敗: " + error);
                Log.e("EPCWrite", "寫入錯誤: " + error);
                Log.e("EPCWrite", "嘗試寫入數據: " + bytesToHex(fullData));
            }

        } catch (Exception e) {
            showMessage("格式錯誤: " + e.getMessage());
            Log.e("EPCWrite", "寫入異常", e);
        }
    }

    // 輔助方法：byte數組轉十六進制字符串（用於調試）
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    private void readAndVerifyNewEPC() {
        // 重新讀取 Tag，並進行驗證
        readSingleTag();

        new Handler().postDelayed(() -> {
            // 讀取後進行 EPC 比對
            if (lastEPC != null && lastEPC.equalsIgnoreCase(etNewEPC.getText().toString().trim())) {
                showMessage("寫入成功且驗證通過！");
            } else {
                showMessage("⚠️ 寫入後驗證失敗，Tag未更新！");
            }
        }, 500); // 讀取後再比對
    }

    private void readSingleTag() {
        // 呼叫 RFID 管理器觸發掃描，並設置延遲來確保可以讀取到 Tag
        mRfidManager.SoftScanTrigger(true);

        // 你也可以在這裡添加自動停止掃描邏輯，根據需求進行設置
        // 例如：mRfidManager.SoftScanTrigger(false);
    }


    private void showMessage(String msg) {
        tvMessages.append("\n" + msg + "\n");
        TopScrollView.post(() -> TopScrollView.fullScroll(View.FOCUS_DOWN));
    }


    private byte[] hexStringToByteArray(String s) {
        s = s.replace(" ", ""); // 移除空格
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void stopReading() {
        int result = mRfidManager.SoftScanTrigger(false);
        if (result != 0) {
            String error = mRfidManager.GetLastError();
            Toast.makeText(this, "停止讀取失敗: " + error, Toast.LENGTH_SHORT).show();
        } else {
            showMessage("停止讀取 EPC");
        }
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Intent received: " + action); // 顯示收到的 action
            if (action == null) return;

            if (action.equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
                int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
                Log.d(TAG, "Response: " + response); // 顯示回應狀態

                if (response == GeneralString.RESPONSE_OPERATION_SUCCESS) {
                    String epc = intent.getStringExtra(GeneralString.EXTRA_EPC);
                    Log.d(TAG, "EPC received: " + epc); // 顯示讀取到的 EPC
                    if (epc != null) {
                        lastEPC = epc;
                        currentEpc = hexStringToByteArray(epc);  // 設置 currentEpc，確保 currentEpc 不為 null
                        Log.d(TAG, "currentEpc received: " + currentEpc); // 顯示讀取到的 EPC
                        showMessage("讀取到 EPC: \n" + lastEPC + "\n" + "長度:" + lastEPC.length());

                        /*// 自動滾動顯示清單
                        ScrollView scrollView = findViewById(R.id.TopScrollView);
                        if (scrollView != null) {
                            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        }*/
                    }
                } else {
                    showMessage("讀取失敗或超時");
                    Log.d(TAG, "讀取失敗或超時"); // 輸出讀取失敗的 Log
                }
                stopReading(); // 停止讀取
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMessageReceiver);
    }
}
