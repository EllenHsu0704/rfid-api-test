package com.smewise.rfidtestapi;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.TextView;
import android.widget.Toast;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.DeviceEvent;
import com.cipherlab.rfid.DeviceVoltageInfo;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfidapi.RfidManager;

public class GetVersion extends AppCompatActivity {
    RfidManager mRfidManager = null;
    TextView tv1 = null;
    Button b1, b2, b3, b4;
    private static final String TAG = "GetVersion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.version);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("GetVersion");

        mRfidManager = RfidManager.InitInstance(this);

        if (mRfidManager == null)
            return;

        tv1 = findViewById(R.id.textView1);
        b1 = findViewById(R.id.bt01);
        b2 = findViewById(R.id.bt02);
        b3 = findViewById(R.id.bt03);
        b4 = findViewById(R.id.bt04);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_RFIDSERVICE_CONNECTED);
        filter.addAction(GeneralString.Intent_RFIDSERVICE_TAG_DATA);
        filter.addAction(GeneralString.Intent_RFIDSERVICE_EVENT);
        filter.addAction(GeneralString.Intent_FWUpdate_ErrorMessage);
        filter.addAction(GeneralString.Intent_FWUpdate_Percent);
        filter.addAction(GeneralString.Intent_FWUpdate_Finish);
        filter.addAction(GeneralString.Intent_GUN_Attached);
        filter.addAction(GeneralString.Intent_GUN_Unattached);
        filter.addAction(GeneralString.Intent_GUN_Power);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(myDataReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(myDataReceiver, filter);
        }



        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在這裡執行按鈕被點擊後的操作
                try {
                    // 獲取 RFID 服務版本
                    String serviceVersion = mRfidManager.GetServiceVersion();
                    // 顯示版本信息在 TextView 中
                    tv1.setText("RFID Service Version: " + serviceVersion);

                } catch (Exception e) {
                    // 錯誤處理
                    e.printStackTrace();
                    tv1.setText("Failed to get service version");
                }
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 按鈕 B2 被點擊時執行的操作
                try {
                    // 獲取 RFID 服務 API 版本
                    String apiVersion = mRfidManager.GetAPIVersion();
                    // 顯示版本信息在 TextView 中
                    tv1.setText("RFID API Version: " + apiVersion);

                } catch (Exception e) {
                    // 錯誤處理
                    e.printStackTrace();
                    tv1.setText("Failed to get API version");
                }
            }
        });

        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 按鈕 B3 被點擊時執行的操作
                ShutdownDevice();
            }
        });

        //GetBatteryLifePercent
        b4.setOnClickListener(new View.OnClickListener()
                              {
                                  @Override
                                  public void onClick(View v) {
                                      DeviceVoltageInfo info = new DeviceVoltageInfo();
                                      int re = mRfidManager.GetBatteryLifePercent(info);
                                      if(re!=ClResult.S_OK.ordinal())
                                      {
                                          String m = mRfidManager.GetLastError();
                                          Log.e(TAG, "GetLastError = " + m);
                                      }
                                      tv1.setText("info Percentage = " + Integer.toString(info.Percentage) + " , "+ "\n"
                                              + "info ChargeStatus = " + Integer.toString(info.ChargeStatus) + " , " + "\n"
                                              + "info Voltage = " + info.Voltage);
                                  }
                              }
        );
    }


    private void ShutdownDevice() {
        try {
            // 調用 ShutdownDevice() 來關閉 RFID 設備
            int re = mRfidManager.ShutdownDevice();

            // 檢查結果是否成功
            if (re != ClResult.S_OK.ordinal()) {
                // 取得錯誤訊息
                String errorMessage = mRfidManager.GetLastError();

                // 顯示錯誤訊息
                Log.e("RFID", "Shutdown failed: " + errorMessage);
                tv1.setText("Shutdown failed: " + errorMessage);
            } else {
                // 成功關閉設備
                tv1.setText("Device successfully shut down");
            }
        } catch (Exception e) {
            // 捕獲任何異常並顯示
            e.printStackTrace();
            tv1.setText("Error shutting down device");
        }
    }

    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_CONNECTED))
            {
                String PackageName = intent.getStringExtra("PackageName");

                // / make sure this AP does already connect with RFID service (after call RfidManager.InitInstance(this)
                String ver = "";
                ver = mRfidManager.GetServiceVersion();
                String api_ver = mRfidManager.GetAPIVersion();
                tv1.setText(PackageName + "," + ver + " , " + api_ver);
                //ForTest();

                String m_PackageName = PackageName;
                Toast.makeText(GetVersion.this,  m_PackageName, Toast.LENGTH_SHORT).show();
                //Toast.makeText(MainActivity.this,  "Intent_RFIDSERVICE_CONNECTED", Toast.LENGTH_SHORT).show();
            }
            else if(intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA))
            {
                /*
                 * type : 0=Normal scan (Press Trigger Key to receive the data) ; 1=Inventory EPC ; 2=Inventory ECP TID ; 3=Reader tag ; 5=Write tag ; 6=Lock tag ; 7=Kill tag ; 8=Authenticate tag ; 9=Untraceable tag
                 * response : 0=RESPONSE_OPERATION_SUCCESS ; 1=RESPONSE_OPERATION_FINISH ; 2=RESPONSE_OPERATION_TIMEOUT_FAIL ; 6=RESPONSE_PASSWORD_FAIL ; 7=RESPONSE_OPERATION_FAIL ;251=DEVICE_BUSY
                 * */

                int type = intent.getIntExtra(GeneralString.EXTRA_DATA_TYPE, -1);
                int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
                double data_rssi = intent.getDoubleExtra(GeneralString.EXTRA_DATA_RSSI, 0);

                String PC = intent.getStringExtra(GeneralString.EXTRA_PC);
                String EPC = intent.getStringExtra(GeneralString.EXTRA_EPC);
                String TID = intent.getStringExtra(GeneralString.EXTRA_TID);
                String ReadData = intent.getStringExtra(GeneralString.EXTRA_ReadData);
                int EPC_length = intent.getIntExtra(GeneralString.EXTRA_EPC_LENGTH, 0);
                int TID_length = intent.getIntExtra(GeneralString.EXTRA_TID_LENGTH, 0);
                int ReadData_length = intent.getIntExtra(GeneralString.EXTRA_ReadData_LENGTH, 0);

                String Data = "response = " + response + " , EPC = " + EPC + "\r TID = " + TID;

                tv1.setText(Data);
                Log.w(TAG, "++++ [Intent_RFIDSERVICE_TAG_DATA] ++++");
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] type=" + type + ", response=" + response + ", data_rssi="+data_rssi   );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] PC=" + PC );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] EPC=" + EPC );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] EPC_length=" + EPC_length );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] TID=" + TID );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] TID_length=" + TID_length );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] ReadData=" + ReadData );
                Log.d(TAG, "[Intent_RFIDSERVICE_TAG_DATA] ReadData_length=" + ReadData_length );

                // If type=8 ; Authenticate response data in ReadData
				/*if(type==GeneralString.TYPE_AUTHENTICATE_TAG && response==GeneralString.RESPONSE_OPERATION_SUCCESS)
				{
					Log.i(TAG, "Authenticate response data=" + ReadData );
				}*/
            }

            //Intent_RFIDSERVICE_EVENT
            else if(intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_EVENT))
            {
                int event  = intent.getIntExtra(GeneralString.EXTRA_EVENT_MASK, -1);
                Log.d(TAG, "[Intent_RFIDSERVICE_EVENT] DeviceEvent=" + event );
                if(event == DeviceEvent.LowBattery.getValue())
                {
                    Log.i(GeneralString.TAG, "LowBattery " );
                }
                else if(event == DeviceEvent.PowerSavingMode.getValue() )
                {
                    Log.i(GeneralString.TAG, "PowerSavingMode " );
                }
                else if(event == DeviceEvent.OverTemperature.getValue())
                {
                    Log.i(GeneralString.TAG, "OverTemperature " );

                }
                else if(event == DeviceEvent.ScannerFailure.getValue())
                {
                    Log.i(GeneralString.TAG, "ScannerFailure " );
                }

            }
            else if(intent.getAction().equals(GeneralString.Intent_FWUpdate_ErrorMessage))
            {
				/*String mse = "";
				mse = intent.getStringExtra(GeneralString.FWUpdate_ErrorMessage);
				int errorcode = intent.getIntExtra(GeneralString.FWUpdate_ErrorCode,-1);
				if(mse!=null)
				{
					Log.d(TAG,  "FWUpdate Error : " + mse  + "(" + errorcode+")");
					Toast.makeText(MainActivity.this,  mse, Toast.LENGTH_SHORT).show();

					if(errorcode==FWUpdateErrorCode.SameVersion.getValue())
					{
						Log.d(TAG,  "SameVersion");
					}
				}
				Log.d(TAG,  "Intent_FWUpdate_ErrorMessage" );*/
            }
            else if(intent.getAction().equals(GeneralString.Intent_FWUpdate_Percent))
            {
                int i = intent.getIntExtra(GeneralString.FWUpdate_Percent,0);
                if(i>=0)
                {
                    tv1.setText( Integer.toString(i));
                }
                Log.d(TAG,  "Intent_FWUpdate_Percent" );
            }
            else if(intent.getAction().equals(GeneralString.Intent_FWUpdate_Finish))
            {
                Log.d(TAG,  "Intent_FWUpdate_Finish" );
            }
            else if(intent.getAction().equals(GeneralString.Intent_GUN_Attached))
            {
                Log.d(TAG,  "Intent_GUN_Attached" );
            }
            else if(intent.getAction().equals(GeneralString.Intent_GUN_Unattached))
            {
                Log.d(TAG,  "Intent_GUN_Unattached" );
            }
            else if(intent.getAction().equals(GeneralString.Intent_GUN_Power))
            {
                Log.d(TAG,  "Intent_GUN_Power" );
                boolean AC = intent.getBooleanExtra(GeneralString.Data_GUN_ACPower, false);
                boolean Connect = intent.getBooleanExtra(GeneralString.Data_GUN_Connect, false);
            }

        }
    };





    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myDataReceiver);
    }
}