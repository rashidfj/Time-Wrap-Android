package com.peek.time.wrap.scan.timewrap;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.channguyen.rsv.RangeSliderView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.peek.time.wrap.scan.timewrap.activities.SplashActivity;
import com.peek.time.wrap.scan.timewrap.helpers.CameraScreen;
import com.peek.time.wrap.scan.timewrap.helpers.Constant;
import com.peek.time.wrap.scan.timewrap.helpers.ScanLine;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private CameraScreen cameraView;
    private ScanLine scanningView;
    private RangeSliderView rangeSliderView;
    private ImageView ivTimerSlider;
    final Handler handler = new Handler();
    private RangeSliderView.OnSlideListener listener;
    private ImageView switchCamera;
    private TextView startStopTextView;
    private int cameraPosition = 1;
    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        changeListeners();
        clickListeners();
    }

    private void initViews() {
//        cameraView = findViewById(R.id.camera_view);

        cameraView  = new CameraScreen(this,cameraPosition);
        scanningView = findViewById(R.id.scanningView);
        rangeSliderView = findViewById(R.id.rangeID);
        ivTimerSlider = findViewById(R.id.iv_timer_slider);
        switchCamera = findViewById(R.id.switch_camera);
        startStopTextView = findViewById(R.id.start_stop_text);
        frameLayout = findViewById(R.id.frame_container);
        frameLayout.addView(cameraView);
    }

    private void changeListeners() {
        listener = index -> {
            Constant.speed = (index + 1) * 2;
            handler.postDelayed(() -> {
                rangeSliderView.setVisibility(View.GONE);
            }, 3000);
        };
    }

    private void clickListeners() {
        rangeSliderView.setOnSlideListener(listener);
        ivTimerSlider.setOnClickListener(view -> rangeSliderView.setVisibility(View.VISIBLE));
        switchCamera.setOnClickListener(view -> {
            if (cameraPosition == 0){
                cameraPosition = 1;
            }else {
                cameraPosition = 0;
            }

            frameLayout.post(() -> {
                frameLayout.removeAllViews();
                cameraView  = new CameraScreen(MainActivity.this,cameraPosition);
                frameLayout.addView(cameraView);
            });

        });
        startStopTextView.setOnClickListener(view -> {
            startScanning();
        });
    }


    private void startScanning() {
        Dexter.withContext(MainActivity.this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            cameraView.setScanVideo(!cameraView.isScanVideo());
                            if (cameraView.isScanVideo()) {
                                scanningView.startAnimation();
                                startStopTextView.setText("Stop");
                            } else {
                                scanningView.stopAnimation();
                                startStopTextView.setText("Start");
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }

                }).onSameThread().check();
    }
}
