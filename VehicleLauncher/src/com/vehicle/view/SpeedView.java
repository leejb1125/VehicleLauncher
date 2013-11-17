package com.vehicle.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class SpeedView extends RelativeLayout {

    private static final String TAG = "SpeedView";

    public SpeedView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public SpeedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public SpeedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
    
    protected void onFinishInflate() {
        Log.d(TAG, "onFinishInflate");
    }

}
