package com.segway.robot.followsample.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.segway.robot.algo.dts.DTSPerson;

/**
 * Created by gaofeng on 2017/3/23.
 */

public class AutoFitDrawableView extends FrameLayout {
    private TextureView mPreview;
    private CustomView mOverlay;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mRotation;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener;

    public AutoFitDrawableView(Context context) {
        super(context);
        setUpViews();
    }

    public AutoFitDrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpViews();
    }

    public AutoFitDrawableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpViews();
    }

    public void setPreviewSizeAndRotation(int width, int height, int rotation) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        mRotation = rotation;
        requestLayout();
    }

    public TextureView getPreview() {
        return mPreview;
    }

    public void setSurfaceTextureListenerForPerview(TextureView.SurfaceTextureListener listener) {
        mSurfaceTextureListener = listener;
    }

    public void drawRect(Rect... rects) {
        mOverlay.drawRect(rects);
    }

    public void drawRect(int id, Rect rect) {
        mOverlay.drawRect(id, rect);
    }

    public void drawRect(DTSPerson[] persons) {
        mOverlay.drawRect(persons);
    }

    TextureView.SurfaceTextureListener mPrivateSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
            TextureView.SurfaceTextureListener listener = mSurfaceTextureListener;
            if (listener != null) {
                listener.onSurfaceTextureAvailable(surfaceTexture, width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
            TextureView.SurfaceTextureListener listener = mSurfaceTextureListener;
            if (listener != null) {
                listener.onSurfaceTextureSizeChanged(surfaceTexture, width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            TextureView.SurfaceTextureListener listener = mSurfaceTextureListener;
            return listener != null && listener.onSurfaceTextureDestroyed(surfaceTexture);
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            TextureView.SurfaceTextureListener listener = mSurfaceTextureListener;
            if (listener != null) {
                listener.onSurfaceTextureUpdated(surfaceTexture);
            }
        }
    };

    private void setUpViews() {
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        mPreview = new TextureView(getContext());
        mOverlay = new CustomView(getContext());
        mPreview.setSurfaceTextureListener(mPrivateSurfaceTextureListener);
        mOverlay.setZOrderOnTop(true);
        addView(mPreview, layoutParams);
        addView(mOverlay, layoutParams);
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mRatioHeight, mRatioWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == mRotation || Surface.ROTATION_270 == mRotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mRatioHeight,
                    (float) viewWidth / mRatioWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (mRotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == mRotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mPreview.setTransform(matrix);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
        configureTransform(width, height);
    }

    private class CustomView extends SurfaceView {
        private final Paint paint;
        private final SurfaceHolder mHolder;
        private final MyHandle mDrawingHandler;

        public CustomView(Context context) {
            super(context);
            mHolder = getHolder();
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(3.0f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setTextSize(40);
            mDrawingHandler = new MyHandle();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        public void drawRect(final Rect... rects) {
            mDrawingHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPreview.invalidate();
                    if (mHolder.getSurface().isValid()) {
                        final Canvas canvas = mHolder.lockCanvas();
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawColor(Color.TRANSPARENT);
                            for (Rect rect : rects) {
                                canvas.drawRect(new Rect(
                                        mRatioWidth - rect.left,
                                        rect.top,
                                        mRatioWidth - rect.right,
                                        rect.bottom), paint);
                            }
                            mHolder.unlockCanvasAndPost(canvas);
                            mDrawingHandler.removeMessages(MyHandle.CLEAR);
                            mDrawingHandler.sendEmptyMessageDelayed(MyHandle.CLEAR, 1000);
                        }
                    }
                }
            });
        }

        public void drawRect(final DTSPerson[] persons) {
            mDrawingHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPreview.invalidate();
                    if (mHolder.getSurface().isValid()) {
                        final Canvas canvas = mHolder.lockCanvas();

                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawColor(Color.TRANSPARENT);
                            for (DTSPerson person : persons) {
                                Rect rect = person.getDrawingRect();
                                canvas.drawRect(new Rect(
                                        mRatioWidth - rect.left,
                                        rect.top,
                                        mRatioWidth - rect.right,
                                        rect.bottom), paint);
                                canvas.drawText("id=" + person.getId(), mRatioWidth - rect.right, rect
                                                .top,
                                        paint);
                            }

                            mHolder.unlockCanvasAndPost(canvas);
                            mDrawingHandler.removeMessages(MyHandle.CLEAR);
                            mDrawingHandler.sendEmptyMessageDelayed(MyHandle.CLEAR, 1000);
                        }
                    }
                }
            });
        }

        public void drawRect(final int id, final Rect rect) {
            mDrawingHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPreview.invalidate();
                    if (mHolder.getSurface().isValid()) {
                        final Canvas canvas = mHolder.lockCanvas();

                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawColor(Color.TRANSPARENT);

                            canvas.drawRect(new Rect(
                                    mRatioWidth - rect.left,
                                    rect.top,
                                    mRatioWidth - rect.right,
                                    rect.bottom), paint);
                            canvas.drawText("id=" + id, mRatioWidth - rect.right, rect.top, paint);

                            mHolder.unlockCanvasAndPost(canvas);
                            mDrawingHandler.removeMessages(MyHandle.CLEAR);
                            mDrawingHandler.sendEmptyMessageDelayed(MyHandle.CLEAR, 1000);
                        }
                    }
                }
            });
        }

        private class MyHandle extends Handler {
            static final int CLEAR = 1;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == CLEAR) {
                    if (mHolder.getSurface().isValid()) {
                        final Canvas canvas = mHolder.lockCanvas();
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            canvas.drawColor(Color.TRANSPARENT);
                            mHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            }
        }
    }
}
