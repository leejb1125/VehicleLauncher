package com.vehicle.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetConnectionReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent arg1) {

        String action = arg1.getAction();
            // 获得网络连接服务
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo info = connManager.getActiveNetworkInfo();

          

    }
}
