// 不設定按鈕監聽器setListeners();
// 按鈕單獨的版本
package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.Gen2Settings;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.InventoryStatusSettings;
import com.cipherlab.rfid.InventoryType;
import com.cipherlab.rfid.RFIDMemoryBank;
import com.cipherlab.rfid.RFIDMode;
import com.cipherlab.rfid.SLFlagSettings;
import com.cipherlab.rfid.SessionSettings;
import com.cipherlab.rfidapi.RfidManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReadUserReservedBankByTID extends AppCompatActivity {
    private static final String TAG = "ReadByTidActivity";
    private RfidManager mRfidManager;
    private EditText etTid, etReadLengthUser, etStartIndexReserved, etReadLengthReserved;
    private TextView tvOutput;
    private Button btnReadTid, btnReadUserBank, btnReadReservedBank;

    // 狀態變數，用於區分是哪個按鈕觸發的掃描
    private enum ScanAction {
        NONE,
        GET_TID,
        READ_USER_BANK,
        READ_RESERVED_BANK
    }

    private ScanAction currentAction = ScanAction.NONE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_user_reserved_bank_by_tid);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("ReadUserReservedBankByTID");

        // 1. 初始化 RFID Manager
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "RfidManager 初始化失敗", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. 綁定 UI 元件
        etTid = findViewById(R.id.etTid);
        btnReadTid = findViewById(R.id.btnReadTid);
        etReadLengthUser = findViewById(R.id.etReadLengthUser);
        btnReadUserBank = findViewById(R.id.btnReadUserBank);
        etStartIndexReserved = findViewById(R.id.etStartIndexReserved);
        etReadLengthReserved = findViewById(R.id.etReadLengthReserved);
        btnReadReservedBank = findViewById(R.id.btnReadReservedBank);
        tvOutput = findViewById(R.id.tvOutput);
        tvOutput.setMovementMethod(new ScrollingMovementMethod()); // 讓 TextView 可以滾動


        //Session S1 會讓標籤在盤點後保持活躍，允許後續的連續存取。
        //logToOutput("正在設定 Gen2 Session 為 S1...");
        Gen2Settings settings = new Gen2Settings();
        settings.Session = SessionSettings.S1;
        settings.InventoryStatus_Action = InventoryStatusSettings.AB_FLIP;
        settings.SL_Flag = SLFlagSettings.Asserted;
        int Sre = mRfidManager.SetGen2(settings);

        if (Sre == ClResult.S_OK.ordinal()) {
            logToOutput("Gen2 Session 已成功設為 S1。");
        }else {
                logToOutput("設定 Gen2 Session 失敗！錯誤: " + mRfidManager.GetLastError());
        }

        // 3. 註冊廣播接收器以接收 RFID 數據
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_CONNECTED);
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(mDataReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mDataReceiver, filter);
        }

        /*
        //
        logToOutput("正在設定 Gen2 Session 為 S1...");
        Gen2Settings settings = new Gen2Settings();
        settings.Session = SessionSettings.S1;
        settings.InventoryStatus_Action = InventoryStatusSettings.AB_FLIP;
        int Sre = mRfidManager.SetGen2(settings);
        if (Sre == ClResult.S_OK.ordinal()) {
            logToOutput("Gen2 Session 已成功設為 S1。");
            if (Sre != ClResult.S_OK.ordinal()) {
                logToOutput("設定 Gen2 Session 失敗！錯誤: " + mRfidManager.GetLastError());
            }
        }*/



        // 4. 設定按鈕點擊事件
        btnReadTid.setOnClickListener(v -> {
            logToOutput("正在掃描 TID...");
            currentAction = ScanAction.GET_TID;
            // 使用 RFIDDirectStartInventoryRound 函式直接啟動 EPC+TID 盤點
            // 參數1: InventoryType.EPC_AND_TID 指定盤點類型
            // 參數2: count 指定盤點次數，例如 3 次
            int re = mRfidManager.RFIDDirectStartInventoryRound(InventoryType.EPC_AND_TID, 3);
            if (re != ClResult.S_OK.ordinal()) {
                String errMsg = mRfidManager.GetLastError();
                logToOutput("啟動 TID 失敗: " + errMsg);
            }
        });

        btnReadUserBank.setOnClickListener(v -> {
            tvOutput.setText("");
            String tidStr = etTid.getText().toString();
            String lengthStr = etReadLengthUser.getText().toString();

            if (tidStr.isEmpty() || lengthStr.isEmpty()) {
                Toast.makeText(this, "TID 和讀取長度不能為空", Toast.LENGTH_SHORT).show();
                return;
            }
            logToOutput("準備讀取 User Bank...");
            currentAction = ScanAction.READ_USER_BANK;

            byte[] tidBytes = hexStringToByteArray(tidStr);
            if (tidBytes == null) {
                Toast.makeText(this, "TID 格式不正確", Toast.LENGTH_SHORT).show();
                return;
            }

            int lengthInWords = Integer.parseInt(lengthStr);

            int re = mRfidManager.RFIDDirectReadTagByTID(
                    null,
                    tidBytes,
                    RFIDMemoryBank.User,
                    0,
                    lengthInWords,
                    3
            );
            if (re != ClResult.S_OK.ordinal()) {
                String errMsg = mRfidManager.GetLastError();
                logToOutput("讀取 User Bank 命令失敗: " + errMsg);
            }
        });

        btnReadReservedBank.setOnClickListener(v -> {
            tvOutput.setText("");
            String tidStr = etTid.getText().toString();
            String startStr = etStartIndexReserved.getText().toString();
            String lengthStr = etReadLengthReserved.getText().toString();

            if (tidStr.isEmpty() || startStr.isEmpty() || lengthStr.isEmpty()) {
                Toast.makeText(this, "TID、起始位置和讀取長度不能為空", Toast.LENGTH_SHORT).show();
                return;
            }
            logToOutput("準備讀取 Reserved Bank...");
            currentAction = ScanAction.READ_RESERVED_BANK;

            byte[] tidBytes = hexStringToByteArray(tidStr);
            if (tidBytes == null) {
                Toast.makeText(this, "TID 格式不正確", Toast.LENGTH_SHORT).show();
                return;
            }

            int startIndexInWords = Integer.parseInt(startStr);
            int lengthInWords = Integer.parseInt(lengthStr);

            int re = mRfidManager.RFIDDirectReadTagByTID(
                    null,
                    tidBytes,
                    RFIDMemoryBank.Reserved,
                    startIndexInWords,
                    lengthInWords,
                    3
            );
            if (re != ClResult.S_OK.ordinal()) {
                String errMsg = mRfidManager.GetLastError();
                logToOutput("讀取 Reserved Bank 命令失敗: " + errMsg);
            }
        });
    }

    // 將日誌訊息附加到 TextView
    private void logToOutput(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String currentText = tvOutput.getText().toString();
        String newText = timestamp + ": " + message + "\n" + currentText;
        tvOutput.setText(newText);
        Log.d(TAG, message);
    }

    // 工具函數: 將 Hex 字串轉換為 byte 陣列
    public static byte[] hexStringToByteArray(String s) {
        if (s == null || s.length() % 2 != 0) {
            return null;
        }
        int len = s.length();
        byte[] data = new byte[len / 2];
        try {
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting hex string to byte array", e);
            return null;
        }
        return data;
    }


    // 廣播接收器，用於處理 RFID 服務的回傳資料
    private final BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GeneralString.Intent_RFIDSERVICE_CONNECTED.equals(action)) {
                // 服務連接成功
                String apiVersion = mRfidManager.GetAPIVersion();
                logToOutput("RFID 服務已連接，API 版本: " + apiVersion);
                Toast.makeText(ReadUserReservedBankByTID.this, "RFID Service Connected", Toast.LENGTH_SHORT).show();
            } else if (GeneralString.Intent_RFIDSERVICE_TAG_DATA.equals(action)) {
                // 接收到標籤數據
                // 從 Intent 中提取數據
                int type = intent.getIntExtra(GeneralString.EXTRA_DATA_TYPE, -1);
                int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);

                // 停止掃描
                mRfidManager.SoftScanTrigger(false);

                // 根據當前的操作來處理數據
                switch (currentAction) {
                    case GET_TID: // 處理 Read TID 的結果
                        // type 2 代表 Inventory EPC+TID 的回傳
                        if (type == 2 && response == 0) {
                            String tid = intent.getStringExtra(GeneralString.EXTRA_TID);
                            if (tid != null && !tid.isEmpty()) {
                                etTid.setText(tid);
                                logToOutput("成功掃描到 TID: " + tid);
                            }
                        } else {
                            logToOutput("掃描 TID 失敗。Type: " + type + ", Response: " + response);
                        }
                        break;
                    case READ_USER_BANK: // 處理 Read User Bank 的結果
                    case READ_RESERVED_BANK: // 處理 Read Reserved Bank 的結果
                        // type 3 代表直接讀取標籤的回傳
                        if (type == 3) {
                            if (response == 0) { // RESPONSE_OPERATION_SUCCESS
                                String readData = intent.getStringExtra(GeneralString.EXTRA_ReadData);
                                logToOutput("讀取成功！數據 (HEX): " + readData);
                            } else {
                                logToOutput("讀取失敗。Response Code: " + response);
                            }
                        }
                        //印出是哪個 bank 回來的
                        logToOutput("→ 這是讀取回應 [Bank: " +
                                (currentAction == ScanAction.READ_USER_BANK ? "User" :
                                        currentAction == ScanAction.READ_RESERVED_BANK ? "Reserved" : "Unknown")
                                + "], Type: 3, Response: " + response);
                        break;
                }
                currentAction = ScanAction.NONE; // 重置狀態
                //logToOutput("接收到 TAG 回應，Type: " + type + ", Response: " + response);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 釋放資源並取消註冊接收器
        if (mRfidManager != null) {
            mRfidManager.Release();
        }
        unregisterReceiver(mDataReceiver);
    }
}


