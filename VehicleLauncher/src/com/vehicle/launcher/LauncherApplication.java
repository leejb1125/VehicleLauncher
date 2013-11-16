package com.vehicle.launcher;

import android.app.Application;



public class LauncherApplication extends Application {
    private static LauncherApplication mApplication;
    WeatherModel mWeatherModel = new WeatherModel(this);
	

	public static synchronized LauncherApplication getInstance() {
		return mApplication;
	}

	@Override
	public void onCreate() {
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
