package com.segway.robot.followsample;

import android.app.Application;
import android.content.Context;

/**
 * @author jacob
 * @date 5/29/18
 */

public class CustomApplication extends Application {

    private static CustomApplication mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getContext());
    }
}
