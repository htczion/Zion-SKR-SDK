package com.htc.wallet.skrsdk.demoapp;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.htc.wallet.skrsdk.ZionSkrSdkManager;
import com.htc.wallet.skrsdk.adapter.ApiKeyAdapter;
import com.htc.wallet.skrsdk.adapter.WalletAdapter;
import com.htc.wallet.skrsdk.demoapp.util.AppVisibilityDetector;
import com.htc.wallet.skrsdk.demoapp.util.ZKMASdkUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DemoApplication extends Application {

    private static final String TAG = "DemoApplication";

    private static final List<AppGotoBackgroundListener> sAppGotoBackgroundListeners =
            new ArrayList<>();

    public interface AppGotoBackgroundListener {
        void onAppGotoBackground();
    }

    public static void addOnAppGotoBackgroundListener(AppGotoBackgroundListener listener) {
        if (listener != null && !sAppGotoBackgroundListeners.contains(listener)) {
            sAppGotoBackgroundListeners.add(listener);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            // Debug tool
            Stetho.initialize(Stetho.newInitializerBuilder(this)
                    .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                    .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                    .build()
            );
        }

        // Init AppVisibilityDetector
        AppVisibilityDetector.init(this, new AppVisibilityDetector.AppVisibilityCallback() {
            @Override
            public void onAppGotoForeground() {
                Log.d(TAG, "Goto Foreground");
            }

            @Override
            public void onAppGotoBackground() {
                Log.d(TAG, "Goto Background");
                for (AppGotoBackgroundListener listener : sAppGotoBackgroundListeners) {
                    listener.onAppGotoBackground();
                }
            }
        });

        // Init SKR
        initSkrSdk(this);
    }

    private void initSkrSdk(Context context) {

        // WalletAdapter
        WalletAdapter walletAdapter = new WalletAdapter() {
            @Override
            public long getUniqueId(@NonNull Context context) {
                Objects.requireNonNull(context, "context is null");

                return ZKMASdkUtil.getUniqueId(context);
            }
        };

        // ApiKeyAdapter
        ApiKeyAdapter apiKeyAdapter = new ApiKeyAdapter() {
            @Override
            public String getGoogleApiKey() {
                return BuildConfig.GOOGLE_API_KEY;
            }

            @Override
            public String getGoogleClientId() {
                return BuildConfig.GOOGLE_CLIENT_ID;
            }

            @Override
            public String getGoogleClientSecret() {
                return BuildConfig.GOOGLE_CLIENT_SECRET;
            }

            @Override
            public String getTZApiKeyStage() {
                return BuildConfig.TZ_API_KEY_STAGE;
            }

            @Override
            public String getTZApiKeyProduction() {
                return BuildConfig.TZ_API_KEY_PRODUCTION;
            }

            @Override
            public String getAesKey() {
                return BuildConfig.AES_KEY_STRING;
            }

            @Override
            public String getSafetyNetApiKeyStage() {
                return BuildConfig.SAFETY_NET_API_KEY;
            }

            @Override
            public String getSafetyNetApiKeyProduction() {
                return BuildConfig.SAFETY_NET_API_KEY;
            }
        };

        ZionSkrSdkManager.Params params = new ZionSkrSdkManager.Params(context)
                .setWalletAdapter(walletAdapter)
                .setApiKeyAdapter(apiKeyAdapter)
                .setIsDebug(BuildConfig.DEBUG);

        // Init ZionSkrSdkManager
        ZionSkrSdkManager.init(params);
    }
}
