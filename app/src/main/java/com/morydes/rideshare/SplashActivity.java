package com.morydes.rideshare;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.morydes.rideshare.alertbanner.AlertDialogForAnything;
import com.morydes.rideshare.appdata.GlobalAppAccess;
import com.morydes.rideshare.appdata.MydApplication;
import com.morydes.rideshare.model.Time;
import com.morydes.rideshare.model.TimeInfo;
import com.morydes.rideshare.model.TimeLocation;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends BaseActivity {


    public static final String KEY_IS_USER_HAS_PIN = "user_has_pin";
    public static final String KEY_TIMELOCATIONS = "timelocations";
    public static final String KEY_ORIGINAL_TIMELOCATIONS = "original_timelocations";
    public static final String KEY_ERROR = "error";
    public static final String ERROR_TYPE_SUCCESS = "success";
    public static final String ERROR_TYPE_SERVER_PROBLEM = "server_problem";
    public static final String ERROR_TYPE_NETWORK_PROBLEM = "network_problem";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //menyembunyikan title bar di layar acitivy
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        //membuat activity menjadi fullscreen
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //WindowManager.LayoutParams.FLAG_FULLSCREEN);

        /** Sets a layout for this activity */
        setContentView(R.layout.activity_splash);

        Intent intent = getIntent();
        double alarm_lat = 0;
        double alarm_lang = 0;
        if (intent.getStringExtra(GlobalAppAccess.KEY_CALL_FROM) != null &&
                !intent.getStringExtra(GlobalAppAccess.KEY_CALL_FROM).isEmpty() &&
                intent.getStringExtra(GlobalAppAccess.KEY_CALL_FROM).equals(GlobalAppAccess.TAG_ALARM_RECEIVER)) {
            alarm_lat = intent.getDoubleExtra("lat", 0);
            alarm_lang = intent.getDoubleExtra("lang", 0);
        }else{
            alarm_lat = 0;
            alarm_lang = 0;
        }

        sendRequestForGetTimes(GlobalAppAccess.URL_GET_TIMES, alarm_lat, alarm_lang);
        //new LoadViewTask().execute();



    }


    public void sendRequestForGetTimes(String url, final double lat, final double lang) {

        //url = url + "?" + "email=" + email + "&password=" + password;
        // TODO Auto-generated method stub
       // showProgressDialog("Loading..", true, false);

        final StringRequest req = new StringRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("DEBUG",response);


                        dismissProgressDialog();


                        TimeInfo timeInfo = MydApplication.gson.fromJson(response, TimeInfo.class);

                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.putExtra("lat", lat);
                        intent.putExtra("lang", lang);

                        List<TimeLocation> timeLocations = new ArrayList<>();
                        boolean isUserHasAlreadyPin = false;


                        if (timeInfo.getResult()) {
                            intent.putExtra(KEY_ERROR, ERROR_TYPE_SUCCESS);
                            timeLocations.addAll(timeInfo.getTimeLocations());
                            for(TimeLocation timeLocation: timeLocations){
                                for(Time time: timeLocation.getTimes()){
                                    if (MydApplication.deviceImieNumber.equals(time.getDeviceId())){
                                        isUserHasAlreadyPin = true;
                                        break;
                                    }
                                }
                                if(isUserHasAlreadyPin)break;
                            }
                           // checkAllPermissionsAndSetUpMap();

                        }else{
                            intent.putExtra(KEY_ERROR, ERROR_TYPE_SERVER_PROBLEM);
                        }
                        /*else {
                            AlertDialogForAnything.showAlertDialogWhenComplte(SplashActivity.this, "Error", "Server problem while loading the timeLocations!", false);
                        }*/


                        intent.putExtra(KEY_IS_USER_HAS_PIN, isUserHasAlreadyPin);
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(KEY_TIMELOCATIONS, (ArrayList<? extends Parcelable>) timeLocations);
                        //intent.putParcelableArrayListExtra(KEY_TIMELOCATIONS, (ArrayList<? extends Parcelable>) timeLocations);
                        intent.putExtras(bundle);

                        startActivity(intent);


                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();

                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("lat", lat);
                intent.putExtra("lang", lang);
                intent.putExtra(KEY_ERROR, ERROR_TYPE_NETWORK_PROBLEM);
                startActivity(intent);

               /* AlertDialogForAnything.showAlertDialogWhenComplte(MainActivity.this, "Error", "Network problem. please try again!", false);*/
               /* String response = getTimesResponseDummy();

                TimeInfo timeInfo = MydApplication.gson.fromJson(response, TimeInfo.class);

                if (timeInfo.getResult()) {
                    timeLocations.clear();
                    timeLocations.addAll(timeInfo.getTimeLocations());
                    checkAllPermissionsAndSetUpMap();

                }*/
            }
        });

        req.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // TODO Auto-generated method stub
        MydApplication.getInstance().addToRequestQueue(req);
    }

}