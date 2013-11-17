package com.vehicle.launcher;

import android.app.Application;



public class LauncherApplication extends Application {
    private static LauncherApplication mApplication;
    WeatherModel mWeatherModel = new WeatherModel(this);
	

	public static synchronized LauncherApplication getInstance() {
		return mApplication;
	}
	

	public LauncherApplication() {
        super();
        mApplication = this;
    }

    @Override
	public void onCreate() {
	    mApplication = this;
		super.onCreate();
		mWeatherModel.initData();
	}
	
	public int getNetworkState(){
	    return WeatherModel.mNetWorkState;
	}
	
	public WeatherModel getWeatherModel(){
	    return mWeatherModel;
	}

	
}
