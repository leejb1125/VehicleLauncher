package com.vehicle.launcher;

/**
 * @author 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import com.example.cam.R;
import com.vehicle.view.BatteryView;
import com.vehicle.view.SignalView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsSeekBar;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.PhoneStateListener;  
import android.telephony.ServiceState;
import android.telephony.SignalStrength;    
import android.telephony.TelephonyManager; 
import android.widget.Switch; 

public class LauncherActivity extends Activity {
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick;
	BatteryView battery;
	SignalView mPhoneSignal;
	TextView mDataLink;
	Switch mNetworkSwitch;
	SeekBar mBrightnessSeekBar;
	SeekBar mVolumeSeekBar;
	Camera camera;
	
	String fileName;
	Activity act;
	Context ctx;
	   
	MyPhoneStateListener mPhoneStateListener; 
	private TelephonyManager mPhone;	
	private AudioManager mAudioManager;
	private PlaySoundPool playSoundPool ;
	Handler mBatteryUpdateHandler = new Handler();

	
	

	private int getOldBrightness() {
		int brightness;
		try {
			brightness = Settings.System.getInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
			System.out.println("brightness" + brightness);
		} catch (SettingNotFoundException snfe) {
			brightness = 255;
		}
		return brightness;
	}

	public void setScreenBrightness(int percent) {
		if (percent < 1) {
			percent = 1;
		} else if (percent > 100) {
			percent = 100;
		}
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		float value = percent / 100.0f;
		Log.i(TAG, "==================ha000000000=========" + value);
		attrs.screenBrightness = value;
		getWindow().setAttributes(attrs);
	}
	
	private void initVolume() {
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int maxVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mVolumeSeekBar.setMax(maxVolume);
		int currentVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		mVolumeSeekBar.setProgress(currentVolume);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		initViews();
		
		mPhoneStateListener = new MyPhoneStateListener(this, mPhoneSignal);
		mPhone = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        mPhone.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        
        //
        DisplayMetrics dm = new DisplayMetrics();   
        getWindowManager().getDefaultDisplay().getMetrics(dm);   
        Log.d(TAG, "resolution: "+dm.widthPixels+" * "+dm.heightPixels);   
        Log.d(TAG, "density: "+dm.density); 
	}
	
	private void initViews(){
		battery = (BatteryView) findViewById(R.id.battery);
		mPhoneSignal = (SignalView) findViewById(R.id.phone_signal);
		mDataLink = (TextView) findViewById(R.id.data_link);
		mNetworkSwitch = (Switch) findViewById(R.id.network_switch);
		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);
		
		buttonClick = (Button) findViewById(R.id.record_button);		
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});
		
		buttonClick.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View arg0) {
				camera.autoFocus(new AutoFocusCallback(){
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
					}
				});
				return true;
			}
		});	

		mNetworkSwitch.setChecked(checkNetworkValidate());
		mNetworkSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setDataConnectivity(isChecked);
				
			}
		});
		
		mBrightnessSeekBar = (SeekBar) findViewById(R.id.brightness_control_bar);
		mBrightnessSeekBar.setProgress(getOldBrightness());
		mBrightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				setScreenBrightness(progress);
			}
		});

		playSoundPool = new PlaySoundPool(LauncherActivity.this);
		playSoundPool.loadSfx(R.raw.water3, 1);
		mVolumeSeekBar =  (SeekBar) findViewById(R.id.volume_control_bar);		
		mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
						mAudioManager.setStreamVolume(
								AudioManager.STREAM_MUSIC, progress, 0);
						if (!mVolumeSeekBar.isSoundEffectsEnabled()) {
							mVolumeSeekBar.setSoundEffectsEnabled(true);
						}
						playSoundPool.play(1, 0);
			}
		});
		initVolume();
	}
	

private boolean checkNetworkValidate() {
    ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
     //mobile 3G Data Network 
    State mobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState(); 

    if (mobile==State.CONNECTED||mobile==State.CONNECTING) 
        return true; 
    return false;
}	
	

	public void setDataConnectivity(boolean isAvaliable) {
		Method dataConnSwitchmethod;
		Class telephonyManagerClass;
		Object ITelephonyStub;
		Class ITelephonyClass;

		TelephonyManager telephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);

		boolean isConnected = (telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED);
		if (isAvaliable && isConnected) {
			return;
		}
		try {
			telephonyManagerClass = Class.forName(telephonyManager.getClass()
					.getName());
			Method getITelephonyMethod = telephonyManagerClass
					.getDeclaredMethod("getITelephony");
			getITelephonyMethod.setAccessible(true);
			ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
			ITelephonyClass = Class
					.forName(ITelephonyStub.getClass().getName());

			if (isAvaliable) {
				dataConnSwitchmethod = ITelephonyClass
						.getDeclaredMethod("disableDataConnectivity");
			} else {
				dataConnSwitchmethod = ITelephonyClass
						.getDeclaredMethod("enableDataConnectivity");
			}
			dataConnSwitchmethod.setAccessible(true);
			dataConnSwitchmethod.invoke(ITelephonyStub);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				int status = intent.getIntExtra("status", 0);
				int health = intent.getIntExtra("health", 0);
				boolean present = intent.getBooleanExtra("present", false);
				int level = intent.getIntExtra("level", 0);
				int scale = intent.getIntExtra("scale", 0);
				int icon_small = intent.getIntExtra("icon-small", 0);
				int plugged = intent.getIntExtra("plugged", 0);
				int voltage = intent.getIntExtra("voltage", 0);
				int temperature = intent.getIntExtra("temperature", 0);
				String technology = intent.getStringExtra("technology");

				String statusString = "";

				switch (status) {
				case BatteryManager.BATTERY_STATUS_UNKNOWN:
					statusString = "unknown";
					break;
				case BatteryManager.BATTERY_STATUS_CHARGING:
					statusString = "charging";
					break;
				case BatteryManager.BATTERY_STATUS_DISCHARGING:
					statusString = "discharging";
					break;
				case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
					statusString = "not charging";
					break;
				case BatteryManager.BATTERY_STATUS_FULL:
					statusString = "full";
					break;
				}

				String healthString = "";

				switch (health) {
				case BatteryManager.BATTERY_HEALTH_UNKNOWN:
					healthString = "unknown";
					break;
				case BatteryManager.BATTERY_HEALTH_GOOD:
					healthString = "good";
					break;
				case BatteryManager.BATTERY_HEALTH_OVERHEAT:
					healthString = "overheat";
					break;
				case BatteryManager.BATTERY_HEALTH_DEAD:
					healthString = "dead";
					break;
				case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
					healthString = "voltage";
					break;
				case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
					healthString = "unspecified failure";
					break;
				}

				String acString = "";

				switch (plugged) {
				case BatteryManager.BATTERY_PLUGGED_AC:
					acString = "plugged ac";
					break;
				case BatteryManager.BATTERY_PLUGGED_USB:
					acString = "plugged usb";
					break;
				}
				if(battery != null){
					battery.setBatteryStatusAndLevel(status, level);
				}

				Log.v("status", statusString);
				Log.v("health", healthString);
				Log.v("present", String.valueOf(present));
				Log.v("level", String.valueOf(level));
				Log.v("scale", String.valueOf(scale));
				Log.v("icon_small", String.valueOf(icon_small));
				Log.v("plugged", acString);
				Log.v("voltage", String.valueOf(voltage));
				Log.v("temperature", String.valueOf(temperature));
				Log.v("technology", technology);
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		//      preview.camera = Camera.open();
		camera = Camera.open();
		camera.startPreview();
		preview.setCamera(camera);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBroadcastReceiver, filter);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
		unregisterReceiver(mBroadcastReceiver);
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			String fileDir = "/sdcard/camtest";
			
			try {
				
				File file = new File(fileDir);
				// 如果文件夹不存在则创建
				if (!file.exists() && !file.isDirectory()) {
					System.out.println("//不存在");
					file.mkdir();
				} else {
					System.out.println("//目录存在");
				}
				// Write to SD Card
				fileName = String.format("/sdcard/camtest/%d.jpg", System.currentTimeMillis());
				outStream = new FileOutputStream(fileName);
				outStream.write(data);
				outStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
			resetCam();
		}
	};
}
