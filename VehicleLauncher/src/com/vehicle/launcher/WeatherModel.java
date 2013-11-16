package com.vehicle.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.way.bean.City;
import com.way.bean.Pm2d5;
import com.way.bean.SimpleWeatherinfo;
import com.way.bean.Weatherinfo;
import com.way.db.CityDB;
import com.way.util.NetUtil;
import com.way.util.SharePreferenceUtil;
import com.way.util.T;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;

public class WeatherModel {
    Context mContext;
    public WeatherModel(Context context) {
        mContext = context;
    }
    
    static WeatherModel mSingleInstance;
    public static ArrayList<EventHandler> mListeners = new ArrayList<EventHandler>();
    private static String NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final int CITY_LIST_SCUESS = 0;
    private static final String FORMAT = "^[a-z,A-Z].*$";
    private static LauncherApplication mApplication;
    private CityDB mCityDB;
    private Map<String, Integer> mWeatherIcon;// 天气图标
    private Map<String, Integer> mWidgetWeatherIcon;// 插件天气图标
    private List<City> mCityList;
    // 首字母集
    private List<String> mSections;
    // 根据首字母存放数据
    private Map<String, List<City>> mMap;
    // 首字母位置集
    private List<Integer> mPositions;
    // 首字母对应的位置
    private Map<String, Integer> mIndexer;
    private boolean isCityListComplite;

