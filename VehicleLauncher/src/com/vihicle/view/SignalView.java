package com.vihicle.view;

import java.util.ArrayList;

import com.example.cam.R;
import com.vehicle.launcher.MyPhoneStateListener;

import android.content.Context;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class SignalView extends RelativeLayout {
    ArrayList<View> mSignalGridViews = new ArrayList<View>(5);

	public SignalView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public SignalView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public SignalView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
    protected void onFinishInflate() {
        mSignalGridViews.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            mSignalGridViews.add(child);
        }
    }
    
    public void updateSignal(final int level){
        post(new Runnable() {            
            @Override
            public void run() {
               switch (level) {
                case MyPhoneStateListener.SIGNAL_STRENGTH_GREAT:                    
                    break;
                case MyPhoneStateListener.SIGNAL_STRENGTH_GOOD:
                    break;
                case MyPhoneStateListener.SIGNAL_STRENGTH_MODERATE:
                    break;
                case MyPhoneStateListener.SIGNAL_STRENGTH_POOR:
                    break;

                default:
                    break;
            }
               for(View child: mSignalGridViews){
                   child.setVisibility(View.INVISIBLE);
               }
               int childCount = getChildCount();
               for (int i = 0; i < level && i < childCount; i++) {
                   View child = getChildAt(i);
                   child.setVisibility(View.VISIBLE);
               }
                
            }
        });
    }

}
