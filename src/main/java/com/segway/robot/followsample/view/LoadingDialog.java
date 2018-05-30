package com.segway.robot.followsample.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.segway.robot.followsample.R;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * @author jacob
 * @date 4/19/18
 */

public class LoadingDialog extends Dialog {

    public LoadingDialog(Activity context) {
        super(context, R.style.dialog_theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_loading_gif);
        GifImageView loadingImage = (GifImageView) findViewById(R.id.loading_image);

        GifDrawable gifDrawable = (GifDrawable) loadingImage.getDrawable();
        gifDrawable.start();
        gifDrawable.setLoopCount(20);
    }
}
