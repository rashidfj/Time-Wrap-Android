package com.peek.time.wrap.scan.timewrap.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.util.Map;


@SuppressLint("StaticFieldLeak")
public class Application extends android.app.Application implements LifecycleObserver {


    public static Application application_TWS;
    public static Context mContext_TWS;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mContext_TWS = getApplicationContext();
            application_TWS = this;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("L");
            }
            try {

                MobileAds.initialize(this, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {
                        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
                        for (String adapterClass : statusMap.keySet()) {
                            AdapterStatus status = statusMap.get(adapterClass);
                            Log.d("MyApp", String.format(
                                    "Adapter name: %s, Description: %s, Latency: %d",
                                    adapterClass, status.getDescription(), status.getLatency()));
                        }

                        // Start loading ads here...

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {

        // Log.d("MyApp", "App in background");

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {

        // Log.d("MyApp", "App in foreground");

    }


    public Application() {
        application_TWS = this;
    }

    public static synchronized Application getInstance() {
        return application_TWS;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    public static Application getApp() {
        if (application_TWS == null) {
            application_TWS = new Application();
        }
        return application_TWS;
    }


}