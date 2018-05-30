package com.segway.robot.followsample.util;

import android.app.Activity;
import android.view.Display;
import android.view.WindowManager;

import com.segway.robot.followsample.view.LoadingDialog;

/**
 * @author jacob
 * @date 4/19/18
 */

public class LoadingUtil {

    private LoadingDialog loadingDialog;


    public void showLoading(Activity activity) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(activity);
            loadingDialog.setCancelable(false);
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
            WindowManager windowManager = activity.getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            WindowManager.LayoutParams lp = loadingDialog.getWindow().getAttributes();
            lp.width = display.getWidth();
            lp.height = display.getHeight();
            loadingDialog.getWindow().setAttributes(lp);
        }
    }

    public void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private static volatile LoadingUtil instance = null;

    public static LoadingUtil getInstance() {

        if (instance == null) {
            synchronized (LoadingUtil.class) {
                if (instance == null) {
                    instance = new LoadingUtil();
                }
            }
        }

        return instance;
    }

    private LoadingUtil() {
    }


}
