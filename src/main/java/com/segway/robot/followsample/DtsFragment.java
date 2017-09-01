package com.segway.robot.followsample;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.segway.robot.algo.Pose2D;
import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.PersonDetectListener;
import com.segway.robot.algo.dts.PersonTrackingListener;
import com.segway.robot.algo.minicontroller.CheckPoint;
import com.segway.robot.algo.minicontroller.CheckPointStateListener;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.support.control.HeadPIDController;

public class DtsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "DtsFragment";

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;

    private Vision mVision;
    private DTS mDTS;
    private boolean isDetectionStarted = false;
    private boolean isTrackingStarted = false;

    private Head mHead;
    private boolean mHeadBind;

    private Base mBase;
    private boolean mBaseBind;
    private HeadPIDController mHeadPIDController = new HeadPIDController();

    private AutoFitDrawableView mTextureView;

    enum DtsState {
        STOP,
        DETECTING,
        TRACKING
    }

    boolean mHeadFollow;
    boolean mBaseFollow;

    DtsState mDtsState;

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static DtsFragment newInstance() {
        return new DtsFragment();
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            bindServices();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.detect).setOnClickListener(this);
        view.findViewById(R.id.track).setOnClickListener(this);
        view.findViewById(R.id.head_follow).setOnClickListener(this);
        view.findViewById(R.id.base_follow).setOnClickListener(this);
        mTextureView = (AutoFitDrawableView) view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mVision = Vision.getInstance();
        mHead = Head.getInstance();
        mBase = Base.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTextureView.getPreview().isAvailable()) {
            bindServices();
        } else {
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            mTextureView.setPreviewSizeAndRotation(PREVIEW_WIDTH, PREVIEW_HEIGHT, rotation);
            mTextureView.setSurfaceTextureListenerForPerview(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        unbindServices();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.detect: {
                if (!isDetectionStarted) {
                    mDTS.startDetectingPerson(mPersonDetectListener);
                    showToast("Start detecting person...");
                    isDetectionStarted = true;
                    isTrackingStarted = false;
                } else {
                    mDTS.stopDetectingPerson();
                    showToast("Stop detecting person...");
                    isDetectionStarted = false;
                }
                break;
            }
            case R.id.track: {
                if (!isTrackingStarted) {
                    mDTS.startPersonTracking(null, 15L * 60 * 1000 * 1000, mPersonTrackingListener);
                    showToast("Start tracking person...");
                    isTrackingStarted = true;
                    isDetectionStarted = false;
                } else {
                    mDTS.stopPersonTracking();
                    showToast("Stop tracking person...");
                    isTrackingStarted = false;
                }
                break;
            }
            case R.id.head_follow: {
                if (!mHeadBind) {
                    showToast("Connect to Head First...");
                    return;
                }
                if (!mHeadFollow) {
                    showToast("Enable Head Follow");
                    mHeadFollow = true;
                } else {
                    showToast("Disable Head Follow");
                    mHeadFollow = false;
                    mHead.setWorldPitch(0.3f);
                    mHead.setWorldYaw(0.0f);
                }
                break;
            }
            case R.id.base_follow: {
                mBase.setOnCheckPointArrivedListener(mCheckPointStateListener);
                if (!mBaseFollow) {
                    showToast("Enable Base Follow");
                    mBaseFollow = true;
                    mBase.setControlMode(Base.CONTROL_MODE_FOLLOW_TARGET);
                } else {
                    showToast("Disable Base Follow");
                    mBaseFollow = false;
                    mBase.stop();
                    mBase.setControlMode(Base.CONTROL_MODE_RAW);
                }
                break;
            }
        }
    }

    CheckPointStateListener mCheckPointStateListener = new CheckPointStateListener() {
        @Override
        public void onCheckPointArrived(CheckPoint checkPoint, Pose2D realPose, boolean isLast) {
            showToast("checkpoint arrived:" + checkPoint);
        }

        @Override
        public void onCheckPointMiss(CheckPoint checkPoint, Pose2D realPose, boolean isLast, int reason) {

        }
    };

    private void bindServices() {
        mVision.bindService(this.getActivity(), mVisionBindStateListener);
        mHead.bindService(this.getActivity(), mHeadBindStateListener);
        mBase.bindService(this.getActivity(), mBaseBindStateListener);
    }

    private void unbindServices() {
        if (mDTS != null) {
            mDTS.stop();
            mDTS = null;
        }
        mVision.unbindService();
        mHead.unbindService();
        mHeadBind = false;
        mBase.unbindService();
    }

    ServiceBinder.BindStateListener mVisionBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mDTS = mVision.getDTS();
            mDTS.setVideoSource(DTS.VideoSource.CAMERA);
            Surface surface = new Surface(mTextureView.getPreview().getSurfaceTexture());
            mDTS.setPreviewDisplay(surface);
            mDTS.start();
        }

        @Override
        public void onUnbind(String reason) {
            mDTS = null;
        }
    };

    private ServiceBinder.BindStateListener mHeadBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mHeadBind = true;
            mHead.setMode(Head.MODE_ORIENTATION_LOCK);
            mHead.setWorldPitch(0.3f);
            mHeadPIDController.init(new HeadControlHandlerImpl(mHead));
            mHeadPIDController.setHeadFollowFactor(1.0f);
        }

        @Override
        public void onUnbind(String reason) {
            mHeadBind = false;
        }
    };

    private ServiceBinder.BindStateListener mBaseBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mBaseBind = true;
        }

        @Override
        public void onUnbind(String reason) {
            mBaseBind = false;
        }
    };

    PersonDetectListener mPersonDetectListener = new PersonDetectListener() {
        @Override
        public void onPersonDetected(DTSPerson[] person) {
            if (person == null) {
                return;
            }
            if (person.length > 0) {
                mTextureView.drawRect(person);
            }
        }

        @Override
        public void onPersonDetectionResult(DTSPerson[] person) {

        }

        @Override
        public void onPersonDetectionError(int errorCode, String message) {

        }
    };

    PersonTrackingListener mPersonTrackingListener = new PersonTrackingListener() {
        @Override
        public void onPersonTracking(final DTSPerson person) {
            Log.d(TAG, "onPersonTracking: " + person);
            if (person == null) {
                return;
            }
//            mTextureView.drawRect(person.getId(), person.getDrawingRect());
            mTextureView.drawRect(person.getDrawingRect());

            if (mHeadFollow) {
                mHeadPIDController.updateTarget(person.getTheta(), person.getDrawingRect(), 480);
            }
            if (mBaseFollow) {
                float personDistance = person.getDistance();
                // There is a bug in DTS, while using person.getDistance(), please check the result
                // The correct distance is between 0.35 meters and 5 meters
                if (personDistance > 0.35 && personDistance < 5) {
                    float followDistance = (float) (personDistance - 1.2);
                    float theta = person.getTheta();
                    Log.d(TAG, "onPersonTracking: update base follow distance=" + followDistance + " theta=" + theta);
                    mBase.updateTarget(followDistance, theta);
                }
            }
        }

        @Override
        public void onPersonTrackingResult(DTSPerson person) {
            Log.d(TAG, "onPersonTrackingResult() called with: person = [" + person + "]");
        }

        @Override
        public void onPersonTrackingError(int errorCode, String message) {
            showToast("Person tracking error: code=" + errorCode + " message=" + message);
            mDtsState = DtsState.STOP;
        }
    };
}
