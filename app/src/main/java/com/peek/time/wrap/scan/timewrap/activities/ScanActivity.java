package com.peek.time.wrap.scan.timewrap.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.PointerIconCompat;
import androidx.lifecycle.LifecycleOwner;

import com.github.channguyen.rsv.RangeSliderView;
import com.google.common.util.concurrent.ListenableFuture;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.peek.time.wrap.scan.timewrap.R;
import com.peek.time.wrap.scan.timewrap.ads.Int_AD_TWS;
import com.peek.time.wrap.scan.timewrap.databinding.ActivityScanBinding;
import com.peek.time.wrap.scan.timewrap.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


public class ScanActivity extends AppCompatActivity {
    final Handler handlerTWS = new Handler();
    private RangeSliderView.OnSlideListener listener;
    private ListenableFuture<ProcessCameraProvider> cameraFutureTWS;
    ImageAnalysis imageAnalysis;
    private final int REQUEST_CODE_PERMISSIONS_TWS = PointerIconCompat.TYPE_CONTEXT_MENU;
    private final String[] REQUIRED_PERMISSIONS = {"android.permission.CAMERA"};
    ProcessCameraProvider camProvTWS;
    CameraSelector camSelTWS;
    boolean capture = false;
    int facingTWS = 0;
    int frRateTWS = 30;
    boolean isSwitchingTWS = false;
    int lCntTWS = 0;
    int lResTWS = 2;
    Camera mCamera;
    Preview mPreview;
    int resXTWS = 360;
    int resYTWS = 640;
    Bitmap resBmpTWS = null;
    List<Bitmap> resBmpListTWS = null;
    Bitmap subBmpTWS = null;
    public WRP_DIR_TWS directionTWS = WRP_DIR_TWS.DOWN;
    private String direction = "horizontal";
    private int brightness = 0;
    private boolean isSeekBarTracking = false;


    public enum WRP_DIR_TWS {
        DOWN,
        RIGHT
    }


    ActivityScanBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initItems();
        changeListeners();
        clickListeners();
    }

    private void initItems() {
        Utils.makeDir(ScanActivity.this);
        binding.peekIdPreview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        cameraFutureTWS = ProcessCameraProvider.getInstance(this);
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(360, 640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImgCap());
        cameraFutureTWS.addListener(ScanActivity.this::cameraPermission, ContextCompat.getMainExecutor(this));

        if (direction.equals("horizontal")) {
            binding.horizontalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.selected_ort_bg)));
            binding.verticalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg)));

        } else if (direction.equals("vertical")) {
            binding.horizontalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg)));
            binding.verticalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.selected_ort_bg)));
        }

        brightness = Settings.System.getInt(ScanActivity.this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);

    }


    private void changeListeners() {
        listener = index -> {
            frRateTWS = 120 - ((index + 1) * 20);
            handlerTWS.postDelayed(() -> {
                binding.rangeID.setVisibility(View.INVISIBLE);
            }, 3000);
        };


        binding.seekBar.setProgress(brightness);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarTracking = false;
                handlerTWS.postDelayed(() -> {
                    if (!isSeekBarTracking) {
                        binding.seekBarContainer.setVisibility(View.INVISIBLE);
                    }
                }, 3000);

            }
        });
    }

    private void clickListeners() {
        binding.rangeID.setOnSlideListener(listener);
        binding.peekIdSubToolbar.back.setOnClickListener(view -> onBackPressed());
        binding.peekIdSubToolbar.ivTimerSlider.setOnClickListener(view -> binding.rangeID.setVisibility(View.VISIBLE));
        binding.peekIdSubToolbar.ivBrightness.setOnClickListener(view -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (Settings.System.canWrite(this)) {
                    binding.seekBarContainer.setVisibility(View.VISIBLE);
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }


            }

        });

        binding.startStopText.setOnClickListener(view -> {
            binding.rangeID.setVisibility(View.INVISIBLE);
            startScanning();
        });

        binding.verticalBtn.setOnClickListener(view -> {
            binding.horizontalBtn.setTextColor(getResources().getColor(R.color.yellow));
            binding.verticalBtn.setTextColor(getResources().getColor(R.color.black));
            binding.horizontalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg)));
            binding.verticalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.selected_ort_bg)));
            direction = "vertical";
        });

        binding.horizontalBtn.setOnClickListener(view -> {
            binding.horizontalBtn.setTextColor(getResources().getColor(R.color.black));
            binding.verticalBtn.setTextColor(getResources().getColor(R.color.yellow));
            binding.horizontalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.selected_ort_bg)));
            binding.verticalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg)));
            direction = "horizontal";
        });

        binding.peekIdSavedImages.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SavedImagesActivity.class);
            startActivity(intent);
        });

        binding.switchCamera.setOnClickListener(ScanActivity.this::CameraSwitching);
        binding.peekIdResultCancel.setOnClickListener(ScanActivity.this::cancelButtonFunctions);

        binding.peekIdResultSave.setOnClickListener(view -> {
            String fileTitle = System.currentTimeMillis() + ".jpeg";
            saveToInternalStorage(resBmpTWS, fileTitle);
            Toast.makeText(ScanActivity.this, "Photo Saved", Toast.LENGTH_SHORT).show();
            cancelButtonFunctions(view);
            Int_AD_TWS.getInstance(this).showAd_WA(ScanActivity.this);
        });
        this.resBmpListTWS = new ArrayList();
        this.resYTWS = 640;
        this.resXTWS = 360;
    }


    private void startScanning() {
        Dexter.withContext(ScanActivity.this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {

                            if (binding.peekIdBottomLl.getVisibility() == View.VISIBLE) {
                                if (direction.equals("horizontal")) {
                                    try {
                                        binding.horizontalBtn.setTextColor(getResources().getColor(R.color.black));
                                        binding.verticalBtn.setTextColor(getResources().getColor(R.color.yellow));
                                        binding.horizontalBtn.setBackground(getTintedDrawable(getResources(), R.drawable.selected_ort_bg));
                                        binding.verticalBtn.setBackground(Objects.requireNonNull(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg)));
                                        startCapture(WRP_DIR_TWS.RIGHT);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else if (direction.equals("vertical")) {
                                    try {
                                        binding.horizontalBtn.setTextColor(getResources().getColor(R.color.yellow));
                                        binding.verticalBtn.setTextColor(getResources().getColor(R.color.black));
                                        binding.verticalBtn.setBackground(getTintedDrawable(getResources(), R.drawable.selected_ort_bg));
                                        binding.horizontalBtn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg));

                                        startCapture(WRP_DIR_TWS.DOWN);

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }

                }).onSameThread().check();
    }


    private String saveToInternalStorage(Bitmap bitmapImage, String fileTitle) {
        File directory = new File(ScanActivity.this.getExternalFilesDir("Photos"), getString(R.string.app_name));
        // Create imageDir
        if (!directory.exists()) {
            directory.mkdir();
        }
        File mypath = new File(directory, fileTitle);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert fos != null;
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    public void cameraPermission() {
        try {
            this.camProvTWS = (ProcessCameraProvider) this.cameraFutureTWS.get();
            if (allPermissionsGranted()) {
                bindPreview(this.camProvTWS);
            } else {
                ActivityCompat.requestPermissions(this, this.REQUIRED_PERMISSIONS, this.REQUEST_CODE_PERMISSIONS_TWS);
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }


    public void cancelButtonFunctions(View view) {
        resumeToBeforeCaptureUI();
    }

    public void CameraSwitching(View view) {
        this.isSwitchingTWS = true;
        if (this.facingTWS == 0) {
            setFacing(1);
        } else {
            setFacing(0);
        }
        bindPreview(this.camProvTWS);
        this.isSwitchingTWS = false;
    }

    private boolean allPermissionsGranted() {
        for (String checkSelfPermission : this.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, checkSelfPermission) != 0) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == this.REQUEST_CODE_PERMISSIONS_TWS) {
            if (allPermissionsGranted()) {
                bindPreview(this.camProvTWS);
                return;
            }
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setFacing(int i) {
        this.facingTWS = i;
    }

    public Bitmap overlay(Bitmap bitmap, Bitmap bitmap2, int i, WRP_DIR_TWS warp_direction) {
        new Matrix().preScale(1.0f, -1.0f);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        canvas.drawBitmap(bitmap, new Matrix(), (Paint) null);
        if (warp_direction == WRP_DIR_TWS.DOWN) {
            canvas.drawBitmap(bitmap2, 0.0f, (float) i, (Paint) null);
        }
        if (warp_direction == WRP_DIR_TWS.RIGHT) {
            canvas.drawBitmap(bitmap2, (float) i, 0.0f, (Paint) null);
        }
        return createBitmap;
    }


    public static Bitmap MirrorBitmap(Bitmap bitmap, int i, int i2) {
        Matrix matrix = new Matrix();
        matrix.preScale((float) i, (float) i2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int i) {
        Matrix matrix = new Matrix();
        matrix.setRotate((float) i);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }


    public void drawScanEffect(Bitmap bitmap, WRP_DIR_TWS warp_direction, int i) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(5.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paint.setColor(getColor(R.color.lineColor));
        } else {
            paint.setColor(-1);
        }
        if (warp_direction == WRP_DIR_TWS.DOWN) {
            float f = (float) (i + 5);
            canvas.drawLine(0.0f, f, (float) bitmap.getWidth(), f, paint);
        } else if (warp_direction == WRP_DIR_TWS.RIGHT) {
            float f2 = (float) (i + 5);
            canvas.drawLine(f2, 0.0f, f2, (float) bitmap.getHeight(), paint);
        }
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }


    public void initializeImageView() {
        Bitmap createBitmap = Bitmap.createBitmap(this.resXTWS, this.resYTWS, Bitmap.Config.ARGB_8888);
        this.resBmpTWS = createBitmap;
        createBitmap.eraseColor(0);
        binding.peekIdResultImageview.setImageBitmap(this.resBmpTWS);
    }


    private void showBeforeCaptureUI() {
        binding.peekIdBottomLl.setVisibility(View.VISIBLE);
        binding.peekIdRlToolbar.setVisibility(View.VISIBLE);
    }

    private void hideBeforeCaptureUI() {
        binding.peekIdBottomLl.setVisibility(View.INVISIBLE);
        binding.peekIdRlToolbar.setVisibility(View.INVISIBLE);
    }


    private void showResultUI() {
        binding.peekIdResultLl.setVisibility(View.VISIBLE);
    }

    private void hideResultUI() {
        binding.peekIdResultLl.setVisibility(View.INVISIBLE);
    }


    public Drawable getTintedDrawable(Resources resources, int i) {
        @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable = resources.getDrawable(i);
        return drawable;
    }


    private void resumeToBeforeCaptureUI() {
        this.lCntTWS = 0;
        this.resBmpTWS = null;
        this.resBmpListTWS = null;
        initializeImageView();
        hideResultUI();
        showBeforeCaptureUI();
        this.capture = false;
    }


    public void startCapture(WRP_DIR_TWS warp_direction) throws InterruptedException {
        this.directionTWS = warp_direction;
        this.lResTWS = 2;
        this.lCntTWS = 0;
        this.resBmpTWS = null;
        this.resBmpListTWS = null;
        initializeImageView();
        hideResultUI();
        hideBeforeCaptureUI();
        this.capture = true;
    }


    public Bitmap toBitmapTWS(Image image) {
        Image.Plane[] pl = image.getPlanes();
        ByteBuffer bf = pl[0].getBuffer();
        ByteBuffer bf2 = pl[1].getBuffer();
        ByteBuffer bf3 = pl[2].getBuffer();
        int remaining = bf.remaining();
        int remaining2 = bf2.remaining();
        int remaining3 = bf3.remaining();
        byte[] bArrTSW = new byte[(remaining + remaining2 + remaining3)];
        bf.get(bArrTSW, 0, remaining);
        bf3.get(bArrTSW, remaining, remaining3);
        bf2.get(bArrTSW, remaining + remaining3, remaining2);
        YuvImage yuvImage = new YuvImage(bArrTSW, 17, image.getWidth(), image.getHeight(), (int[]) null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }


    public void bindPreview(ProcessCameraProvider processCameraProvider) {
        try {
            processCameraProvider.unbindAll();
            this.mPreview = new Preview.Builder().setTargetResolution(new Size(360, 640)).build();
            this.camSelTWS = new CameraSelector.Builder().requireLensFacing(this.facingTWS).build();
            ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(360, 640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
            this.imageAnalysis = build;
            build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImgCap());
            this.mPreview.setSurfaceProvider(binding.peekIdPreview.getSurfaceProvider());
            this.mCamera = processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.camSelTWS, this.mPreview);
            processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.camSelTWS, this.imageAnalysis, this.mPreview);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    public class ImgCap implements ImageAnalysis.Analyzer {
        private ImgCap() {
        }

        @SuppressLint("UnsafeOptInUsageError")
        public void analyze(ImageProxy imageProxy) {
            try {
                Bitmap bitmap;
                if (binding.peekIdPreview.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING && binding.peekIdPreview.getChildAt(0).getClass() == TextureView.class) {
                    bitmap = ((TextureView) binding.peekIdPreview.getChildAt(0)).getBitmap(ScanActivity.this.resXTWS, ScanActivity.this.resYTWS);
                } else if (imageProxy.getFormat() == 35) {
                    ScanActivity scanActivity = ScanActivity.this;
                    bitmap = scanActivity.rotateBitmap(scanActivity.toBitmapTWS(imageProxy.getImage()), 90);
                    if (ScanActivity.this.facingTWS == 0) {
                        bitmap = ScanActivity.MirrorBitmap(bitmap, 1, -1);
                    }
                } else {
                    bitmap = null;
                }
                if (bitmap == null) {
                    imageProxy.close();
                    return;
                }
                if ((ScanActivity.this.lCntTWS >= ScanActivity.this.resYTWS || ScanActivity.this.directionTWS != WRP_DIR_TWS.DOWN) && !((ScanActivity.this.lCntTWS < ScanActivity.this.resXTWS && ScanActivity.this.directionTWS == WRP_DIR_TWS.RIGHT && ScanActivity.this.facingTWS == 0) || (ScanActivity.this.lCntTWS < ScanActivity.this.resXTWS && ScanActivity.this.directionTWS == WRP_DIR_TWS.RIGHT && ScanActivity.this.facingTWS == 1))) {
                    if (ScanActivity.this.capture) {
                        ScanActivity.this.stopCapture();
                    }
                } else if (ScanActivity.this.capture) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (ScanActivity.this.resBmpTWS == null) {
                        ScanActivity.this.initializeImageView();
                    }
                    if (ScanActivity.this.resBmpListTWS == null) {
                        ScanActivity.this.resBmpListTWS = new ArrayList();
                    }
                    if (ScanActivity.this.directionTWS == WRP_DIR_TWS.DOWN) {
                        ScanActivity scanActivity3 = ScanActivity.this;
                        scanActivity3.subBmpTWS = Bitmap.createBitmap(bitmap, 0, scanActivity3.lCntTWS, ScanActivity.this.resXTWS, ScanActivity.this.lResTWS);
                    } else if (ScanActivity.this.directionTWS == WRP_DIR_TWS.RIGHT) {
                        ScanActivity scanActivity4 = ScanActivity.this;
                        scanActivity4.subBmpTWS = Bitmap.createBitmap(bitmap, scanActivity4.lCntTWS, 0, ScanActivity.this.lResTWS, ScanActivity.this.resYTWS);
                    }
                    ScanActivity scanActivity5 = ScanActivity.this;
                    scanActivity5.resBmpTWS = scanActivity5.overlay(scanActivity5.resBmpTWS, ScanActivity.this.subBmpTWS, ScanActivity.this.lCntTWS, ScanActivity.this.directionTWS);
                    binding.peekIdResultImageview.setImageBitmap(ScanActivity.this.resBmpTWS);
                    ScanActivity scanActivity8 = ScanActivity.this;
                    scanActivity8.drawScanEffect(bitmap, scanActivity8.directionTWS, ScanActivity.this.lCntTWS);
                    ScanActivity.this.lCntTWS += ScanActivity.this.lResTWS;
                    long currentTimeMillis2 = currentTimeMillis - System.currentTimeMillis();
                    if (currentTimeMillis2 < ((long) ScanActivity.this.frRateTWS)) {
                        try {
                            Thread.sleep(((long) ScanActivity.this.frRateTWS) - currentTimeMillis2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                binding.peekIdPreviewImageview.setImageBitmap(bitmap);
                imageProxy.close();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }


    public void stopCapture() {
        try {
            if ((this.lCntTWS == this.resYTWS && this.directionTWS == WRP_DIR_TWS.DOWN) || (this.lCntTWS == this.resXTWS && this.directionTWS == WRP_DIR_TWS.RIGHT)) {
                showResultUI();
            }
            this.capture = false;
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void onBackPressed() {
        try {
            if (binding.peekIdBottomLl.getVisibility() == View.VISIBLE) {
                finish();
            } else {
                resumeToBeforeCaptureUI();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }


}






