package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.DeviceResponse;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDMemoryBank;
import com.cipherlab.rfidapi.RfidManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class writeEPCbyTID extends AppCompatActivity {
    private static final String TAG = "RFIDApp";
    private RfidManager mRfidManager;
    private EditText tidInput, epcInput, passwordInput;
    private TextView resultText;
    private Button readButton, writeButton, clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_epcby_tid);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("WriteEPCbyTID");
        // 初始化 UI 元件
        tidInput = findViewById(R.id.tidInput);
        epcInput = findViewById(R.id.epcInput);
        passwordInput = findViewById(R.id.passwordInput);
        resultText = findViewById(R.id.resultText);
        readButton = findViewById(R.id.readButton);
        writeButton = findViewById(R.id.writeButton);
        clearButton = findViewById(R.id.clearButton);

        // 設置預設值
        /*
        tidInput.setText("e2801100200028d30fc3ffff");
        epcInput.setText("3330afec2b0115c000000001");
         */
        passwordInput.setText("00000000");



        // 初始化 RFID 管理器
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "無法初始化 RFID 管理器", Toast.LENGTH_LONG).show();
            return;
        }

        // 註冊廣播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        //registerReceiver(mMessageReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(mMessageReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mMessageReceiver, filter);
        }

        // 讀取按鈕點擊處理
        readButton.setOnClickListener(v -> {
            String tidStr = tidInput.getText().toString().trim();
            String passwordStr = passwordInput.getText().toString().trim();

            if (tidStr.isEmpty()) {
                Toast.makeText(this, "請輸入 TID", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                byte[] tidBytes = hexStringToByteArray(tidStr);
                byte[] passwordBytes = getPasswordBytes(passwordStr);

                int result = mRfidManager.RFIDDirectReadTagByTID(
                        null,
                        tidBytes,
                        RFIDMemoryBank.EPC,
                        4, // 從字 2 開始（跳過 CRC 和 PC）
                        12, // 讀取 6 字（12 字節，假設 EPC 長度，可調整）
                        3  // 重試次數
                );

                if (result != ClResult.S_OK.ordinal()) {
                    String error = mRfidManager.GetLastError();
                    showMessage("讀取失敗: " + error);
                    Log.e(TAG, "讀取失敗: " + error);
                }
            } catch (Exception e) {
                showMessage("錯誤: " + e.getMessage());
                Log.e(TAG, "讀取錯誤: " + e.getMessage());
            }
        });

        // 寫入按鈕點擊處理
        writeButton.setOnClickListener(v -> {
            String tidStr = tidInput.getText().toString().trim();
            String epcStr = epcInput.getText().toString().trim();
            String passwordStr = passwordInput.getText().toString().trim();

            if (tidStr.isEmpty() || epcStr.isEmpty()) {
                Toast.makeText(this, "請輸入 TID 和 EPC", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "輸入 TID: " + tidStr + ", EPC: " + epcStr +
                    ", 密碼: " + (passwordStr.isEmpty() ? "null" : passwordStr));

            try {
                byte[] tidBytes = hexStringToByteArray(tidStr);
                byte[] epcBytes = hexStringToByteArray(epcStr);
                byte[] passwordBytes = getPasswordBytes(passwordStr);

                // 檢查 EPC 長度（4 的倍數，對應字數）
                if (epcStr.length() % 4 != 0) {
                    Toast.makeText(this, "EPC 長度必須為 4 的倍數", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (epcBytes.length > 32) {
                    Toast.makeText(this, "EPC 資料超過 32 字節", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 計算 EPC 長度（以字為單位）
                int epcWordLength = epcBytes.length / 2; // 12 字節 = 6 字
                Log.d(TAG, "EPC 字長度: " + epcWordLength);

                // 設置 PC 值
                int pcValue = (epcWordLength << 11); // 長度位於 bit 15-11
                byte[] pcBytes = new byte[] {
                        (byte) ((pcValue >> 8) & 0xFF),
                        (byte) (pcValue & 0xFF)
                };
                Log.d(TAG, "設置 PC 值: " + String.format("%02X%02X", pcBytes[0], pcBytes[1]));

                // 合併 PC 和 EPC
                byte[] combinedData = new byte[2 + epcBytes.length];
                System.arraycopy(pcBytes, 0, combinedData, 0, pcBytes.length);
                System.arraycopy(epcBytes, 0, combinedData, pcBytes.length, epcBytes.length);
                Log.d(TAG, "合併資料: " + bytesToHex(combinedData));

                DeviceResponse response = mRfidManager.RFIDDirectWriteTagByTID(
                        //passwordBytes,
                        null,
                        tidBytes,
                        RFIDMemoryBank.EPC,
                        2, // 從字 2 開始（跳過 CRC 和 PC）
                        3, // 重試次數
                        combinedData
                );

                if (response == DeviceResponse.OperationSuccess) {
                    showMessage("寫入成功");
                    Log.i(TAG, "寫入成功");
                } else {
                    String error = mRfidManager.GetLastError();
                    showMessage("寫入失敗: " + error);
                    Log.e(TAG, "寫入失敗: " + error);
                }
            } catch (Exception e) {
                showMessage("錯誤: " + e.getMessage());
                Log.e(TAG, "寫入錯誤: " + e.getMessage());
            }
        });

        // 清空按鈕點擊處理
        clearButton.setOnClickListener(v -> {
            tidInput.setText("e2801100200028d30fc3ffff");
            epcInput.setText("3330afec2b0115c000000001");
            passwordInput.setText("00000000");
            showMessage("清除完成");
        });
    }

    private byte[] getPasswordBytes(String passwordStr) {
        if (passwordStr.isEmpty() || passwordStr.length() != 8 || !passwordStr.matches("[0-9A-Fa-f]{8}")) {
            return new byte[] { 0x00, 0x00, 0x00, 0x00 }; // 預設密碼 00000000
        }
        //return hexStringToByteArray(passwordStr);
        byte[] bytes = hexStringToByteArray(passwordStr);
        if (bytes == null) {
            Log.d(TAG, "密碼格式無效，使用預設密碼");
            return new byte[] { 0x00, 0x00, 0x00, 0x00 };
        }
        return bytes;
    }

    private byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("[^0-9A-Fa-f]", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void showMessage(String message) {
        runOnUiThread(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            resultText.append(timestamp + " - " + message + "\n");

            NestedScrollView scrollView = findViewById(R.id.scrollMessages);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }


    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
                int type = intent.getIntExtra(GeneralString.EXTRA_DATA_TYPE, -1);
                int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
                String tid = intent.getStringExtra(GeneralString.EXTRA_TID); // 嘗試獲取 TID
                String PC = intent.getStringExtra(GeneralString.EXTRA_PC);
                String EPC = intent.getStringExtra(GeneralString.EXTRA_EPC);
                String data = intent.getStringExtra(GeneralString.EXTRA_ReadData);

                runOnUiThread(() -> {
                    Log.d(TAG, "廣播資料: type=" + type + ", response=" + response +
                            ", data=" + data + ", tid=" + tid + ", pc=" + PC + ", epc=" + EPC);
                    switch (response) {
                        case 0: // RESPONSE_OPERATION_SUCCESS
                            if (data != null && !data.isEmpty()) {
                                epcInput.setText(data);
                                showMessage("讀取成功: EPC = " + data);
                                Log.i(TAG, "讀取成功: EPC = " + data);
                            } else {
                                resultText.setText("讀取成功，但無資料");
                                Log.i(TAG, "讀取成功，但無資料");
                            }
                            break;
                        case 1: // RESPONSE_OPERATION_FINISH
                            showMessage("操作完成  ");
                            Log.i(TAG, "操作完成");
                            break;
                        case 2: // RESPONSE_OPERATION_TIMEOUT_FAIL
                            showMessage("操作超時");
                            Log.e(TAG, "操作超時");
                            break;
                        case 6: // RESPONSE_PASSWORD_FAIL
                            showMessage("密碼驗證失敗");
                            Log.e(TAG, "密碼驗證失敗");
                            break;
                        case 7: // RESPONSE_OPERATION_FAIL
                            showMessage("操作失敗");
                            Log.e(TAG, "操作失敗");
                            break;
                        case 251: // DEVICE_BUSY
                            showMessage("設備忙碌");
                            Log.e(TAG, "設備忙碌");
                            break;
                        default:
                            showMessage("未知回應: " + response);
                            Log.e(TAG, "未知回應: " + response);
                            break;
                    }
                    Log.d(TAG, "廣播資料: type=" + type + ", response=" + response);
                });
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRfidManager != null) {
            try {
                mRfidManager.Release();
            } catch (Exception e) {
                Log.e(TAG, "釋放 RfidManager 錯誤: " + e.getMessage());
            }
        }
        unregisterReceiver(mMessageReceiver);
    }
}
