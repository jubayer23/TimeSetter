package com.morydes.rideshare.sharedprefs;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.morydes.rideshare.BuildConfig;
import com.google.gson.Gson;


/**
 * Created by jubayer on 6/6/2017.
 */


public class PrefManager {
    private static final String TAG = PrefManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    private static Gson GSON = new Gson();
    // Sharedpref file name
    private static final String PREF_NAME = BuildConfig.APPLICATION_ID;

    private static final String KEY_NUM_OF_TIME_USER_SET_ALARM = "num_of_time_user_set_alarm";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);

    }

    public void setNumberOfTimeUserSetAlarm(int obj) {
        editor = pref.edit();

        editor.putInt(KEY_NUM_OF_TIME_USER_SET_ALARM, obj);

        // commit changes
        editor.commit();
    }
    public int getNumberOfTimeUserSetAlarm() {
        return pref.getInt(KEY_NUM_OF_TIME_USER_SET_ALARM,0);
    }





}