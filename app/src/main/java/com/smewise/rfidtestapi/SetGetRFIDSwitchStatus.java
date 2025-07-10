package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.SwitchMode;
import com.cipherlab.rfid.TriggerSwitchMode;
import com.cipherlab.rfidapi.RfidManager;

import java.util.ArrayList;

public class SetGetRFIDSwitchStatus extends AppCompatActivity {

    private RfidManager mRfidManager;
    private TextView tvStatus;
    private Button btnRfidMode;
    private Button btnPistolMode;
    private Button btnGetStatus;
    private Button btnTriggeroff;
    private Button btnSwitchrfid;
    private Button btnSwitchbarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_get_rfidswitch_status);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("SetGetRFIDSwitchStatus");


        // 初始化 RfidManager
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "RFID Manager initialization failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // 查找 UI 元素
        tvStatus = findViewById(R.id.tv_status);
        btnRfidMode = findViewById(R.id.btn_rfid_mode);
        btnPistolMode = findViewById(R.id.btn_pistol_mode);
        btnGetStatus = findViewById(R.id.btn_get_status);
        btnTriggeroff = findViewById(R.id.btn_Trigger_off);
        btnSwitchrfid = findViewById(R.id.btn_SwitchMode_rfid);
        btnSwitchbarcode = findViewById(R.id.btn_SwitchMode_barcode);

        //disabled Trigger Switch Mode
        btnTriggeroff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 禁用 TriggerSwitchMode
                int result = mRfidManager.SetTriggerSwitchMode(false);
                if (result != ClResult.S_OK.ordinal()) {
                    String lastError = mRfidManager.GetLastError();
                    tvStatus.setText("Error: " + lastError);
                } else {
                    tvStatus.setText("Trigger Switch Mode Disabled");

                    // 重新初始化 RfidManager
                    mRfidManager = RfidManager.InitInstance(getApplicationContext());
                    if (mRfidManager == null) {
                        tvStatus.setText("RfidManager re-initialization failed");
                    } else {
                        tvStatus.setText("RfidManager re-initialized");
                    }

                    // 進一步檢查 TriggerSwitchMode 的狀態
                    TriggerSwitchMode triggerStatus = mRfidManager.GetTriggerSwitchMode();
                    if (!triggerStatus.TriggerSwitchStatus) {
                        tvStatus.setText("Trigger Switch Mode successfully disabled");
                    } else {
                        tvStatus.setText("Failed to disable Trigger Switch Mode");
                    }
                }
            }
        });

        //Ues UHFRFIDReader
        btnSwitchrfid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个包含目标模式的 ArrayList
                ArrayList<SwitchMode> switchModeList = new ArrayList<>();
                switchModeList.add(SwitchMode.UHFRFIDReader);  // 設置為 RFID 模式

                // 调用 SetSwitchModeByCounts 来设置模式
                int result = mRfidManager.SetSwitchModeByCounts(switchModeList);

                // 根据返回值显示结果
                if (result != ClResult.S_OK.ordinal()) {
                    String lastError = mRfidManager.GetLastError();
                    tvStatus.setText("Error: " + lastError);
                } else {
                    tvStatus.setText("Switch Mode set to RFID (UHFRFIDReader)");
                }
            }
        });

        btnSwitchbarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个包含目标模式的 ArrayList
                ArrayList<SwitchMode> switchModeList = new ArrayList<>();
                switchModeList.add(SwitchMode.BarcodeReader);  // 設置為 Barcode 模式

                // 调用 SetSwitchModeByCounts 来设置模式
                int result = mRfidManager.SetSwitchModeByCounts(switchModeList);

                // 根据返回值显示结果
                if (result != ClResult.S_OK.ordinal()) {
                    String lastError = mRfidManager.GetLastError();
                    tvStatus.setText("Error: " + lastError);
                } else {
                    tvStatus.setText("Switch Mode set to Barcode (BarcodeReader)");
                }
            }
        });



        // 設置 RFID 模式按鈕
        btnRfidMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 設置 RFID 模式
                int result = mRfidManager.SetRFIDSwitchStatus(true);
                if (result != ClResult.S_OK.ordinal()) {
                    String lastError = mRfidManager.GetLastError();
                    tvStatus.setText("Error: " + lastError);
                } else {
                    tvStatus.setText("Set to RFID Mode");
                }
            }
        });

        // 設置 Pistol Only 模式按鈕
        btnPistolMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 設置 Pistol Only 模式
                int result = mRfidManager.SetRFIDSwitchStatus(false);
                if (result != ClResult.S_OK.ordinal()) {
                    String lastError = mRfidManager.GetLastError();
                    tvStatus.setText("Error: " + lastError);
                } else {
                    tvStatus.setText("Set to Pistol Only Mode");
                }
            }
        });

        // 獲取當前模式按鈕
        btnGetStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取当前模式
                int status = mRfidManager.GetRFIDSwitchStatus();
                String lastError = mRfidManager.GetLastError();

                if (status == -1) {
                    tvStatus.setText("Error: " + lastError);
                } else {
                    if (status == 1) {
                        tvStatus.setText("Current Mode: RFID Mode");
                    } else if (status == 0) {
                        tvStatus.setText("Current Mode: Pistol Only Mode");
                    } else {
                        tvStatus.setText("Unexpected Status: " + status);
                    }
                }

                // 打印调试信息
                Log.d("RFIDStatus", "Status: " + status + " Error: " + lastError);
            }
        });
    }


}