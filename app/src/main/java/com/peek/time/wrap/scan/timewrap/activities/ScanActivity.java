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
    final Handler handler = new Handler();
    private RangeSliderView.OnSlideListener listener;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    ImageAnalysis imageAnalysis;
    private final int REQUEST_CODE_PERMISSIONS = PointerIconCompat.TYPE_CONTEXT_MENU;
    private final String[] REQUIRED_PERMISSIONS = {"android.permission.CAMERA"};
    ProcessCameraProvider cameraProvider;
    CameraSelector cameraSelector;
    boolean capture = false;
    int facing = 0;
    int frameRate = 30;
    boolean isSwitching = false;
    int lineCount = 0;
    int lineResolution = 2;
    Camera mCamera;
    Preview preview;
    int resolutionX = 360;
    int resolutionY = 640;
    Bitmap resultBitmap = null;
    List<Bitmap> resultBitmapList = null;
    Bitmap subBitmap = null;
    public WARP_DIRECTION warpDirection = WARP_DIRECTION.DOWN;
    private String direction = "horizontal";
    private int brightness = 0;
    private boolean isSeekBarTracking = false;


    public enum WARP_DIRECTION {
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
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(360, 640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImgCap());
        cameraProviderFuture.addListener(ScanActivity.this::cameraPermission, ContextCompat.getMainExecutor(this));

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
            frameRate = 120 - ((index + 1) * 20);
            handler.postDelayed(() -> {
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
                handler.postDelayed(() -> {
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
                }else {
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
            saveToInternalStorage(resultBitmap, fileTitle);
            Toast.makeText(ScanActivity.this, "Photo Saved", Toast.LENGTH_SHORT).show();
            cancelButtonFunctions(view);
        });
        this.resultBitmapList = new ArrayList();
        this.resolutionY = 640;
        this.resolutionX = 360;
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
                                        startCapture(WARP_DIRECTION.RIGHT);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else if (direction.equals("vertical")) {
                                    try {
                                        binding.horizontalBtn.setTextColor(getResources().getColor(R.color.yellow));
                                        binding.verticalBtn.setTextColor(getResources().getColor(R.color.black));
                                        binding.verticalBtn.setBackground(getTintedDrawable(getResources(), R.drawable.selected_ort_bg));
                                        binding.horizontalBtn.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.unselected_ort_bg));

                                        startCapture(WARP_DIRECTION.DOWN);

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
            this.cameraProvider = (ProcessCameraProvider) this.cameraProviderFuture.get();
            if (allPermissionsGranted()) {
                bindPreview(this.cameraProvider);
            } else {
                ActivityCompat.requestPermissions(this, this.REQUIRED_PERMISSIONS, this.REQUEST_CODE_PERMISSIONS);
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }


    public void cancelButtonFunctions(View view) {
        resumeToBeforeCaptureUI();
    }

    public void CameraSwitching(View view) {
        this.isSwitching = true;
        if (this.facing == 0) {
            setFacing(1);
        } else {
            setFacing(0);
        }
        bindPreview(this.cameraProvider);
        this.isSwitching = false;
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
        if (i == this.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                bindPreview(this.cameraProvider);
                return;
            }
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setFacing(int i) {
        this.facing = i;
    }

    public Bitmap overlay(Bitmap bitmap, Bitmap bitmap2, int i, WARP_DIRECTION warp_direction) {
        new Matrix().preScale(1.0f, -1.0f);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        canvas.drawBitmap(bitmap, new Matrix(), (Paint) null);
        if (warp_direction == WARP_DIRECTION.DOWN) {
            canvas.drawBitmap(bitmap2, 0.0f, (float) i, (Paint) null);
        }
        if (warp_direction == WARP_DIRECTION.RIGHT) {
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


    public void drawScanEffect(Bitmap bitmap, WARP_DIRECTION warp_direction, int i) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(5.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paint.setColor(getColor(R.color.yellow));
        } else {
            paint.setColor(-1);
        }
        if (warp_direction == WARP_DIRECTION.DOWN) {
            float f = (float) (i + 5);
            canvas.drawLine(0.0f, f, (float) bitmap.getWidth(), f, paint);
        } else if (warp_direction == WARP_DIRECTION.RIGHT) {
            float f2 = (float) (i + 5);
            canvas.drawLine(f2, 0.0f, f2, (float) bitmap.getHeight(), paint);
        }
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }


    public void initializeImageView() {
        Bitmap createBitmap = Bitmap.createBitmap(this.resolutionX, this.resolutionY, Bitmap.Config.ARGB_8888);
        this.resultBitmap = createBitmap;
        createBitmap.eraseColor(0);
        binding.peekIdResultImageview.setImageBitmap(this.resultBitmap);
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
        this.lineCount = 0;
        this.resultBitmap = null;
        this.resultBitmapList = null;
        initializeImageView();
        hideResultUI();
        showBeforeCaptureUI();
        this.capture = false;
    }


    public void startCapture(WARP_DIRECTION warp_direction) throws InterruptedException {
        this.warpDirection = warp_direction;
        this.lineResolution = 2;
        this.lineCount = 0;
        this.resultBitmap = null;
        this.resultBitmapList = null;
        initializeImageView();
        hideResultUI();
        hideBeforeCaptureUI();
        this.capture = true;
    }


    public Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        ByteBuffer buffer2 = planes[1].getBuffer();
        ByteBuffer buffer3 = planes[2].getBuffer();
        int remaining = buffer.remaining();
        int remaining2 = buffer2.remaining();
        int remaining3 = buffer3.remaining();
        byte[] bArr = new byte[(remaining + remaining2 + remaining3)];
        buffer.get(bArr, 0, remaining);
        buffer3.get(bArr, remaining, remaining3);
        buffer2.get(bArr, remaining + remaining3, remaining2);
        YuvImage yuvImage = new YuvImage(bArr, 17, image.getWidth(), image.getHeight(), (int[]) null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }


    public void bindPreview(ProcessCameraProvider processCameraProvider) {
        processCameraProvider.unbindAll();
        this.preview = new Preview.Builder().setTargetResolution(new Size(360, 640)).build();
        this.cameraSelector = new CameraSelector.Builder().requireLensFacing(this.facing).build();
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(360, 640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        this.imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImgCap());
        this.preview.setSurfaceProvider(binding.peekIdPreview.getSurfaceProvider());
        this.mCamera = processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.preview);
        processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.imageAnalysis, this.preview);
    }


    public class ImgCap implements ImageAnalysis.Analyzer {
        private ImgCap() {
        }

        @SuppressLint("UnsafeOptInUsageError")
        public void analyze(ImageProxy imageProxy) {
            Bitmap bitmap;
            if (binding.peekIdPreview.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING && binding.peekIdPreview.getChildAt(0).getClass() == TextureView.class) {
                bitmap = ((TextureView) binding.peekIdPreview.getChildAt(0)).getBitmap(ScanActivity.this.resolutionX, ScanActivity.this.resolutionY);
            } else if (imageProxy.getFormat() == 35) {
                ScanActivity scanActivity = ScanActivity.this;
                bitmap = scanActivity.rotateBitmap(scanActivity.toBitmap(imageProxy.getImage()), 90);
                if (ScanActivity.this.facing == 0) {
                    bitmap = ScanActivity.MirrorBitmap(bitmap, 1, -1);
                }
            } else {
                bitmap = null;
            }
            if (bitmap == null) {
                imageProxy.close();
                return;
            }
            if ((ScanActivity.this.lineCount >= ScanActivity.this.resolutionY || ScanActivity.this.warpDirection != WARP_DIRECTION.DOWN) && !((ScanActivity.this.lineCount < ScanActivity.this.resolutionX && ScanActivity.this.warpDirection == WARP_DIRECTION.RIGHT && ScanActivity.this.facing == 0) || (ScanActivity.this.lineCount < ScanActivity.this.resolutionX && ScanActivity.this.warpDirection == WARP_DIRECTION.RIGHT && ScanActivity.this.facing == 1))) {
                if (ScanActivity.this.capture) {
                    ScanActivity.this.stopCapture();
                }
            } else if (ScanActivity.this.capture) {
                long currentTimeMillis = System.currentTimeMillis();
                if (ScanActivity.this.resultBitmap == null) {
                    ScanActivity.this.initializeImageView();
                }
                if (ScanActivity.this.resultBitmapList == null) {
                    ScanActivity.this.resultBitmapList = new ArrayList();
                }
                if (ScanActivity.this.warpDirection == WARP_DIRECTION.DOWN) {
                    ScanActivity scanActivity3 = ScanActivity.this;
                    scanActivity3.subBitmap = Bitmap.createBitmap(bitmap, 0, scanActivity3.lineCount, ScanActivity.this.resolutionX, ScanActivity.this.lineResolution);
                } else if (ScanActivity.this.warpDirection == WARP_DIRECTION.RIGHT) {
                    ScanActivity scanActivity4 = ScanActivity.this;
                    scanActivity4.subBitmap = Bitmap.createBitmap(bitmap, scanActivity4.lineCount, 0, ScanActivity.this.lineResolution, ScanActivity.this.resolutionY);
                }
                ScanActivity scanActivity5 = ScanActivity.this;
                scanActivity5.resultBitmap = scanActivity5.overlay(scanActivity5.resultBitmap, ScanActivity.this.subBitmap, ScanActivity.this.lineCount, ScanActivity.this.warpDirection);
                binding.peekIdResultImageview.setImageBitmap(ScanActivity.this.resultBitmap);
                ScanActivity scanActivity8 = ScanActivity.this;
                scanActivity8.drawScanEffect(bitmap, scanActivity8.warpDirection, ScanActivity.this.lineCount);
                ScanActivity.this.lineCount += ScanActivity.this.lineResolution;
                long currentTimeMillis2 = currentTimeMillis - System.currentTimeMillis();
                if (currentTimeMillis2 < ((long) ScanActivity.this.frameRate)) {
                    try {
                        Thread.sleep(((long) ScanActivity.this.frameRate) - currentTimeMillis2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            binding.peekIdPreviewImageview.setImageBitmap(bitmap);
            imageProxy.close();
        }
    }


    public void stopCapture() {
        if ((this.lineCount == this.resolutionY && this.warpDirection == WARP_DIRECTION.DOWN) || (this.lineCount == this.resolutionX && this.warpDirection == WARP_DIRECTION.RIGHT)) {
            showResultUI();
        }
        this.capture = false;
    }

    public void onBackPressed() {
        if (binding.peekIdBottomLl.getVisibility() == View.VISIBLE) {
            finish();
        } else {
            resumeToBeforeCaptureUI();
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






