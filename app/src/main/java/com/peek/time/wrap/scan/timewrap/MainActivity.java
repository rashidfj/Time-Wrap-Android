package com.peek.time.wrap.scan.timewrap;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.peek.time.wrap.scan.timewrap.helpers.CameraScreen;
import com.peek.time.wrap.scan.timewrap.helpers.ScanLine;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private CameraScreen cameraView;
    private ScanLine scanningView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera_view);
        scanningView = findViewById(R.id.scanningView);

    }

    public void onScan(View view) {

        Dexter.withContext(MainActivity.this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            cameraView.setScanVideo(!cameraView.isScanVideo());
                            TextView startStopText = (TextView) view;
                            if (cameraView.isScanVideo()) {
                                scanningView.startAnimation();
                                startStopText.setText("Stop");
                            } else {
                                scanningView.stopAnimation();
                                startStopText.setText("Start");
                            }
                        }
//                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
//
//
//                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }

                }).onSameThread().check();


    }
}
