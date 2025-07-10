package com.smewise.rfidtestapi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cipherlab.rfid.ClResult;
import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RFIDMemoryBank;
import com.cipherlab.rfidapi.RfidManager;

//Deprecated
public class SelectedMemoryBank extends AppCompatActivity {
    RfidManager mRfidManager = null;
    TextView tv1 = null;
    Button b1, b2, b3, b4, b5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selected_memory_bank);
        getWindow().setStatusBarColor(Color.GRAY);
        getSupportActionBar().setTitle("SelectedMemoryBank");

        mRfidManager = RfidManager.InitInstance(this);

        tv1 = findViewById(R.id.textView1);
        b1 = findViewById(R.id.bt01);
        b2 = findViewById(R.id.bt02);
        b3 = findViewById(R.id.bt03);
        b4 = findViewById(R.id.bt04);
        b5 = findViewById(R.id.bt05);

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

        // Set button click listeners
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMemoryBank(RFIDMemoryBank.Reserved);
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMemoryBank(RFIDMemoryBank.EPC);
            }
        });

        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMemoryBank(RFIDMemoryBank.TID);
            }
        });

        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMemoryBank(RFIDMemoryBank.User);
            }
        });

        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSelectedMemoryBank();
            }
        });
    }

    // Select the memory bank and display
    private void selectMemoryBank(RFIDMemoryBank bank) {
        int result = mRfidManager.SelectMemoryBank(bank);
        if (result != ClResult.S_OK.ordinal()) {
            String error = mRfidManager.GetLastError();
            tv1.setText( "Error selecting memory bank: " + error);
        } else {
            String memoryBankName = getMemoryBankName(bank);
            tv1.setText(memoryBankName + " selected successfully!");
        }
    }

    // Get and display the currently selected memory bank
    private void getSelectedMemoryBank() {
        RFIDMemoryBank selectedBank = mRfidManager.GetSelectedMemoryBank();
        if (selectedBank == RFIDMemoryBank.Err) {
            String error = mRfidManager.GetLastError();
            tv1.setText("Error: " + error);
        } else {
            String bankName = getMemoryBankName(selectedBank);
            tv1.setText("Selected Memory Bank: " + bankName);
        }
    }

    // Helper method to convert RFIDMemoryBank enum to String
    private String getMemoryBankName(RFIDMemoryBank bank) {
        switch (bank) {
            case EPC:
                return "EPC";
            case TID:
                return "TID";
            case Reserved:
                return "Reserved";
            case User:
                return "User";
            default:
                return "Unknown";
        }
    }
}