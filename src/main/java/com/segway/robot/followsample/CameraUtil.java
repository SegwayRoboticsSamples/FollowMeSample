package com.segway.robot.followsample;

/**
 * This class help to get image from main camera
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class CameraUtil {
    private static final String TAG = "CameraUtil";
    private static final int INIT = 0x5a;
    private Context mContext;
    private Surface mSurface;
    private Surface mAlgoSurface;
    private CameraDevice camera;
    private CameraCaptureSession session;
    //    private ImageReader imageReader;
    private static CameraUtil sInstance;
    private CaptureRequest.Builder mBuilder;

    private boolean cameraInitiated;
    private CaptureRequest mCaptureRequest;

    private HandlerThread mWorkThread;

    private Handler mBackgroundHandler;
    private HandlerThread mHandlerThread;

    public static CaptureRequest.Key<String> CONTROL_FACE_VIEW_RECT;
    public static CaptureRequest.Key<Boolean> CONTROL_AE_LOCK;

    int mLeft;
    int mRight;
    int mTop;
    int mBottom;

    Boolean isExposureROIUpdated = false;

    private CameraUtil() {
        try {
            Class captureKey = Class.forName("android.hardware.camera2.CaptureRequest$Key");
            Constructor constructor = captureKey.getDeclaredConstructor(String.class, Class.class);
            CONTROL_FACE_VIEW_RECT = (CaptureRequest.Key<String>) constructor.newInstance("android.control.faceViewRect", String.class);
            CONTROL_AE_LOCK = (CaptureRequest.Key<Boolean>) constructor.newInstance("android.control.aeLock", boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static synchronized CameraUtil getsInstance() {
        if (sInstance == null) {
            sInstance = new CameraUtil();
        }
        return sInstance;
    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            CameraUtil.this.camera = camera;
            try {
                List<Surface> surfaceList = new ArrayList();
                if (mSurface != null) {
                    surfaceList.add(mSurface);
                }
                if (mAlgoSurface != null) {
                    surfaceList.add(mAlgoSurface);
                }

                camera.createCaptureSession(surfaceList, sessionStateCallback, null);
                cameraInitiated = true;
            } catch (CameraAccessException e) {
                Log.e(TAG, "CameraDevice.StateCallback createCaptureSession error", e);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraInitiated = false;
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraInitiated = false;
            camera.close();
        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            CameraUtil.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                        if (isExposureROIUpdated) {
                            setExposureRectInternal();
                            isExposureROIUpdated = false;
                        }
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };
    Image mAlgoImage = null;
    Object mLock = new Object();
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try {
                Image image = reader.acquireLatestImage();
                synchronized (mLock) {
                    if (mAlgoImage != null) {
                        mAlgoImage.close();
                    }
                    mAlgoImage = image;
                }
            } catch (Exception e) {
                Log.e(TAG, "onImageAvailable: read image exception", e);
            }
        }
    };

    public synchronized void init(Context _context, Surface surface, Surface algo) {
        if (mBackgroundHandler == null) {
            startBackgroundThread();
        }
        mContext = _context.getApplicationContext();
        mSurface = surface;
        mAlgoSurface = algo;
        if (mWorkThread == null) {
            mWorkThread = new HandlerThread("PTCamera");
            mWorkThread.start();
            Handler handler = new Handler(mWorkThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == INIT) {
                        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                        try {
                            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            manager.openCamera("0", cameraStateCallback, null);
//                            imageReader = ImageReader.newInstance(width, height, imageFormat, 3); //ImageFormat.YUV_420_888
//                            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
                        } catch (CameraAccessException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            };

            handler.sendEmptyMessage(INIT);
        }
    }

    public synchronized void release() {
        cameraInitiated = false;
        if (session != null) {
            session.close();
            session = null;
        }
//        if (imageReader != null) {
//            imageReader.close();
//            imageReader = null;
//        }
        if (camera != null) {
            camera.close();
            camera = null;
        }
        if (mWorkThread != null) {
            mWorkThread.quit();
            mWorkThread = null;
        }

        mAlgoImage = null;
    }

    private void startBackgroundThread() {
        if (mHandlerThread != null && mBackgroundHandler != null) {
            return;
        }
        mHandlerThread = new HandlerThread("camera2");
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        mBackgroundHandler = new Handler(looper);
    }

    public synchronized void setExposureRect(int left, int right, int top, int bottom) {
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;
        if (mLeft > 1000)
            mLeft = 1000;
        if (mRight > 1000)
            mRight = 1000;
        if (mTop > 1000)
            mTop = 1000;
        if (mBottom > 1000)
            mBottom = 1000;
        if (mLeft < -1000)
            mLeft = -1000;
        if (mRight < -1000)
            mRight = -1000;
        if (mTop < -1000)
            mTop = -1000;
        if (mBottom < -1000)
            mBottom = -1000;
        isExposureROIUpdated = true;
    }

    private void setExposureRectInternal() {

        if (mCaptureRequest == null) {
            Log.w(TAG, "Try to setExposureRect but CaptureRequest is null");
            return;
        }
        Log.d(TAG, "setExposureRectInternal() called with: left = [" + mLeft + "], right = [" + mRight + "], top = [" + mTop + "], bottom = [" + mBottom + "]");
        mBuilder.set(CONTROL_FACE_VIEW_RECT, "(" + mLeft + "," + mTop + ", " + mRight + ", " + mBottom + ")");
        if (session != null) {
            try {
                //session.abortCaptures();
                session.capture(mBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CaptureRequest createCaptureRequest() {
        try {
            mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            mBuilder.addTarget(imageReader.getSurface());
            if (mSurface != null) {
                mBuilder.addTarget(mSurface);
            }
            if (mAlgoSurface != null) {
                mBuilder.addTarget(mAlgoSurface);
            }
            mCaptureRequest = mBuilder.build();
            return mCaptureRequest;
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void checkInitiated() {
        if (!cameraInitiated) {
            throw new RuntimeException("CameraUtil not initiated");
        }
    }

    long priv;

    private int getFrameRate() {
        long now = System.currentTimeMillis();
        int rate = (int) (1000 / (now - priv + 1));
        priv = now;
        return rate;
    }
}

