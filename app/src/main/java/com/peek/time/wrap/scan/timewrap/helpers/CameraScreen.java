package com.peek.time.wrap.scan.timewrap.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;

import com.peek.time.wrap.scan.timewrap.utils.EGLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraScreen extends SurfaceView {

    private EGLUtils mEglUtils = new EGLUtils();
    private PeekVideoRenderer videoRenderer = new PeekVideoRenderer();
    private PeekRenderer mRenderer = new PeekRenderer();

    private PeekScanRenderer scanRenderer = new PeekScanRenderer();

    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private Handler mHandler;

    private int screenWidth = -1, screenHeight,previewWidth,previewHeight;
    private Rect rect = new Rect();

    private Handler cameraHandler;
    private HandlerThread cameraThread;

    private boolean isScan = false;
    private boolean isf = false;
    private float scanHeight;
    private float pixelHeight;
    private int cameraPosition = 1;
    HandlerThread handlerThread;

    public boolean isScanVideo() {
        return isScan;
    }

    public void setScanVideo(boolean scan) {
        isScan = scan;
    }

    public CameraScreen(Context context,int cameraPosition) {
        super(context);
        this.cameraPosition = cameraPosition;
        init();
    }

    public CameraScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init(){
        if (cameraThread != null) {
            videoRenderer.release();
            mRenderer.release();
            scanRenderer.release();
            mEglUtils.release();
        }
        cameraThread = new HandlerThread("Camera2Thread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        initCamera2(cameraPosition);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "test", Toast.LENGTH_SHORT).show();
                        mEglUtils.initEGL(getHolder().getSurface());
                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        mRenderer.initShader();
                        videoRenderer.initShader();
                        scanRenderer.initShader();
                        videoRenderer.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                cameraHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(mCameraCaptureSession == null){
                                            return;
                                        }
                                        try {
                                            videoRenderer.drawFrame();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        int videoTexture = videoRenderer.getTexture();
                                        if(isScan){
                                            if(!isf){
                                                scanHeight = pixelHeight*Constant.speed;
                                            }else{
                                                scanHeight += (pixelHeight*Constant.speed);
                                            }
                                            if(scanHeight < 2.0){
                                                float fh = scanHeight;
                                                if(scanHeight >= 1.0 ){
                                                    scanHeight = 3.0f;
                                                    fh = 1.0f;
                                                }
                                                Log.e("TAGsssssss", "run: "+scanHeight);
                                                scanRenderer.drawFrame(videoRenderer.getTexture(),fh);
                                            }
                                            videoTexture = scanRenderer.getTexture();
                                        }
                                        isf = isScan;
                                        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                                        GLES20.glViewport(rect.left,rect.top,rect.width(),rect.height());
                                        mRenderer.drawFrame(videoTexture);

                                        mEglUtils.swap();
                                    }
                                });
                            }
                        });

                        if(screenWidth != -1){
                            openCamera2();
                        }
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
                final int sw = screenWidth;
                screenWidth = w;
                screenHeight = h;
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Size mPreviewSize =  getPreferredPreviewSize(mSizes, screenWidth, screenHeight);
                        previewWidth = mPreviewSize.getHeight();
                        previewHeight = mPreviewSize.getWidth();
                        pixelHeight = 1.0f/previewHeight;
                        int left, top, viewWidth, viewHeight;
                        float sh = screenWidth * 1.0f / screenHeight;
                        float vh = previewWidth * 1.0f / previewHeight;
                        if (sh < vh) {
                            left = 0;
                            viewWidth = screenWidth;
                            viewHeight = (int) (previewHeight * 1.0f / previewWidth * viewWidth);
                            top = (screenHeight - viewHeight) / 2;
                        } else {
                            top = 0;
                            viewHeight = screenHeight;
                            viewWidth = (int) (previewWidth * 1.0f / previewHeight * viewHeight);
                            left = (screenWidth - viewWidth) / 2;
                        }
                        rect.left = left;
                        rect.top = top;
                        rect.right = left + viewWidth;
                        rect.bottom = top + viewHeight;
                        videoRenderer.setSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                        scanRenderer.setSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                        if(sw == -1){
                            openCamera2();
                        }
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "desto", Toast.LENGTH_SHORT).show();
                        if(mCameraCaptureSession != null){
                            mCameraCaptureSession.getDevice().close();
                            mCameraCaptureSession.close();
                            mCameraCaptureSession = null;
                        }
                        GLES20.glDisable(GLES20.GL_BLEND);
                        videoRenderer.release();
                        mRenderer.release();
                        scanRenderer.release();
                        mEglUtils.release();
                    }
                });
            }
        });
    }

    private Size[] mSizes;
    public void initCamera2(int cameraPosition) {
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            String[] CameraIdList = mCameraManager.getCameraIdList();
            mCameraId = CameraIdList[cameraPosition];
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map != null){
                mSizes = map.getOutputSizes(SurfaceTexture.class);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("WrongConstant")
    private void openCamera2(){
        if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(mCameraId, stateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    private void takePreview() {
        try {
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(videoRenderer.getSurface());
            mCameraDevice.createCaptureSession(Collections.singletonList(videoRenderer.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    mCameraCaptureSession = cameraCaptureSession;
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    CaptureRequest previewRequest = builder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private Size getPreferredPreviewSize(Size[] sizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : sizes) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getHeight() > width && option.getWidth() > height) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size s1, Size s2) {
                    return Long.signum(s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
                }
            });
        }
        return sizes[0];
    }}
