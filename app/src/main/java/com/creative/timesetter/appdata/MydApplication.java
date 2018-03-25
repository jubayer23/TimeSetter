package com.creative.timesetter.appdata;

import android.app.Application;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.creative.timesetter.sharedprefs.PrefManager;
import com.google.gson.Gson;


public class MydApplication extends Application {

    public static Gson gson;

    public static final String TAG = MydApplication.class.getSimpleName();

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static MydApplication mInstance;

    private static PrefManager pref;

    private float scale;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        this.scale = getResources().getDisplayMetrics().density;

        pref = new PrefManager(this);

        gson = new Gson();
    }

    public static synchronized MydApplication getInstance() {
        return mInstance;
    }


    public PrefManager getPrefManger() {
        if (pref == null) {
            pref = new PrefManager(this);
        }

        return pref;
    }

    public RequestQueue getRequestQueue() {

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
    public int getPixelValue(int dps) {
        int pixels = (int) (dps * scale + 0.5f);
        return pixels;
    }
}
