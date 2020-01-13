package com.android.ct7liang.wificonfig.a;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class SystemUtils {

    public static boolean isConnectedWifi(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public static String getSSID(Context context){
        String ssid;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O||Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) {
            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = mWifiManager.getConnectionInfo();
            ssid = info.getSSID();

            return ssid.replace("\"", "");
        } else if (Build.VERSION.SDK_INT==Build.VERSION_CODES.O_MR1){
            ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            ssid = networkInfo.getExtraInfo();

            return ssid.replace("\"", "");
        }else{
            return "<unknown ssid>暂无Android9.0以上版本方案";
        }
    }

    public static String getBssid(Context context){

        WifiManager mWifiManager1 = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info1 = mWifiManager1.getConnectionInfo();
        Log.i("ct7liang", info1.getBSSID());
        return info1.getBSSID();

    }

}