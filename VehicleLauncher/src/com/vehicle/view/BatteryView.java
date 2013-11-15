package com.vehicle.view;

import javax.net.ssl.HandshakeCompletedListener;

import com.example.cam.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class BatteryView extends RelativeLayout {
	private static final String TAG = "BatteryView";
	ImageView mBatteryFrame;
	ImageView mBatteryPlug;
	NinePatchDrawable mBatteryFrameDrawable;
	Rect mContentArea = new Rect();
	Paint mPaint = new Paint();
	int mStatus;
	int mLevel;
	final int LOW_BATTERY_THRESHOLD = 20;

	public BatteryView(Context context) {
		super(context);
		setWillNotDraw(false);
	}

	public BatteryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWillNotDraw(false);
	}

	public BatteryView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
	}
	
    @Override
    protected void onFinishInflate(){
    	mBatteryFrame = (ImageView)findViewById(R.id.battery_frame);
    	mBatteryPlug = (ImageView)findViewById(R.id.battery_plug);
    	if(mBatteryFrame != null){
    		mBatteryFrameDrawable = (NinePatchDrawable) mBatteryFrame.getDrawable();
    	}    	
    }

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		if(mBatteryFrameDrawable != null){
    		Rect rPadding = new Rect();
			mPaint.setColor(Color.BLUE);
			mPaint.setStyle(Style.FILL);
    		mBatteryFrameDrawable.getPadding(rPadding);
    		mContentArea.set(0, 0, getWidth(), getHeight());    		
    		mContentArea.set(rPadding.left, rPadding.top, mContentArea.right - rPadding.width(), mContentArea.bottom - rPadding.height());
    		int fullWidth = mContentArea.width();
    		mContentArea.right = mContentArea.left + (int)(fullWidth * mLevel * 0.01);
			canvas.drawRect(mContentArea, mPaint);
			Log.d(TAG, "onDraw content area = " + mContentArea);
		}
		super.onDraw(canvas);
	}
	
	public void setBatteryStatusAndLevel(final int status, final int level){
		mStatus = status;
		mLevel = level;
		post(new Runnable() {		
			@Override
			public void run() {
				mBatteryPlug.setVisibility(View.INVISIBLE);
				switch (status) {
				case BatteryManager.BATTERY_STATUS_CHARGING:
					if(mBatteryFrame != null){
						mBatteryFrame.setImageResource(R.drawable.battery_charging);
					}
					if(mBatteryPlug != null){
						mBatteryPlug.setVisibility(View.VISIBLE);
					}
					break;
				case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				case BatteryManager.BATTERY_STATUS_DISCHARGING:
					if(mBatteryFrame != null){
						mBatteryFrame.setImageResource(R.drawable.battery_discharged);
					}
					break;
				case BatteryManager.BATTERY_STATUS_FULL:
					if(mBatteryFrame != null){
						mBatteryFrame.setImageResource(R.drawable.battery_discharged);
					}
					break;
				}
				
				if(level < LOW_BATTERY_THRESHOLD && status != BatteryManager.BATTERY_STATUS_CHARGING){
					if(mBatteryFrame != null){
						mBatteryFrame.setImageResource(R.drawable.battery_low_battery);
					}
				}
			}
		});
		
	}

}
