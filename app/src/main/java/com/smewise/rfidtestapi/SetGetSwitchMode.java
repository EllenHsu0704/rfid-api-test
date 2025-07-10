package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.SwitchMode;
import com.cipherlab.rfidapi.RfidManager;

public class SetGetSwitchMode extends AppCompatActivity {

    private RfidManager mManager;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_get_switch_mode);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("SetGetSwitchMode");


        // 初始化 RfidManager
        mManager = RfidManager.InitInstance(this);
        if (mManager == null) {
            Log.e("MainActivity", "Failed to initialize RfidManager.");
            return;
        }

        // 按鈕和 TextView
        Button btnRfidMode = findViewById(R.id.btn_set_rfid_only);
        Button btnPistolMode = findViewById(R.id.btn_set_pistol_only);
        Button btnGetStatus = findViewById(R.id.btn_get_mode);
        Button btnRfidPistol = findViewById(R.id.btn_set_rfid_and_pistol);  // 新增按鈕
        tvStatus = findViewById(R.id.tv_status);

        // 設置 RFID 模式
        btnRfidMode.setOnClickListener(view -> switchToRfidMode());

        // 設置條碼掃描模式 (Pistol 模式)
        btnPistolMode.setOnClickListener(view -> switchToPistolMode());

        // 設置 RFID 和條碼掃描器同時啟用
        btnRfidPistol.setOnClickListener(view -> switchToRfidAndPistolMode());

        // 獲取當前狀態
        btnGetStatus.setOnClickListener(view -> getSwitchMode());
    }

    // 切換到 RFID 模式
    private void switchToRfidMode() {
        int result = mManager.SetSwitchMode(SwitchMode.UHFRFIDReader);
        if (result == ClResult.S_OK.ordinal()) {
            tvStatus.setText("RFID mode activated.");
            Log.i("MainActivity", "Switched to RFID mode.");
        } else {
            String lastError = mManager.GetLastError();
            tvStatus.setText("Error: " + lastError);
            Log.e("MainActivity", "SetSwitchMode failed: " + lastError);
        }
    }

    // 切換到條碼掃描模式 (Pistol 模式)
    private void switchToPistolMode() {
        int result = mManager.SetSwitchMode(SwitchMode.BarcodeReader);
        if (result == ClResult.S_OK.ordinal()) {
            tvStatus.setText("Barcode Reader mode (Pistol) activated.");
            Log.i("MainActivity", "Switched to Barcode Reader mode.");
        } else {
            String lastError = mManager.GetLastError();
            tvStatus.setText("Error: " + lastError);
            Log.e("MainActivity", "SetSwitchMode failed: " + lastError);
        }
    }

    // 切換到 RFID 和條碼掃描器同時啟用模式
    private void switchToRfidAndPistolMode() {
        int result = mManager.SetSwitchMode(SwitchMode.UHFRFIDBarcodeReader);  // UHFRFIDBarcodeReader 模式來啟用兩者
        if (result == ClResult.S_OK.ordinal()) {
            tvStatus.setText("RFID and Barcode Reader mode activated simultaneously.");
            Log.i("MainActivity", "Switched to RFID and Barcode Reader mode simultaneously.");
        } else {
            String lastError = mManager.GetLastError();
            tvStatus.setText("Error: " + lastError);
            Log.e("MainActivity", "SetSwitchMode failed: " + lastError);
        }
    }

    // 獲取設備狀態
    private void getSwitchMode() {
        // 獲取當前 UHF RFID 讀取器的模式
        SwitchMode mode = mManager.GetSwitchMode();
        if (mode == SwitchMode.Err) {
            String lastError = mManager.GetLastError();
            Log.e("GetSwitchMode", "GetSwitchMode failed: " + lastError);
            tvStatus.setText("Error: " + lastError);
        } else {
            tvStatus.setText("Current mode: " + mode);
        }
    }
}