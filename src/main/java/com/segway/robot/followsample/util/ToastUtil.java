package com.segway.robot.followsample.util;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.Toast;

import com.segway.robot.followsample.R;


/**
 * @author jacob
 */
public class ToastUtil {

    private static String oldMsg;
    private static Toast toast;
    private static long firstTime;
    private static long secondTime;
    private static Button bt_toast;

    /**
     * @param newMsg
     * @Title showToast
     * @Description only one Toast used
     */
    public static void showToast(Context context, String newMsg) {
        if (toast == null) {
            oldMsg = newMsg;
            toast = Toast.makeText(context, newMsg, Toast.LENGTH_LONG);
            bt_toast = new Button(context);
            //style
            bt_toast.setBackgroundResource(R.drawable.shape_toast);
            bt_toast.setTextColor(Color.parseColor("#ffffff"));
            bt_toast.setText(newMsg);
            bt_toast.setAllCaps(false);
            bt_toast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            toast.setView(bt_toast);
            toast.show();
            firstTime = System.currentTimeMillis();
        } else {
            secondTime = System.currentTimeMillis();
            if (newMsg.equals(oldMsg)) {
                if (secondTime - firstTime > Toast.LENGTH_SHORT) {
                    toast.show();
                }
            } else {
                oldMsg = newMsg;
                bt_toast.setText(newMsg);
                toast.show();
            }
        }
        firstTime = secondTime;
    }
}
