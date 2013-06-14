package com.quanta.pobu.apps.AutoGuest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Calendar;

public class AutoGuestActivity extends Activity {
    private static final String TAG = "AutoGuestActivity";

    private WebView wv1;
    private boolean bSubmit = false;
    private boolean bBackEnabled = false;//[21]++
    private String mUserId = null;
    private String mPasswd = null;
    private String mPortalServer = "";

    // frameworks/base/wifi/java/android/net/wifi/WifiWatchdogStateMachine.java
    private static final String WALLED_GARDEN_NOTIFICATION_ID = "WifiWatchdog.walledgarden";

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {//[16]++ try to catch the NullPointerException
            final Context context = getBaseContext();
            //[0.1.0] check expired date first
            //final Context context = getBaseContext();
            Calendar expired = AutoWiFiPreference.getExpiredDate(context);

            Log.i(TAG, "expired at " + expired.getTime().toString());
            if (expired.get(Calendar.YEAR) < 2013)
                Log.w(TAG, "no expired date set");
            //since r9[0.1.0], we have a new data called expired date
            //need to check if this form is not set, by default it will be 1970/01/01
            //so we can check for expired date is recent days only
            if (expired.get(Calendar.YEAR) >= 2013 && expired.before(Calendar.getInstance())) {
                Log.w(TAG, "Your password is expired.");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    getActionBar().setDisplayShowTitleEnabled(false);
                    getActionBar().setDisplayShowHomeEnabled(false);
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                    getActionBar().setDisplayUseLogoEnabled(false);
                }//only do this on ICS and later

                showWarningDialog();
            } else { //[0.1.0]++ check expired date, only load when no expired
                //mHandler.sendEmptyMessageDelayed(0, 10);//200
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        load_page(context);
                    }
                });
            }
        } catch (Exception e) {
            //to solve unknown issue that found in [14][15] on UI2A
            //read Version.txt for more information
            showWarningDialog();
            Log.e(TAG, "treat as expired! " + e.getMessage());
        }
    }

    private void showWarningDialog() {
        new AlertDialog.Builder(AutoGuestActivity.this)
                .setTitle(R.string.expired_title)
                .setMessage(R.string.expired_message)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.action_close),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                AutoGuestActivity.this.finish();
                            }
                        })
                .setPositiveButton(getString(R.string.action_enter_new),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(AutoGuestActivity.this,
                                        AutoWiFiPreference.class));
                                AutoGuestActivity.this.finish();
                            }
                        }).show();
        //Log.d(TAG, "warning dialog is shown");
    }

    //    Handler mHandler = new Handler() {
    //
    //        @Override
    //        public void handleMessage(Message msg)
    //        {
    //            load_page();
    //            //super.handleMessage(msg);
    //        }
    //    };

    @SuppressLint("SetJavaScriptEnabled")
    private void load_page(Context context) {
        //[12]++ no to change the preference or process
        findViewById(android.R.id.text1).setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            }
        });//[12]++

        mPortalServer = getString(R.string.portal_server);
        //Log.i(TAG, "Portal Server = " + mPortalServer);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (settings.contains(AutoWiFiPreference.ID_USER_INFO_USERID)) {
            mUserId = settings.getString(AutoWiFiPreference.ID_USER_INFO_USERID, "");
        }
        if (settings.contains(AutoWiFiPreference.ID_USER_INFO_PASSWD)) {
            mPasswd = settings.getString(AutoWiFiPreference.ID_USER_INFO_PASSWD, "");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            if (mUserId == null || mUserId.trim().isEmpty()
                    || mPasswd == null || mPasswd.trim().isEmpty()) {
                Log.w(TAG, "no info, no connect");

                //          Intent intent = new Intent();
                //          intent.setClass(this, AutoWiFiPreference.class);
                //          startActivity(intent);//show preference page to input data
                Toast.makeText(AutoGuestActivity.this, getString(R.string.please_set_preference),
                        Toast.LENGTH_SHORT).show();

                this.finish();//do nothing...
                return;
            } else {
                //this is OK, continue
            }
        } else if (mUserId == null || mUserId.equalsIgnoreCase("")
                || mPasswd == null || mPasswd.equalsIgnoreCase("")) {//for SDK <= 9
            Log.w(TAG, "no info, no connect");
            Toast.makeText(AutoGuestActivity.this,
                    getString(R.string.please_set_preference),
                    Toast.LENGTH_SHORT).show();
            this.finish();//do nothing...
            return;
        }

        //Log.i(TAG, "UserID = " + mUserId);
        //Log.i(TAG, "Passwd = " + mPasswd);

        /* WebView */
        wv1 = (WebView) findViewById(R.id.webView1);
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.getSettings().setSupportZoom(false);
        wv1.getSettings().setBlockNetworkImage(true);//[0.1.2]++
        //wv1.getSettings().setAppCacheEnabled(false);
        wv1.getSettings().setLoadsImagesAutomatically(false);
        //wv1.getSettings().setSavePassword(false);
        //wv1.getSettings().setSaveFormData(false);
        wv1.getSettings().setRenderPriority(RenderPriority.HIGH);
        wv1.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        //		wv1.setWebViewClient(new WebViewClient() {
        //			public void onReceivedError(WebView view, int errorCode,
        //					String description, String failingUrl)
        //			{
        //				Toast.makeText(AutoGuestActivity.this, "Oh no! " + description,
        //					Toast.LENGTH_SHORT).show();
        //			}
        //		});
        wv1.setWebViewClient(new MyWebViewClient(context));

        wv1.addJavascriptInterface(new JavaScriptCallFunc(), "Quanta");
        Log.i(TAG, "start login process");
        wv1.loadUrl("http://www.google.com");
    }

    @Override
    public void onBackPressed() {
        //[12]-- this.finish();
        if (bBackEnabled) super.onBackPressed();
    }

    private final class MyWebViewClient extends WebViewClient {
        Context mContext;

        public MyWebViewClient(Context context) {
            mContext = context;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG, "onPageStarted: " + url);
            bBackEnabled = false;//[21]++
            //[0.0.5]jimmy-- check at onPageFinished
            //			if (!url.contains(mPortalServer + "/cgi-bin")) {
            //				AutoGuestActivity.this.finish();
            //				Toast.makeText(AutoGuestActivity.this, "Login Success!",
            //					Toast.LENGTH_SHORT).show();
            //			}
            mTimeoutHandler.sendEmptyMessageDelayed(0, 10000);//[14]++ add timeout handler
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "onPageFinished: " + url);
            mTimeoutHandler.removeMessages(0);//[14]++ add timeout handler
            if (url.contains(mPortalServer + "/cgi-bin")) {
                wv1.loadUrl("javascript:document.getElementById('user').value='"
                        + mUserId + "';");
                wv1.loadUrl("javascript:document.getElementById('password').value='"
                        + mPasswd + "';");
                if (url.contains("cmd=login")) {
                    if (!bSubmit) {
                        bSubmit = true;
                        wv1.loadUrl("javascript:document.getElementById('regform').submit();");
                    } else {//[0.0.6]got error messages
                        Log.d(TAG, "already do the login but failed");
                        //wv1.addJavascriptInterface(new JavaScriptCallFunc(),
                        //	"Quanta");
                        wv1.loadUrl("javascript:Quanta.onLoad(document.getElementById('errorbox').innerText);");
                        bBackEnabled = true;//[21]++
                    }
                    //Log.v(TAG, "load javascript");
                } else {
                    Log.d(TAG, "no login?");
                    wv1.loadUrl("javascript:Quanta.onLoad(document.getElementById('errorbox').innerText);");
                    bBackEnabled = true;//[21]++
                }
            } else {//close this activity
                bBackEnabled = true;//[21]++
                //[0.0.5]jimmy++ check at onPageFinished
                Toast.makeText(AutoGuestActivity.this, getString(R.string.login_success),
                        Toast.LENGTH_LONG).show();
                Log.i(TAG, "Login Success!");

                try {//[12]++ try to clear the Watchdog notification
                    NotificationManager notificationManager =
                            (NotificationManager) mContext
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(WALLED_GARDEN_NOTIFICATION_ID, 1);
                } catch (Exception e) {
                    Log.e(TAG, "clear notification: " + e.getMessage());
                    e.printStackTrace();
                }

                AutoGuestActivity.this.finish();
            }
            super.onPageFinished(view, url);
        }
    }

    private final class JavaScriptCallFunc {
        @SuppressWarnings("unused")
        public void onLoad(String errMsg) {
            Log.e(TAG, "msg: " + errMsg);
            Toast.makeText(AutoGuestActivity.this, errMsg, Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setClass(AutoGuestActivity.this, AutoWiFiPreference.class);
            startActivity(intent);
            AutoGuestActivity.this.finish();
        }
    }

    //[14]++ add timeout handler
    private Handler mTimeoutHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(AutoGuestActivity.this, getString(R.string.no_response_shutdown),
                    Toast.LENGTH_LONG).show();
            AutoGuestActivity.this.finish();
            super.handleMessage(msg);
        }
    };
}
