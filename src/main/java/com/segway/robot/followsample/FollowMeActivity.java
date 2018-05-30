package com.segway.robot.followsample;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;

import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.followsample.interfaces.PresenterChangeInterface;
import com.segway.robot.followsample.interfaces.ViewChangeInterface;
import com.segway.robot.followsample.presenter.FollowMePresenter;
import com.segway.robot.followsample.util.LoadingUtil;
import com.segway.robot.followsample.util.ToastUtil;
import com.segway.robot.followsample.view.AutoFitDrawableView;

/**
 * @author jacob
 * @Des While using following function, the robot may run into people or other obstacles. Always be on the look-out.
 */
public class FollowMeActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "FollowMeActivity";

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;

    private AutoFitDrawableView mAutoFitDrawableView;
    private LinearLayout mButtons;

    private FollowMePresenter mFollowMePresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoadingUtil.getInstance().showLoading(this);
        setContentView(R.layout.activity_follow_me);
        initView();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        mAutoFitDrawableView.setPreviewSizeAndRotation(PREVIEW_WIDTH, PREVIEW_HEIGHT, rotation);
        mAutoFitDrawableView.setSurfaceTextureListenerForPerview(mSurfaceTextureListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFollowMePresenter.stopPresenter();
        finish();
    }

    private void initView() {
        mAutoFitDrawableView = (AutoFitDrawableView) findViewById(R.id.autoDrawable);
        mButtons = (LinearLayout) findViewById(R.id.buttons);
    }

    private void initListener() {
        findViewById(R.id.initiateDetect).setOnClickListener(this);
        findViewById(R.id.terminateDetect).setOnClickListener(this);
        findViewById(R.id.initiateTrack).setOnClickListener(this);
        findViewById(R.id.terminateTrack).setOnClickListener(this);
        mAutoFitDrawableView.setOnClickListener(this);
    }


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            mFollowMePresenter = new FollowMePresenter(mPresenterChangeInterface, mViewChangeInterface);
            mFollowMePresenter.startPresenter();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private PresenterChangeInterface mPresenterChangeInterface = new PresenterChangeInterface() {

        @Override
        public void dismissLoading() {
            LoadingUtil.getInstance().dismissLoading();
            mButtons.setVisibility(View.VISIBLE);
        }

        @Override
        public void showToast(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtil.showToast(FollowMeActivity.this, message);
                }
            });
        }

        @Override
        public void drawPersons(DTSPerson[] dtsPersons) {
            mAutoFitDrawableView.drawRect(dtsPersons);
        }

        @Override
        public void drawPerson(DTSPerson dtsPerson) {
            mAutoFitDrawableView.drawRect(dtsPerson.getDrawingRect());
        }
    };


    private ViewChangeInterface mViewChangeInterface = new ViewChangeInterface() {
        @Override
        public AutoFitDrawableView getAutoFitDrawableView() {
            return mAutoFitDrawableView;
        }
    };

    @Override
    public void onClick(View v) {

        if (!mFollowMePresenter.isServicesAvailable()) {
            return;
        }
        switch (v.getId()) {
            case R.id.initiateDetect:
                mFollowMePresenter.actionInitiateDetect();
                break;
            case R.id.terminateDetect:
                mFollowMePresenter.actionTerminateDetect();
                break;
            case R.id.initiateTrack:
                mFollowMePresenter.actionInitiateTrack();
                break;
            case R.id.terminateTrack:
                mFollowMePresenter.actionTerminateTrack();
                break;
            case R.id.autoDrawable:
                // click camera image to switch mode,the default mode is closed
                if (mFollowMePresenter.getObstacleAvoidanceOpen()) {
                    mFollowMePresenter.setObstacleAvoidanceOpen(false);
                    ToastUtil.showToast(FollowMeActivity.this, "Obstacle avoidance function is closed....");
                } else {
                    mFollowMePresenter.setObstacleAvoidanceOpen(true);
                    ToastUtil.showToast(FollowMeActivity.this, "Obstacle avoidance function is opened....");
                }
                break;
        }
    }
}
