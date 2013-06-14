package com.quanta.pobu.apps.AutoGuest;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.DatePicker;

import java.util.Calendar;

public class AutoWiFiPreference extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener,
        DatePickerDialog.OnDateSetListener {

    private boolean DEBUG = false;

    public static final String ID_USER_INFO_USERID = "user_info_userid";
    public static final String ID_USER_INFO_PASSWD = "user_info_passwd";
    //private static final String ID_VERSION_CODE = "version_code";
    private static final String TAG = "AutoGuest";
    private static final String ID_VERSION_NAME = "version_name";
    private static final String ID_EXPIRED_DATE = "expired_date";//[0.0.8]++
    //private static final String ID_EXPIRED_DATE2 = "expired_date2";//[17]++
    private static final String ID_AUTO_LOGIN = "auto_login";//[13]++
    private static final String ID_CONNECT_NOW = "connect_now";
    //for honeycomb and newer (SDK >= 11)
    private boolean mOnSet = false;
    //[18]++ add check network status
    private WifiManager mWifiMgr;
    private IntentFilter intentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG)
                Log.d(TAG, "onReceive: " + action);
            if (action.endsWith(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo wifi =
                        (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (AutoWiFiReceiver.isConnected(context, wifi)) {
                    mPrefConnectNow.setEnabled(true);
                    mPrefConnectNow.setSummary(R.string.connect_now_summary_enable);
                } else {
                    mPrefConnectNow.setEnabled(false);
                    mPrefConnectNow.setSummary(R.string.connect_now_summary_disable);
                }
            }//set Connect Now preference enable only when connect to Guest
        }
    };

    private Preference mPrefConnectNow;

    /**
     * get package information
     *
     * @param context
     * @param cls
     * @return
     */
    public static PackageInfo getPackageInfo(Context context, Class<?> cls) {
        try {
            ComponentName comp = new ComponentName(context, cls);
            return context.getPackageManager().getPackageInfo(
                    comp.getPackageName(), 0);
            //return pinfo;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * get password invalid date
     *
     * @param context
     * @return
     */
    public static Calendar getExpiredDate(Context context) {
        Calendar cal = Calendar.getInstance();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        cal.setTimeInMillis(settings.getLong(ID_EXPIRED_DATE, 0));
        return cal;
    }

    /**
     * set password invalid date
     *
     * @param context
     * @param year
     * @param monthOfYear
     * @param dayOfMonth
     */
    public static void setExpiredDate(Context context, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthOfYear);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        //Log.d(TAG, cal.getTime().toString());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        //        if (settings.contains(AutoWiFiPreference.ID_EXPIRED_DATE)) {
        if (settings != null) {
            Editor editor = settings.edit();
            editor.putLong(ID_EXPIRED_DATE, cal.getTimeInMillis());
            editor.commit();
        }
    }

    /**
     * check if the auto login is enabled
     *
     * @param context
     * @return
     */
    public static boolean isAutoLoginEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(ID_AUTO_LOGIN, true);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DEBUG = getResources().getBoolean(R.bool.pref_engineer_mode);
        addPreferencesFromResource(R.xml.preference);

        final PackageInfo pinfo = getPackageInfo(getBaseContext(), AutoWiFiPreference.class);
        findPreference(ID_VERSION_NAME).setSummary(
                String.format("%s (revision %d)", pinfo.versionName, pinfo.versionCode));
        //[0.1.0]--
        //this.findPreference(ID_VERSION_CODE).setSummary(
        //    String.format("Revision %d", pinfo.versionCode));

        //[0.0.5]update preference screen
        findPreference(ID_USER_INFO_PASSWD).setOnPreferenceChangeListener(this);
        findPreference(ID_USER_INFO_USERID).setOnPreferenceChangeListener(this);
        //[13]++
        findPreference(ID_AUTO_LOGIN).setOnPreferenceChangeListener(this);
        //    //[17]++
        //    DatePreference date = (DatePreference) this.findPreference(ID_EXPIRED_DATE2);
        //    if (date != null) {
        //        date.setOnPreferenceChangeListener(this);
        //        date.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        //
        //            @Override
        //            public boolean onPreferenceClick(Preference preference)
        //            {
        //                Log.d(TAG, "onPreferenceClick " + preference.toString());
        //                return true;
        //            }
        //        });
        //    }

        ////[18]++ add check Wi-Fi status
        mPrefConnectNow = findPreference(ID_CONNECT_NOW);
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi.isAvailable() && AutoWiFiReceiver.isConnected(this, wifi)) {
            mPrefConnectNow.setEnabled(true);
            mPrefConnectNow.setSummary(R.string.connect_now_summary_enable);
        } else {
            mPrefConnectNow.setEnabled(false);
            mPrefConnectNow.setSummary(R.string.connect_now_summary_disable);
        }

        //[19]++
        intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        updateScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);

        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (preference.getKey().equals(ID_CONNECT_NOW)) {
            Intent intent = new Intent(AutoWiFiPreference.this, AutoGuestActivity.class);
            //intent.setClass(this, AutoGuestActivity.class);
            startActivity(intent);
            if (DEBUG) Log.v(TAG, ID_CONNECT_NOW);
            AutoWiFiPreference.this.finish();//close the setup activity
            return false;
        } else if (preference.getKey().equals(ID_EXPIRED_DATE)) {//set expired date
            final Context context = getBaseContext();
            Calendar expired = getExpiredDate(context);
            //since r9[0.1.0], we have a new data called expired date
            //need to check if this form is not set, by default it will be 1970/01/01
            //so we can check for expired date is recent days only
            if (expired.get(Calendar.YEAR) < 2013) {
                expired = Calendar.getInstance();//use today as default start
            }
            final DatePickerDialog d =
                    new DatePickerDialog(AutoWiFiPreference.this, this, expired.get(Calendar.YEAR),
                            expired.get(Calendar.MONTH), expired.get(Calendar.DAY_OF_MONTH));
            //calendar.get(Calendar.YEAR),
            //calendar.get(Calendar.MONTH),
            //calendar.get(Calendar.DAY_OF_MONTH));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                d.setButton(DatePickerDialog.BUTTON_POSITIVE,
                        context.getString(R.string.action_set), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Log.d(TAG, "action_set");
                        mOnSet = true;
                        //[18]-- use DatePickerDialog.onDateSet method
                        //DatePicker picker = d.getDatePicker();
                        //setExpiredDate(context, picker.getYear(), picker.getMonth(),
                        //        picker.getDayOfMonth());
                        //updateScreen();
                        dialog.dismiss();
                    }
                });
                d.setButton(DatePickerDialog.BUTTON_NEGATIVE,
                        context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Log.d(TAG, "cancel");
                        mOnSet = false;
                        dialog.dismiss();
                    }
                });
            }
            d.show();
        }

        boolean r = super.onPreferenceTreeClick(preferenceScreen, preference);

        updateScreen();

        return r;
    }

    private void updateScreen() {
        //[0.0.4]
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.contains(AutoWiFiPreference.ID_USER_INFO_USERID)) {
            String userid = settings.getString(AutoWiFiPreference.ID_USER_INFO_USERID, "");
            if (userid != null && userid != "")
                this.findPreference(ID_USER_INFO_USERID).setSummary(userid);
            else
                //[0.0.5]
                this.findPreference(ID_USER_INFO_USERID).setSummary("");
        }

        if (settings.contains(AutoWiFiPreference.ID_USER_INFO_PASSWD)) {
            String passwd =
                    settings.getString(AutoWiFiPreference.ID_USER_INFO_PASSWD, "");
            //[0.0.5]jimmy++ show password directly
            //String passwd1 = "";
            if (passwd != null && passwd != "") {
                //	for (int i = 0; i < passwd.length(); i++)
                //		passwd1 += "*";
                this.findPreference(ID_USER_INFO_PASSWD).setSummary(passwd);
            }
            //this.findPreference(ID_USER_INFO_PASSWD).setSummary(passwd1);
            else
                //[0.0.5]
                this.findPreference(ID_USER_INFO_PASSWD).setSummary("");
        }

        //[0.0.8]++ notification for expired date
        if (settings.contains(AutoWiFiPreference.ID_EXPIRED_DATE)) {
            Calendar cal = getExpiredDate(this);//Calendar.getInstance();
            //cal.setTimeInMillis(settings.getLong(ID_EXPIRED_DATE, 0));
            this.findPreference(ID_EXPIRED_DATE).setSummary(
                    DateFormat.getLongDateFormat(getBaseContext()).format(cal.getTime()));
            //String.format("%tB %te, %tY", cal, cal, cal));
            //  this.findPreference(ID_EXPIRED_DATE2).setSummary(
            //      DateFormat.getLongDateFormat(getBaseContext()).format(cal.getTime()));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(ID_AUTO_LOGIN)) {//to enable/disable receiver
            setPackageEnabled(getBaseContext(), Boolean.parseBoolean(newValue.toString()));
            //        } else if (preference.getKey().equals(ID_EXPIRED_DATE2)) {
            //            Log.d(TAG, "preference = " + preference.toString());
            //            Log.d(TAG, "  newValue = " + newValue.toString());
        } else {//for other preference to update the UI
            preference.setSummary(newValue.toString());
        }
        return true;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        //[17]--//do nothing
        //Log.d(TAG, String.format("onDateSet %s", mOnSet));
        //[17]++ save to preference
        //[18]++ add SDK<11 support (SDK<11 have its buttons and calls onDateSet when SET)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || mOnSet) {
            if (DEBUG)
                Log.d(TAG, String.format("%04d/%02d/%02d", year, monthOfYear + 1, dayOfMonth));
            setExpiredDate(getBaseContext(), year, monthOfYear, dayOfMonth);
        }
        updateScreen();
    }

    /**
     * set receiver enable/disable
     *
     * @param context
     * @param enabled
     */
    private void setPackageEnabled(Context context, boolean enabled) {
        //Context context = getBaseContext();
        final ComponentName activity = new ComponentName(context, AutoWiFiReceiver.class);
        final PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(activity,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        if (DEBUG)
            Log.v(TAG, String.format("===> set %s", (enabled ? "enable" : "disable")));
        // AcerPreset.this.finish();
    }
}
