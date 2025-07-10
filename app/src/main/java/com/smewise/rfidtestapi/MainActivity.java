package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.SwitchMode;
import com.cipherlab.rfidapi.RfidManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15;
    RfidManager mRfidManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.GRAY);


        // 初始化 RfidManager
        RfidManager mRfidManager = RfidManager.InitInstance(this);
        if (mRfidManager == null) {
            Toast.makeText(this, "RFID Manager initialization failed", Toast.LENGTH_SHORT).show();
            return;
        }

        //RfidManager mRfidManager = RfidManager.InitInstance(this);
        //IntentFilter filter = new IntentFilter();
        b1 = findViewById(R.id.bt01);
        b2 = findViewById(R.id.bt02);
        b3 = findViewById(R.id.bt03);
        b4 = findViewById(R.id.bt04);
        b5 = findViewById(R.id.bt05);
        b6 = findViewById(R.id.bt06);
        b7 = findViewById(R.id.bt07);
        b8 = findViewById(R.id.bt08);
        b9 = findViewById(R.id.bt09);
        b10 = findViewById(R.id.bt010);
        b11 = findViewById(R.id.bt011);
        b12 = findViewById(R.id.bt012);
        b13 = findViewById(R.id.bt013);
        b14 = findViewById(R.id.bt014);
        b15 = findViewById(R.id.bt015);




        //GetServiceVersion
        //KeepDeviceAlive
        //ShutdownDevice
        //GetAPIVersion
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, GetVersion.class);
                startActivity(intent);
            }
        });

        //GetSwitchMode
        //SetSwitchMode
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, SetGetSwitchMode.class);
                startActivity(intent);
            }
        });



        //SetRFIDSwitchStatus
        //GetRFIDSwitchStatus
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在進入 SetRFIDSwitchStatus 前禁用 TriggerSwitchMode
                if (mRfidManager != null) {
                    // 禁用 TriggerSwitchMode，這樣 SetRFIDSwitchStatus 就能正常工作
                    ArrayList<SwitchMode> switchModes = new ArrayList<>();
                    switchModes.add(SwitchMode.UHFRFIDReader);  // 設置 RFID 模式
                    mRfidManager.SetSwitchModeByCounts(switchModes); // 禁用觸發模式
                }

                // 跳轉到 SetRFIDSwitchStatus Activity
                Intent intent = new Intent(MainActivity.this, SetGetRFIDSwitchStatus.class);
                startActivity(intent);
            }
        });


        //ScanTest
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, ScanTest.class);
                startActivity(intent);
            }
        });


        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, ContinuousInventory.class);
                startActivity(intent);
            }
        });

        //
        b6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, RFIDTagMassive.class);
                startActivity(intent);
            }
        });

        //
        b7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, RFIDDirectInventoryRound.class);
                startActivity(intent);
            }
        });


        b8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, LongEPCWriteTest.class);
                startActivity(intent);
            }
        });


        b9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, writeEPCbyTID.class);
                startActivity(intent);
            }
        });


        //ReadUserReservedBankByTID
        b10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, ReadUserReservedBankByTID.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mine_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_button) {
            Toast.makeText(this, "Factory Default！", Toast.LENGTH_SHORT).show();
            // 可選：跳轉到其他活動，例如 GetVersion
            // Intent intent = new Intent(MainActivity.this, GetVersion.class);
            // startActivity(intent);
            RfidManager mRfidManager = RfidManager.InitInstance(this);
            int re = mRfidManager.ResetToDefault();
            if (re != ClResult.S_OK.ordinal()) {
                String last = mRfidManager.GetLastError();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}