package com.vehicle.view;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vehicle.launcher.LauncherApplication;
import com.vehicle.launcher.WeatherModel;
import com.way.bean.City;
import com.way.bean.Pm2d5;
import com.way.bean.SimpleWeather;
import com.way.bean.SimpleWeatherinfo;
import com.way.bean.Weather;
import com.way.bean.Weatherinfo;
import com.way.db.CityDB;
import com.way.util.ConfigCache;

import com.way.util.L;
import com.way.util.NetUtil;
import com.way.util.SharePreferenceUtil;
import com.way.util.T;
import com.way.util.TimeUtil;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WeatherView extends RelativeLayout implements
WeatherModel.EventHandler {
    public static final String UPDATE_WIDGET_WEATHER_ACTION = "com.way.action.update_weather";
    public static final String WEATHER_SIMPLE_URL = "http://www.weather.com.cn/data/sk/";// 简要天气信息
    public static final String WEATHER_BASE_URL = "http://m.weather.com.cn/data/";// 详细天气
    public static final String PM2D5_BASE_URL = "http://www.pm25.in/api/querys/pm2_5.json?city=SHENZHEN&token=HUL7sQBaUKVvgWzdKdTB&stations=no";
    private static final String WEATHER_INFO_FILENAME = "_weather.json";
    private static final String SIMPLE_WEATHER_INFO_FILENAME = "_simple_weather.json";
    private static final String PM2D5_INFO_FILENAME = "_pm2d5.json";
    private static final int LOACTION_OK = 0;
    private static final int ON_NEW_INTENT = 1;
    private static final int UPDATE_EXISTS_CITY = 2;
    private static final int GET_WEATHER_RESULT = 3;
    protected static final String TAG = "WeatherView";
    private LocationClient mLocationClient;
    private CityDB mCityDB;
    private SharePreferenceUtil mSpUtil;
    private WeatherModel mWeatherModel;
    private City mCurCity;
    private Weatherinfo mCurWeatherinfo;
    private SimpleWeatherinfo mCurSimpleWeatherinfo;
    private Pm2d5 mCurPm2d5;
    private Gson mGson;
    private ImageView  mUpdateBtn;
    private ProgressBar mUpdateProgressBar;
    private TextView mTitleTextView;
    private City mNewIntentCity;

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv;
    private ImageView weatherImg, pmImg;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case LOACTION_OK:

                String cityName = (String) msg.obj;
                L.i("cityName = " + cityName);
                mCurCity = mCityDB.getCity(cityName);
                L.i(mCurCity.toString());
                mSpUtil.setCity(mCurCity.getCity());
                cityTv.setText(mCurCity.getCity());
                updateWeather(true);
                break;
            case ON_NEW_INTENT:
                mCurCity = mNewIntentCity;
                mSpUtil.setCity(mCurCity.getCity());
                cityTv.setText(mCurCity.getCity());
                updateWeather(true);
                break;
            case UPDATE_EXISTS_CITY:
                String sPCityName = mSpUtil.getCity();
                mCurCity = mCityDB.getCity(sPCityName);
                updateWeather(false);
                break;
            case GET_WEATHER_RESULT:
                updateWeatherInfo();
                updatePm2d5Info();
                updateWidgetWeather();
                mUpdateBtn.setVisibility(View.VISIBLE);
                mUpdateProgressBar.setVisibility(View.GONE);
                break;
            default:
                break;
            }
        }

    };
    
    public WeatherView(Context context) {
        super(context);
    }

    public WeatherView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeatherView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    private void updateWidgetWeather() {
        getContext().sendBroadcast(new Intent(UPDATE_WIDGET_WEATHER_ACTION));
    }
    
    private void initView() {
//        mCityManagerBtn = (ImageView) findViewById(R.id.title_city_manager);
//        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
//        mShareBtn = (ImageView) findViewById(R.id.title_share);
//        mLocationBtn = (ImageView) findViewById(R.id.title_location);
//        mCityManagerBtn.setOnClickListener(this);
//        mUpdateBtn.setOnClickListener(this);
//        mShareBtn.setOnClickListener(this);
//        mLocationBtn.setOnClickListener(this);
        
      
        if (TextUtils.isEmpty(mSpUtil.getCity())) {
            if (NetUtil.getNetworkState(getContext()) != NetUtil.NETWORN_NONE) {
                mLocationClient.start();
                mLocationClient.requestLocation();
                T.showShort(getContext(), "正在定位...");
                mUpdateBtn.setVisibility(View.GONE);
                mUpdateProgressBar.setVisibility(View.VISIBLE);
            } else {
                //T.showShort(this, R.string.net_err);
            }
        } else {
            mHandler.sendEmptyMessage(UPDATE_EXISTS_CITY);
        }
    }
    
    private void initData() {
        WeatherModel.mListeners.add(this);
        mWeatherModel = LauncherApplication.getInstance().getWeatherModel();
        mSpUtil = mWeatherModel.getSharePreferenceUtil();
        mLocationClient = mWeatherModel.getLocationClient();

        mLocationClient.registerLocationListener(mLocationListener);
        mCityDB = mWeatherModel.getCityDB();
        // 不转换没有 @Expose 注解的字段
        mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .create();
    }
    
    private void updateWeather(final boolean isRefresh) {
        if (NetUtil.getNetworkState(getContext()) == NetUtil.NETWORN_NONE && isRefresh) {
            //T.showLong(this, R.string.net_err);
            return;
        }
        if (mCurCity == null) {
            //ßT.showLong(mApplication, "未找到此城市,请重新定位或选择...");
            return;
        }
        // T.showShort(this, "正在刷新天气...");
        timeTv.setText("同步中...");
        mTitleTextView.setText(mCurCity.getCity() + "天气");
        mUpdateBtn.setVisibility(View.GONE);
        mUpdateProgressBar.setVisibility(View.VISIBLE);
        // 启动线程获取天气信息
        new Thread() {
            @Override
            public void run() {
                super.run();
                getWeatherInfo(isRefresh);
                getSimpleWeatherInfo(isRefresh);
                getPm2d5Info(isRefresh);

                if (mCurSimpleWeatherinfo != null)
                    L.i(mCurSimpleWeatherinfo.toString());
                if (mCurWeatherinfo != null)
                    L.i(mCurWeatherinfo.toString());
                if (mCurPm2d5 != null)
                    L.i(mCurPm2d5.toString());
                mHandler.sendEmptyMessage(GET_WEATHER_RESULT);
            }

        }.start();
    }

    private void getWeatherInfo(boolean isRefresh) {
        String url = WEATHER_BASE_URL + mCurCity.getNumber() + ".html";
        String result;
        if (!isRefresh) {
            if (mWeatherModel.getmCurWeatherinfo() != null) {// 读取内存中的信息
                mCurWeatherinfo = mWeatherModel.getmCurWeatherinfo();
                L.i("get the weather info from memory");
                return;// 直接返回，不继续执行
            }
            // result = getInfoFromFile(WEATHER_INFO_FILENAME);// 文件中的信息
            result = ConfigCache.getUrlCache(url);
            if (!TextUtils.isEmpty(result)) {
                parseWeatherInfo(url, result, false);
                L.i("get the weather info from file");
                return;
            }
        }

        // L.i("weather url: " + url);
        String weatherResult = connServerForResult(url);
        if (TextUtils.isEmpty(weatherResult))
            weatherResult = getInfoFromFile(WEATHER_INFO_FILENAME);
        parseWeatherInfo(url, weatherResult, true);
    }

    private void getSimpleWeatherInfo(boolean isRefresh) {
        String url = WEATHER_SIMPLE_URL + mCurCity.getNumber() + ".html";
        String result;
        if (!isRefresh) {
            if (mWeatherModel.getCurSimpleWeatherinfo() != null) {// 读取内存中的信息
                mCurSimpleWeatherinfo = mWeatherModel.getCurSimpleWeatherinfo();
                L.i("get the simple weather info from memory");
                return;// 直接返回，不继续执行
            }
            // result = getInfoFromFile(SIMPLE_WEATHER_INFO_FILENAME);// 文件中的信息
            result = ConfigCache.getUrlCache(url);
            if (!TextUtils.isEmpty(result)) {
                parseSimpleWeatherInfo(url, result, false);
                L.i("get the simple weather info from file");
                return;
            }
        }

        // L.i("weather url: " + url);
        String weatherResult = connServerForResult(url);
        if (TextUtils.isEmpty(weatherResult))
            weatherResult = getInfoFromFile(SIMPLE_WEATHER_INFO_FILENAME);
        parseSimpleWeatherInfo(url, weatherResult, true);
    }

    private void getPm2d5Info(boolean isRefresh) {
        String urlPm2d5 = PM2D5_BASE_URL.replace("SHENZHEN",
                mCurCity.getAllPY());
        String result;
        if (!isRefresh) {
            if (mWeatherModel.getmCurPm2d5() != null) {// 内存中的信息
                mCurPm2d5 = mWeatherModel.getmCurPm2d5();
                L.i("get the pm2.5 info from memory");
                return;
            }
            // result = getInfoFromFile(PM2D5_INFO_FILENAME);// 文件中的信息
            result = ConfigCache.getUrlCache(urlPm2d5);
            if (!TextUtils.isEmpty(result)) {
                parsePm2d5Info(urlPm2d5, result, false);
                L.i("get the pm2.5 info from file");
                return;
            }
        }
        // L.i("pm2.5 url: " + urlPm2d5);
        String pmResult = connServerForResult(urlPm2d5);
        if (TextUtils.isEmpty(pmResult) || pmResult.contains("error")) {// 如果获取失败，则取本地文件中的信息，
            String fileResult = getInfoFromFile(PM2D5_INFO_FILENAME);
            // 只有当本地文件信息与当前城市相匹配时才使用
            if (!TextUtils.isEmpty(fileResult)
                    && fileResult.contains(mCurCity.getCity()))
                pmResult = fileResult;
        }
        // pmResult = getInfoFromFile(PM2D5_INFO_FILENAME);

        parsePm2d5Info(urlPm2d5, pmResult, true);
    }

    private void parseWeatherInfo(String url, String result,
            boolean isRefreshWeather) {
        mCurWeatherinfo = null;
        mWeatherModel.setmCurWeatherinfo(null);
        if (!TextUtils.isEmpty(result) && !result.contains("页面没有找到")) {
            // L.i(result);
            Weather weather = mGson.fromJson(result, Weather.class);
            mCurWeatherinfo = weather.getWeatherinfo();
            // L.i(mCurWeatherinfo.toString());
        } else {
            result = "";
        }
        if (isRefreshWeather && !TextUtils.isEmpty(result))
            // save2File(result, WEATHER_INFO_FILENAME);
            ConfigCache.setUrlCache(result, url);
    }

    private void parseSimpleWeatherInfo(String url, String result,
            boolean isRefreshWeather) {
        mCurSimpleWeatherinfo = null;
        mWeatherModel.setCurSimpleWeatherinfo(null);
        if (!TextUtils.isEmpty(result) && !result.contains("页面没有找到")) {
            // L.i(result);
            SimpleWeather weather = mGson.fromJson(result, SimpleWeather.class);
            mCurSimpleWeatherinfo = weather.getWeatherinfo();
            // L.i(mCurSimpleWeatherinfo.toString());
            mWeatherModel.setCurSimpleWeatherinfo(mCurSimpleWeatherinfo);
        } else {
            result = "";
        }
        if (isRefreshWeather && !TextUtils.isEmpty(result))
            // save2File(result, SIMPLE_WEATHER_INFO_FILENAME);
            ConfigCache.setUrlCache(result, url);
    }

    private void parsePm2d5Info(String url, String result,
            boolean isRefreshPm2d5) {
        mCurPm2d5 = null;
        mWeatherModel.setmCurWeatherinfo(null);
        if (!TextUtils.isEmpty(result) && !result.contains("error")) {
            // L.i(result);
            List<Pm2d5> pm2d5s = mGson.fromJson(result,
                    new TypeToken<List<Pm2d5>>() {
                    }.getType());
            mCurPm2d5 = pm2d5s.get(0);
            // L.i(mCurPm2d5.toString());
        } else {
            result = "";
        }
        if (isRefreshPm2d5 && !TextUtils.isEmpty(result))
            // save2File(result, PM2D5_INFO_FILENAME);
            ConfigCache.setUrlCache(result, url);
    }


    /**
     * 从文件中获取信息
     * 
     * @param fileName
     * @return
     */
    private String getInfoFromFile(String fileName) {
        String result = "";
        try {
            FileInputStream fis = getContext().openFileInput(fileName);
            byte[] buffer = new byte[fis.available()];// 本地文件可以实例化buffer，网络文件不可行
            fis.read(buffer);
            result = new String(buffer);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新天气界面
     */
    private void updateWeatherInfo() {
        if (mCurWeatherinfo != null) {
            mWeatherModel.setmCurWeatherinfo(mCurWeatherinfo);// 保存到全局变量中
            temperatureTv.setText(mCurWeatherinfo.getTemp1());
            cityTv.setText(mCurWeatherinfo.getCity());

            String wind = mCurWeatherinfo.getWind1();
            if (wind.contains("转")) {
                String[] strs = wind.split("转");
                wind = strs[0];
            }
            windTv.setText(wind);
            String climate = mCurWeatherinfo.getWeather1();
            climateTv.setText(climate);
            mSpUtil.setSimpleClimate(climate);
            String[] strs = { "晴", "晴" };
            if (climate.contains("转")) {// 天气带转字，取前面那部分
                strs = climate.split("转");
                climate = strs[0];
                if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
                    strs = climate.split("到");
                    climate = strs[1];
                }
            }
            L.i("处理后的天气为：" + climate);
            if (mWeatherModel.getWeatherIconMap().containsKey(climate)) {
                int iconRes = mWeatherModel.getWeatherIconMap().get(climate);
                weatherImg.setImageResource(iconRes);
            } else {
                // do nothing 没有这样的天气图片

            }
            if (mCurSimpleWeatherinfo != null) {
                if (!mCurSimpleWeatherinfo.getTime().equals(mSpUtil.getTime())) {
                    mSpUtil.setTime(mCurSimpleWeatherinfo.getTime());
                    mSpUtil.setTimeSamp(System.currentTimeMillis());// 保存一下更新的时间戳
                }
                mSpUtil.setSimpleTemp(mCurSimpleWeatherinfo.getTemp());
                timeTv.setText(TimeUtil.getDay(mSpUtil.getTimeSamp())
                        + mCurSimpleWeatherinfo.getTime() + "发布");
                humidityTv.setText("湿度:" + mCurSimpleWeatherinfo.getSD());
            }
            
        } else {
            temperatureTv.setText("N/A");
            cityTv.setText(mCurCity.getCity());
            windTv.setText("N/A");
            climateTv.setText("N/A");
            //weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
            //T.showLong(mApplication, "获取天气信息失败");
            L.d(TAG, "updateWeatherInfo failed");
        }
    }

    /**
     * 更新pm2.5界面
     */
    private void updatePm2d5Info() {
        if (mCurPm2d5 != null) {
            mWeatherModel.setmCurPm2d5(mCurPm2d5);
            pmQualityTv.setText(mCurPm2d5.getQuality());
            pmDataTv.setText(mCurPm2d5.getPm2_5_24h());
            int pm2_5 = Integer.parseInt(mCurPm2d5.getPm2_5_24h());
//            int pm_img = R.drawable.biz_plugin_weather_0_50;
//            if (pm2_5 > 300) {
//                pm_img = R.drawable.biz_plugin_weather_greater_300;
//            } else if (pm2_5 > 200) {
//                pm_img = R.drawable.biz_plugin_weather_201_300;
//            } else if (pm2_5 > 150) {
//                pm_img = R.drawable.biz_plugin_weather_151_200;
//            } else if (pm2_5 > 100) {
//                pm_img = R.drawable.biz_plugin_weather_101_150;
//            } else if (pm2_5 > 50) {
//                pm_img = R.drawable.biz_plugin_weather_51_100;
//            } else {
//                pm_img = R.drawable.biz_plugin_weather_0_50;
//            }

//            pmImg.setImageResource(pm_img);
        } else {
            pmQualityTv.setText("N/A");
            pmDataTv.setText("N/A");
            //pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            T.showLong(getContext(), "未获取到PM2.5数据");
        }
    }

    // 请求服务器，获取返回数据
    private String connServerForResult(String url) {
        HttpGet httpRequest = new HttpGet(url);
        String strResult = "";
        if (NetUtil.getNetworkState(getContext()) != NetUtil.NETWORN_NONE) {
            try {
                // HttpClient对象
                HttpClient httpClient = new DefaultHttpClient();
                // 获得HttpResponse对象
                HttpResponse httpResponse = httpClient.execute(httpRequest);
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                    // 取得返回的数据
                    strResult = EntityUtils.toString(httpResponse.getEntity());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return strResult; // 返回结果
    }

    BDLocationListener mLocationListener = new BDLocationListener() {

        @Override
        public void onReceivePoi(BDLocation arg0) {
            // do nothing
        }

        @Override
        public void onReceiveLocation(BDLocation location) {
            // mActionBar.setProgressBarVisibility(View.GONE);
            mUpdateBtn.setVisibility(View.VISIBLE);
            mUpdateProgressBar.setVisibility(View.GONE);
            if (location == null || TextUtils.isEmpty(location.getCity())) {
                // T.showShort(getApplicationContext(), "location = null");
//                final Dialog dialog = IphoneDialog.getTwoBtnDialog(
//                        MainActivity.this, "定位失败", "是否手动选择城市?");
//                ((Button) dialog.findViewById(R.id.ok))
//                        .setOnClickListener(new OnClickListener() {
//
//                            @Override
//                            public void onClick(View v) {
//                                startActivityForResult();
//                                dialog.dismiss();
//                            }
//                        });
//                dialog.show();
                L.d(TAG, "locating city failed.");
                return;
            }
            String cityName = location.getCity();
            mLocationClient.stop();
            Message msg = mHandler.obtainMessage();
            msg.what = LOACTION_OK;
            msg.obj = cityName;
            mHandler.sendMessage(msg);// 更新天气
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            mNewIntentCity = (City) data.getSerializableExtra("city");
            mHandler.sendEmptyMessage(ON_NEW_INTENT);
        }
    }

    @Override
    public void onCityComplete() {
        
    }

    @Override
    public void onNetChange() {
        
    }

}