    private LocationClient mLocationClient = null;
    private SharePreferenceUtil mSpUtil;
    private Weatherinfo mCurWeatherinfo;
    private SimpleWeatherinfo mCurSimpleWeatherinfo;
    private Pm2d5 mCurPm2d5;
    public static int mNetWorkState;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case CITY_LIST_SCUESS:
                isCityListComplite = true;
                if (mListeners.size() > 0)// 通知接口完成加载
                    for (EventHandler handler : mListeners) {
                        handler.onCityComplete();
                    }
                break;
            default:
                break;
            }
        }
    };



    private LocationClientOption getLocationClientOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setAddrType("all");
        option.setServiceName(mContext.getPackageName());
        option.setScanSpan(0);
        option.disableCache(true);
        return option;
    }

    public void initData() {
        mNetWorkState = NetUtil.getNetworkState(mContext);
        initCityList();
        mLocationClient = new LocationClient(mContext, getLocationClientOption());
        initWeatherIconMap();
        mSpUtil = new SharePreferenceUtil(mContext,
                SharePreferenceUtil.CITY_SHAREPRE_FILE);
        IntentFilter filter = new IntentFilter(NET_CHANGE_ACTION);
        mContext.registerReceiver(netChangeReceiver, filter);
    }

    public synchronized CityDB getCityDB() {
        if (mCityDB == null)
            mCityDB = openCityDB();
        return mCityDB;
    }

    public synchronized SharePreferenceUtil getSharePreferenceUtil() {
        if (mSpUtil == null)
            mSpUtil = new SharePreferenceUtil(mContext,
                    SharePreferenceUtil.CITY_SHAREPRE_FILE);
        return mSpUtil;
    }

    public synchronized LocationClient getLocationClient() {
        if (mLocationClient == null)
            mLocationClient = new LocationClient(mContext,
                    getLocationClientOption());
        return mLocationClient;
    }

    private CityDB openCityDB() {
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + mContext.getPackageName() + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);
        if (!db.exists()) {
            // L.i("db is not exists");
            try {
                InputStream is = mContext.getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len = -1;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                }
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                T.showLong(mApplication, e.getMessage());
                System.exit(0);
            }
        }
        return new CityDB(mContext, path);
    }

    public List<City> getCityList() {
        return mCityList;
    }

    public List<String> getSections() {
        return mSections;
    }

    public Map<String, List<City>> getMap() {
        return mMap;
    }

    public List<Integer> getPositions() {
        return mPositions;
    }

    public Map<String, Integer> getIndexer() {
        return mIndexer;
    }

    public boolean isCityListComplite() {
        return isCityListComplite;
    }

    public Map<String, Integer> getWeatherIconMap() {
        return mWeatherIcon;
    }

    public int getWeatherIcon(String climate) {
        int weatherRes = R.drawable.biz_plugin_weather_qing;
        if (TextUtils.isEmpty(climate))
            return weatherRes;
        String[] strs = { "晴", "晴" };
        if (climate.contains("转")) {// 天气带转字，取前面那部分
            strs = climate.split("转");
            climate = strs[0];
            if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
                strs = climate.split("到");
                climate = strs[1];
            }
        }
        if (mWeatherIcon.containsKey(climate)) {
            weatherRes = mWeatherIcon.get(climate);
        }
        return weatherRes;
    }



    public Weatherinfo getmCurWeatherinfo() {
        return mCurWeatherinfo;
    }

    public SimpleWeatherinfo getCurSimpleWeatherinfo() {
        return mCurSimpleWeatherinfo;
    }

    public void setCurSimpleWeatherinfo(SimpleWeatherinfo simpleWeatherinfo) {
        this.mCurSimpleWeatherinfo = simpleWeatherinfo;
    }

    public void setmCurWeatherinfo(Weatherinfo mCurWeatherinfo) {
        this.mCurWeatherinfo = mCurWeatherinfo;
    }

    public Pm2d5 getmCurPm2d5() {
        return mCurPm2d5;
    }

    public void setmCurPm2d5(Pm2d5 mCurPm2d5) {
        this.mCurPm2d5 = mCurPm2d5;
    }

    private void initWeatherIconMap() {
        mWeatherIcon = new HashMap<String, Integer>();
        mWeatherIcon.put("暴雪", R.drawable.biz_plugin_weather_baoxue);
        mWeatherIcon.put("暴雨", R.drawable.biz_plugin_weather_baoyu);
        mWeatherIcon.put("大暴雨", R.drawable.biz_plugin_weather_dabaoyu);
        mWeatherIcon.put("大雪", R.drawable.biz_plugin_weather_daxue);
        mWeatherIcon.put("大雨", R.drawable.biz_plugin_weather_dayu);

        mWeatherIcon.put("多云", R.drawable.biz_plugin_weather_duoyun);
        mWeatherIcon.put("雷阵雨", R.drawable.biz_plugin_weather_leizhenyu);
        mWeatherIcon.put("雷阵雨冰雹",
                R.drawable.biz_plugin_weather_leizhenyubingbao);
        mWeatherIcon.put("晴", R.drawable.biz_plugin_weather_qing);
        mWeatherIcon.put("沙尘暴", R.drawable.biz_plugin_weather_shachenbao);

        mWeatherIcon.put("特大暴雨", R.drawable.biz_plugin_weather_tedabaoyu);
        mWeatherIcon.put("雾", R.drawable.biz_plugin_weather_wu);
        mWeatherIcon.put("小雪", R.drawable.biz_plugin_weather_xiaoxue);
        mWeatherIcon.put("小雨", R.drawable.biz_plugin_weather_xiaoyu);
        mWeatherIcon.put("阴", R.drawable.biz_plugin_weather_yin);

        mWeatherIcon.put("雨夹雪", R.drawable.biz_plugin_weather_yujiaxue);
        mWeatherIcon.put("阵雪", R.drawable.biz_plugin_weather_zhenxue);
        mWeatherIcon.put("阵雨", R.drawable.biz_plugin_weather_zhenyu);
        mWeatherIcon.put("中雪", R.drawable.biz_plugin_weather_zhongxue);
        mWeatherIcon.put("中雨", R.drawable.biz_plugin_weather_zhongyu);
    }



    private void initCityList() {
        mCityList = new ArrayList<City>();
        mSections = new ArrayList<String>();
        mMap = new HashMap<String, List<City>>();
        mPositions = new ArrayList<Integer>();
        mIndexer = new HashMap<String, Integer>();
        mCityDB = openCityDB();// 这个必须最先复制完,所以我放在单线程中处理
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                isCityListComplite = false;
                prepareCityList();
                mHandler.sendEmptyMessage(CITY_LIST_SCUESS);
            }
        }).start();
    }

    private boolean prepareCityList() {
        mCityList = mCityDB.getAllCity();// 获取数据库中所有城市
        for (City city : mCityList) {
            String firstName = city.getFirstPY();// 第一个字拼音的第一个字母
            if (firstName.matches(FORMAT)) {
                if (mSections.contains(firstName)) {
                    mMap.get(firstName).add(city);
                } else {
                    mSections.add(firstName);
                    List<City> list = new ArrayList<City>();
                    list.add(city);
                    mMap.put(firstName, list);
                }
            } else {
                if (mSections.contains("#")) {
                    mMap.get("#").add(city);
                } else {
                    mSections.add("#");
                    List<City> list = new ArrayList<City>();
                    list.add(city);
                    mMap.put("#", list);
                }
            }
        }
        Collections.sort(mSections);// 按照字母重新排序
        int position = 0;
        for (int i = 0; i < mSections.size(); i++) {
            mIndexer.put(mSections.get(i), position);// 存入map中，key为首字母字符串，value为首字母在listview中位置
            mPositions.add(position);// 首字母在listview中位置，存入list中
            position += mMap.get(mSections.get(i)).size();// 计算下一个首字母在listview的位置
        }
        return true;
    }

    BroadcastReceiver netChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(NET_CHANGE_ACTION)) {
                if (mListeners.size() > 0)// 通知接口完成加载
                    for (EventHandler handler : mListeners) {
                        handler.onNetChange();
                    }
            }
            mNetWorkState = NetUtil.getNetworkState(mApplication);
        }

    };

    public static abstract interface EventHandler {
        public abstract void onCityComplete();

        public abstract void onNetChange();
    }

}
