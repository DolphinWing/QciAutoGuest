package com.quanta.pobu.apps.AutoGuest;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * Created by dolphin on 2013/6/18.
 *
 * Google AdMob for Android
 * https://developers.google.com/mobile-ads-sdk/docs/android/fundamentals?hl=zh-tw
 *
 * Android Admob advert in PreferenceActivity
 * http://stackoverflow.com/a/5850299
 *
 * Android + AdMob
 * http://imax-live.blogspot.tw/2012/11/android-sdk-eclipse-google-admob-ads.html
 */
public class AdPreference extends Preference {
    public AdPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdPreference(Context context) {
        super(context);
    }

    public final static String MY_AD_UNIT_ID = "MY_AD_UNIT_ID";

    @Override
    protected View onCreateView(ViewGroup parent) {
        // this will create the linear layout defined in ads_layout.xml
        View view = super.onCreateView(parent);

        // the context is a PreferenceActivity
        Activity activity = (Activity) getContext();

        // Create the adView
        AdView adView = new AdView(activity, AdSize.BANNER, MY_AD_UNIT_ID);

        ((LinearLayout) view).addView(adView);

        // Initiate a generic request to load it with an ad
        AdRequest request = new AdRequest();
        request.addTestDevice(AdRequest.TEST_EMULATOR);
        adView.loadAd(request);

        return view;
    }

}
