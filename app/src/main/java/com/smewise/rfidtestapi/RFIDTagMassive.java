package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDMemoryBank;
import com.cipherlab.rfidapi.RfidManager;

public class RFIDTagMassive extends AppCompatActivity {
    private static final String TAG = "RFIDTest";
    private RfidManager mRfidManager = null;
    private TextView resultTextView;
    private EditText dataInput;
    private Spinner memoryBankSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rfidtag_massive);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("RFIDTagMassive");

        // 初始化 UI 元件
        resultTextView = findViewById(R.id.resultTextView);
        dataInput = findViewById(R.id.dataInput);
        memoryBankSpinner = findViewById(R.id.memoryBankSpinner);
        Button readButton = findViewById(R.id.readButton);
        Button writeButton = findViewById(R.id.writeButton);

        // 設置記憶庫選擇下拉選單
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"EPC", "User", "Reserved"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        memoryBankSpinner.setAdapter(adapter);

        // 初始化 RfidManager
        mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            resultTextView.setText("無法初始化 RfidManager");
            Log.e(TAG, "RfidManager 初始化失敗");
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

        // 讀取按鈕點擊事件
        readButton.setOnClickListener(v -> performReadOperation());

        // 寫入按鈕點擊事件
        writeButton.setOnClickListener(v -> performWriteOperation());
    }

    private void performReadOperation() {
        resultTextView.setText("正在執行讀取操作...");
        RFIDMemoryBank bank = getSelectedMemoryBank();
        int result = mRfidManager.RFIDReadTagMassive(null, bank, 0, 0);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            String errorMessage = "讀取失敗: " + error;
            resultTextView.setText(errorMessage);
            Log.e(TAG, "GetLastError = " + error);
        } else {
            resultTextView.setText("讀取操作已啟動，等待觸發器...");
            Log.i(TAG, "讀取操作已啟動");
        }
    }

    private void performWriteOperation() {
        String inputData = dataInput.getText().toString().trim();
        if (inputData.isEmpty()) {
            Toast.makeText(this, "請輸入要寫入的數據", Toast.LENGTH_SHORT).show();
            return;
        }
        if (inputData.getBytes().length > 32) {
            Toast.makeText(this, "數據長度超過 32 位元組", Toast.LENGTH_SHORT).show();
            return;
        }

        resultTextView.setText("正在執行寫入操作...");
        RFIDMemoryBank bank = getSelectedMemoryBank();
        byte[] dataBytes = inputData.getBytes();
        int result = mRfidManager.RFIDWriteTagMassive(null, bank, dataBytes, 0, dataBytes.length);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            String errorMessage = "寫入失敗: " + error;
            resultTextView.setText(errorMessage);
            Log.e(TAG, "GetLastError = " + error);
        } else {
            resultTextView.setText("寫入操作已啟動，等待觸發器...");
            Log.i(TAG, "寫入操作已啟動");
        }
    }

    private RFIDMemoryBank getSelectedMemoryBank() {
        String selected = memoryBankSpinner.getSelectedItem().toString();
        switch (selected) {
            case "EPC":
                return RFIDMemoryBank.EPC;
            case "User":
                return RFIDMemoryBank.User;
            case "Reserved":
                return RFIDMemoryBank.Reserved;
            default:
                return RFIDMemoryBank.EPC;
        }
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || !action.equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
                return;
            }

            int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
            switch (response) {
                case GeneralString.RESPONSE_OPERATION_SUCCESS:
                    String epc = intent.getStringExtra(GeneralString.EXTRA_EPC);
                    String successMessage = "讀取成功，EPC: " + epc;
                    resultTextView.setText(successMessage);
                    Log.i(TAG, successMessage);
                    break;
                case GeneralString.RESPONSE_OPERATION_FINISH:
                    resultTextView.setText("操作完成");
                    Log.i(TAG, "RESPONSE_OPERATION_FINISH");
                    break;
                case GeneralString.RESPONSE_OPERATION_FAIL:
                    resultTextView.setText("操作失敗");
                    Log.i(TAG, "RESPONSE_OPERATION_FAIL");
                    break;
                case GeneralString.RESPONSE_OPERATION_TIMEOUT_FAIL:
                    resultTextView.setText("操作超時失敗");
                    Log.i(TAG, "RESPONSE_OPERATION_TIMEOUT_FAIL");
                    break;
                default:
                    resultTextView.setText("未知回應: " + response);
                    Log.i(TAG, "未知回應: " + response);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMessageReceiver);
        if (mRfidManager != null) {
            mRfidManager.Release();
        }
    }
}
