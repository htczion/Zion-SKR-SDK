package com.htc.wallet.skrsdk.demoapp;

import android.content.Intent;
import android.os.Bundle;

import com.htc.wallet.skrsdk.applink.BaseAppLinkReceiverActivity;

public class AppLinkReceiverActivity extends BaseAppLinkReceiverActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_layout);

        checkFirebaseLink();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
