package com.quanta.pobu.apps.AutoGuest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AutoWiFiReceiver extends BroadcastReceiver {
    private static final String TAG = "AutoWiFiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //Log.d(TAG, action);

        if (action.endsWith(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            //if (netInfo != null) {
            //    Log.d(TAG, netInfo.toString());
            //}

            //String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
            //Log.d(TAG, "BSSID: " + bssid);

            if (AutoWiFiPreference.isAutoLoginEnabled(context)//[24]++
                    && isConnected(context, netInfo)) {
                Log.i(TAG, "start login process");
                Intent intent1 = new Intent();
                intent1.setClass(context, AutoGuestActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            }
            //} else if (action.endsWith(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            //    //Log.d(TAG, "SUPPLICANT_STATE_CHANGED_ACTION");
            //} else if (action.endsWith(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            //    int state =
            //        intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
            //			WifiManager.WIFI_STATE_UNKNOWN);
            //    //Log.d(TAG, String.format("  state = %d", state));
        } //else if (action.endsWith(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
        //    boolean connected =
        //            intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
        //    Log.d(TAG, String.format("SUPPLICANT connected = %s", connected));
        //}

        if (!AutoWiFiPreference.isAutoLoginEnabled(context))//[24]++ set disable if no auto
            AutoWiFiPreference.setPackageEnabled(context, false);
    }

    public static boolean isConnected(Context context, NetworkInfo netInfo) {
        //if (context.getResources().getBoolean(R.bool.pref_engineer_mode)) {
        //    return true;
        //}
        if (netInfo != null && netInfo.isConnected()) {//[25]++ fix possible NullPointerException
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            if (wifiInfo != null) {
                String wifiSsid = wifiInfo.getSSID();
                //[13]dolphin++ 2013-03-18, seems 4.2 changed?
                wifiSsid = wifiSsid.replace("\"", "");
                //Log.v(TAG, " SSID: " + wifiSsid);
                //Log.v(TAG, "  MAC: " + wifiInfo.getMacAddress());

                String[] lists =
                        context.getResources().getStringArray(R.array.portal_ssid_list);
                for (String ssid : lists) {
                    //Log.d(TAG, ssid);//[13]dolphin++ 2013-03-18, seems 4.2 changed?
                    if (ssid.equals(wifiSsid)) {
                        return true;//already connected
                    }
                }//check the list
            }//has WifiInfo
        }//only do when connected
        return false;
    }

    public static boolean checkNetworkAvailable(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi.isAvailable()) {
            //Toast.makeText(this, "Wifi" , Toast.LENGTH_LONG).show();
            Log.v(TAG, "Wi-Fi is available");
            return true;
        }

        return false;
    }
}
