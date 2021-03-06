
package com.vehicle.launcher;

import com.vehicle.view.SignalView;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class MyPhoneStateListener extends PhoneStateListener {

    private String TAG = "";
    
    private Context mContext;

    public static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;

    public static final int SIGNAL_STRENGTH_POOR = 1;

    public static final int SIGNAL_STRENGTH_MODERATE = 2;

    public static final int SIGNAL_STRENGTH_GOOD = 3;

    public static final int SIGNAL_STRENGTH_GREAT = 4;

    public static final int NUM_SIGNAL_STRENGTH_BINS = 5;
    
    SignalView mSignalView;
    TextView mNetDataView;

    public MyPhoneStateListener(Context ctx, SignalView signalView, TextView netDataView) {
        mContext = ctx;
        mSignalView = signalView;
        mNetDataView = netDataView;
    }

    @Override
    public void onDataActivity(int direction) {
        super.onDataActivity(direction);
        String dataDir = "";
        Log.v(TAG, "onDataActivity: " + direction);
        switch (direction) {
            case TelephonyManager.DATA_ACTIVITY_NONE:               
                break;
            case TelephonyManager.DATA_ACTIVITY_IN: 
                dataDir = "down";
                break;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                dataDir = "up";
                break;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                dataDir = "updown";
                break;
            default:
                break;
        }
        TelephonyManager manager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        if(dataDir == ""){
           
            int netType = manager.getNetworkType();
            String strNetType = "3G";
            switch(netType){
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    strNetType = "2G";
                    break;
                default :
                        break;
            }
//            if(state == TelephonyManager.DATA_CONNECTED){
//                if(mNetDataView != null){
//                    mNetDataView.setText("3G");
//                }
//                
//            }else{
//                
//            }
            final String finalNetType = strNetType;
            
            if(mNetDataView != null){
                mNetDataView.post(new Runnable() {                   
                    @Override
                    public void run() {
                      mNetDataView.setText(finalNetType);                        
                    }
                });
                
            }
            return;
        }
        int dataState = manager.getDataState();
        final String finalDataDir = dataDir;
        if(mNetDataView != null && dataState != TelephonyManager.DATA_DISCONNECTED){
            mNetDataView.post(new Runnable() {                   
                @Override
                public void run() {
                  mNetDataView.setText(finalDataDir);                        
                }
            });
            
        }
    }

    @Override
    public void onDataConnectionStateChanged(int state) {
        super.onDataConnectionStateChanged(state);
        String connectionState = "";
        switch (state) {
            case TelephonyManager.DATA_DISCONNECTED:
                Log.d(TAG, "Disconnected");
                connectionState = "";
                break;
            case TelephonyManager.DATA_CONNECTED:
                Log.d(TAG, "Connected");
                connectionState = "Connected";
                break;
            case TelephonyManager.DATA_CONNECTING:
                Log.d(TAG, "Connecting");
                connectionState = "Connecting";
                break;
            case TelephonyManager.DATA_SUSPENDED:
                Log.d(TAG, "Disconnecting");
                connectionState = "Disconnecting";
                break;
        }

        if (state == TelephonyManager.DATA_DISCONNECTED) {
            Log.v(TAG, "onDataConnectionStateChanged: " + state);
        }
       
//        if(state == TelephonyManager.DATA_CONNECTED){
//            if(mNetDataView != null){
//                mNetDataView.setText("3G");
//            }
//            
//        }else{
//            
//        }

    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        String phonestate;
        switch (serviceState.getState()) {
            case ServiceState.STATE_EMERGENCY_ONLY:
                phonestate = "STATE_EMERGENCY_ONLY";
                break;
            case ServiceState.STATE_IN_SERVICE:
                phonestate = "STATE_IN_SERVICE";
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                phonestate = "STATE_OUT_OF_SERVICE";
                if(mSignalView != null){
                    mSignalView.updateSignal(0);
                }
                break;
            case ServiceState.STATE_POWER_OFF:
                phonestate = "STATE_POWER_OFF";
                if(mSignalView != null){
                    mSignalView.updateSignal(0);
                }
                break;
            default:
                phonestate = "Unknown";
        }

        Log.v(TAG, "onServiceStateChanged");
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        Log.d(TAG, "SignalStrength = " + String.valueOf(signalStrength
                .getGsmSignalStrength()));
        int gsmLevel = getGsmLevel(signalStrength);
        if(mSignalView != null){
            mSignalView.updateSignal(gsmLevel);
        }

    }

    public int getGsmLevel(SignalStrength signalStrength) {
        int level;
        // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
        // asu = 0 (-113dB or less) is very weak
        // signal, its better to show 0 bars to the user in such cases.
        // asu = 99 is a special case, where the signal strength is unknown.
        int asu = signalStrength.getGsmSignalStrength();
        if (asu <= 2 || asu == 99)
            level = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        else if (asu >= 12)
            level = SIGNAL_STRENGTH_GREAT;
        else if (asu >= 8)
            level = SIGNAL_STRENGTH_GOOD;
        else if (asu >= 5)
            level = SIGNAL_STRENGTH_MODERATE;
        else
            level = SIGNAL_STRENGTH_POOR;
        Log.d(TAG, "getGsmLevel=" + level);
        return level;
    }
}
