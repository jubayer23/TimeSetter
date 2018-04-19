package com.morydes.rideshare;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.morydes.rideshare.Utility.CommonMethods;
import com.morydes.rideshare.Utility.DeviceInfoUtils;
import com.morydes.rideshare.Utility.GpsEnableTool;
import com.morydes.rideshare.Utility.LastLocationOnly;
import com.morydes.rideshare.Utility.RunnTimePermissions;
import com.morydes.rideshare.Utility.UserLastKnownLocation;
import com.morydes.rideshare.adapter.TimeAdapter;
import com.morydes.rideshare.alertbanner.AlertDialogForAnything;
import com.morydes.rideshare.appdata.GlobalAppAccess;
import com.morydes.rideshare.appdata.MydApplication;
import com.morydes.rideshare.billingUtil.IabBroadcastReceiver;
import com.morydes.rideshare.fragment.DatePickerFragment;
import com.morydes.rideshare.fragment.TimePickerFragment;
import com.morydes.rideshare.model.NearByPlaceInfo;
import com.morydes.rideshare.model.Time;
import com.morydes.rideshare.model.TimeLocation;
import com.morydes.rideshare.model.TimeInfo;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, View.OnClickListener,CompoundButton.OnCheckedChangeListener,
IabBroadcastReceiver.IabBroadcastListener{
    private GoogleMap mMap;

    List<Marker> markers = new ArrayList<>();

    LinearLayout ll_bottom_sheet;

    BottomSheetBehavior sheetBehavior;

    private List<TimeLocation> timeLocations = new ArrayList<>();
    private List<TimeLocation> originalTimeLocations = new ArrayList<>();

    //bottom sheet ui item
    private TextView tv_num_of_time;
    private Button btn_set_time;
    private RecyclerView recyclerView;
    private TimeAdapter timeAdapter;
    private TextView tv_no_time_set_warning;
    private TextView tv_location_name;
    //private List<Time> times = new ArrayList<>();


    private Marker userClickMarker;
    /*
    * This variable is very important
    * */
    public static int selectedTimePosition = 0;
    private Marker prevClickedMarker;
    private HashMap<Marker, Integer> hashMapMarker = new HashMap<>();
    LatLngBounds.Builder builder;


    private double alarm_lat, alarm_lang;

    private CheckBox ch_only_today;

    private AdView adview_banner;
    private InterstitialAd mInterstitialAd;

    private static final int botomSheetPeekHeight = 430;

    private BillingHelper billingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        billingHelper = new BillingHelper(this);
        billingHelper.initiateBillingConfiguration();

        initToolbar();

        init();

        initBottomSheetListAdapter();

        loadAdview();

        if (getIntent().getStringExtra(GlobalAppAccess.KEY_CALL_FROM) != null &&
                !getIntent().getStringExtra(GlobalAppAccess.KEY_CALL_FROM).isEmpty() &&
                getIntent().getStringExtra(GlobalAppAccess.KEY_CALL_FROM).equals(GlobalAppAccess.TAG_ALARM_RECEIVER)) {
            alarm_lat = getIntent().getDoubleExtra("lat", 0);
            alarm_lang = getIntent().getDoubleExtra("lang", 0);
        } else {
            alarm_lat = 0;
            alarm_lang = 0;
        }

        if (savedInstanceState == null) {


            sendRequestForGetTimes(GlobalAppAccess.URL_GET_TIMES);

        }
        //https://blog.mapbox.com/how-to-build-a-location-picker-for-your-app-8e61be7fc9cc
        //https://github.com/mapbox/mapbox-gl-native/blob/dfd0b105cd0e45407f4df5e0e8c4f2d2739df83e/platform/android/MapboxGLAndroidSDKTestApp/src/main/java/com/mapbox/mapboxsdk/testapp/activity/navigation/LocationPickerActivity.java
    }

    private void checkAllPermissionsAndSetUpMap() {
        /**
         * This is marshmallow runtime Permissions
         * It will ask user for grand permission in queue order[FIFO]
         * If user gave all permission then check whether user device has google play service or not!
         * NB : before adding runtime request for permission Must add manifest permission for that
         * specific request
         * */
        if (RunnTimePermissions.requestForAllRuntimePermissions(this)) {
            if (!DeviceInfoUtils.isGooglePlayServicesAvailable(MainActivity.this)) {
                AlertDialogForAnything.showAlertDialogWhenComplte(this, "Warning", "This app need google play service to work properly. Please install it!!", false);
            }

            setUpMap();
        }
    }

    private void init() {

        ll_bottom_sheet = (LinearLayout) findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(ll_bottom_sheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        //bottom sheet
        tv_num_of_time = (TextView) findViewById(R.id.tv_num_of_time);
        tv_location_name = (TextView) findViewById(R.id.tv_location_name);
        tv_location_name.setVisibility(View.GONE);
        tv_no_time_set_warning = (TextView) findViewById(R.id.tv_no_time_set_warning);
        tv_no_time_set_warning.setVisibility(View.GONE);
        btn_set_time = (Button) findViewById(R.id.btn_set_time);
        btn_set_time.setOnClickListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        ch_only_today = (CheckBox) findViewById(R.id.ch_only_today);
        ch_only_today.setOnCheckedChangeListener(this);

    }

    private void initBottomSheetListAdapter() {

        timeAdapter = new TimeAdapter(this, timeLocations);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(timeAdapter);
    }

    private void setUpMap() {
        showProgressDialog("please wait..", true, false);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        dismissProgressDialog();
        if (mMap != null) {
            return;
        }
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            RunnTimePermissions.requestForAllRuntimePermissions(this);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        LastLocationOnly lastLocationOnly = new LastLocationOnly(this);
        if (lastLocationOnly.canGetLocation()) {


            placeAllMarkersAndPlacementCamera(lastLocationOnly.getLatitude(), lastLocationOnly.getLongitude());

        } else {
            GpsEnableTool gpsEnableTool = new GpsEnableTool(this);
            gpsEnableTool.enableGPs();
        }

    }

    protected void placeAllMarkersAndPlacementCamera(double lat, double lang) {

        builder = new LatLngBounds.Builder();
        placeAllMarkerOfListInMap();

        if (alarm_lat != 0 && alarm_lang != 0) {
            LatLng latLng = new LatLng(alarm_lat, alarm_lang);
            zoomToSpecificLocation(latLng);
            alarm_lat = 0;
            alarm_lang = 0;
        } else {
            LatLng latLng = new LatLng(lat, lang);
            builder.include(latLng);
            moveMapCameraToLoadAllMarker();
        }



       /* if (mMap == null) return;
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lang))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
    }

    private void placeAllMarkerOfListInMap() {
        int count = 0;
        mMap.clear();
        hashMapMarker.clear();
        if(builder == null){
            builder = new LatLngBounds.Builder();
        }

        for (TimeLocation timeLocation : timeLocations) {
            LatLng latLng = new LatLng(timeLocation.getLat(), timeLocation.getLang());
            Marker marker = mMap.addMarker(getTimeMarker(latLng));
            builder.include(latLng);
            hashMapMarker.put(marker, count);
            count++;
        }
    }

    private void placeLastMarkerOfListInMap() {
        int lastPosition = timeLocations.size() - 1;
        TimeLocation timeLocation = timeLocations.get(lastPosition);
        LatLng latLng = new LatLng(timeLocation.getLat(), timeLocation.getLang());
        Marker marker = mMap.addMarker(getTimeMarker(latLng));
        hashMapMarker.put(marker, lastPosition);
        onMarkerClick(marker);
    }

    private void moveMapCameraToLoadAllMarker() {
        LatLngBounds bounds = builder.build();
        int padding = 80; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }

    protected void zoomToSpecificLocation(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)      // Sets the center of the map to location user
                .zoom(20)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        sentRequestToCheckIfItIsValidPOI(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (prevClickedMarker != null && marker.equals(prevClickedMarker)) {
            toggleBottomSheet();
            return false;
        }
        prevClickedMarker = marker;

        //times.clear();
        if (userClickMarker != null && marker.equals(userClickMarker)) {
            selectedTimePosition = -1;
            updateBottomSheetUi(0);
        } else {
            selectedTimePosition = hashMapMarker.get(marker);
            TimeLocation timeLocation = timeLocations.get(selectedTimePosition);
            if (timeLocation.getTimes().size() > 0) {
                updateBottomSheetUi(timeLocation.getTimes().size());
            } else {
                updateBottomSheetUi(0);
            }
            //times.addAll(timeLocation.getTimes());
        }
        timeAdapter.notifyDataSetChanged();

        sheetBehavior.setPeekHeight(botomSheetPeekHeight);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

            Log.d("DEBUG","called check box");
            updateTimeLocationBasedOnTodayDate(isChecked);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.btn_set_time) {
            if (prevClickedMarker == null) return;
            double lat = prevClickedMarker.getPosition().latitude;
            double lang = prevClickedMarker.getPosition().longitude;
            if (userClickMarker != null && userClickMarker.equals(prevClickedMarker)) {
                showTimeSetDialog(lat, lang);
            } else {
                showTimeSetDialog(lat, lang);
            }

        }
    }

    public void toggleBottomSheet() {

        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            sheetBehavior.setPeekHeight(botomSheetPeekHeight);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setPeekHeight(botomSheetPeekHeight);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void updateTimeLocationBasedOnTodayDate(boolean isChecked){
        showProgressDialog("Loading...",true,false);

        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        timeLocations.clear();

        if(isChecked){
            Calendar calender_current = Calendar.getInstance();
            Calendar calendar_time_location = Calendar.getInstance();



            for(TimeLocation timeLocation: originalTimeLocations){
                for(Time time: timeLocation.getTimes()){
                    Date date_time_location = CommonMethods.convertStringToDate(time.getTime(),"dd/MM/yyyy HH:mm:ss.SSS");
                    calendar_time_location.setTime(date_time_location);

                    boolean sameDay = calender_current.get(Calendar.YEAR) == calendar_time_location.get(Calendar.YEAR) &&
                            calender_current.get(Calendar.DAY_OF_YEAR) == calendar_time_location.get(Calendar.DAY_OF_YEAR);

                    if(sameDay){
                        timeLocations.add(timeLocation);
                        break;
                    }
                }
            }

        }else{
            timeLocations.addAll(originalTimeLocations);
        }




        builder = null;
        placeAllMarkerOfListInMap();

        dismissProgressDialog();


        if(isChecked && timeLocations.isEmpty()){
            AlertDialogForAnything.showNotifyDialog(this,AlertDialogForAnything.ALERT_TYPE_ERROR,"No point for today!");
            return;
        }

        moveMapCameraToLoadAllMarker();

    }

    private MarkerOptions getUserClickMarkerOptions(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .title("To set time click on the bottom Set Time button!")
                .draggable(true);
    }

    private MarkerOptions getUserClickMarkerOptions(LatLng position, String locationName) {
        return new MarkerOptions()
                .position(position)
                .title(locationName)
                .snippet("To set time click on the bottom Set Time button!")
                .draggable(true);
    }

    private MarkerOptions getTimeMarker(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
    }

    public void removeMarkerAndSetAsUserClickedMarker() {
        showProgressDialog("Loading..", true, false);
        timeLocations.remove(selectedTimePosition);
        placeAllMarkerOfListInMap();
        userClickMarker = mMap.addMarker(getUserClickMarkerOptions(prevClickedMarker.getPosition()));
        selectedTimePosition = -1;
        dismissProgressDialog();
        updateBottomSheetUi(0);
        sheetBehavior.setPeekHeight(botomSheetPeekHeight);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void updateBottomSheetUi(int num_of_time_set) {
        if (num_of_time_set > 0) {
            tv_num_of_time.setText(num_of_time_set + " time set!");
            recyclerView.setVisibility(View.VISIBLE);
            tv_no_time_set_warning.setVisibility(View.GONE);
        } else {
            tv_num_of_time.setText("No time set!");
            recyclerView.setVisibility(View.GONE);
            tv_no_time_set_warning.setVisibility(View.VISIBLE);
        }
    }

    private void showTimeSetDialog(final double lat, final double lang) {
        final Dialog dialog_start = new Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog_start.setCancelable(true);
        dialog_start.setContentView(R.layout.dialog_set_time);

        final TextView tv_set_date = (TextView) dialog_start.findViewById(R.id.tv_set_date);
        final TextView tv_set_time = (TextView) dialog_start.findViewById(R.id.tv_set_time);
        Button btn_submit = (Button) dialog_start.findViewById(R.id.btn_submit);
        ImageView img_close_dialog = (ImageView) dialog_start.findViewById(R.id.img_close_dialog);

        final Calendar calendar = Calendar.getInstance();
        calendar.clear();

        final boolean[] isDateSet = {false};
        final boolean[] isTimeSet = {false};


        final DatePickerDialog.OnDateSetListener onStartDateChange = new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {

                isDateSet[0] = true;
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                Date date = calendar.getTime();
                tv_set_date.setText(CommonMethods.formatDate(date, "MMM-d-y"));
                //event_date[0] = AppConstant.getDateTimeFormat().format(date);
            }
        };

        final TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                isTimeSet[0] = true;
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                Date date = calendar.getTime();
                tv_set_time.setText(CommonMethods.formatDate(date, "hh:mm a"));
            }
        };

        tv_set_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment datePicker = new DatePickerFragment();
                datePicker.callBack(onStartDateChange);
                datePicker.show(getSupportFragmentManager(), "datePicker");
            }
        });

        tv_set_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment timePicker = new TimePickerFragment();
                timePicker.callBack(onTimeSetListener);
                timePicker.show(getSupportFragmentManager(), "timepicker");
            }
        });

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDateSet[0] && isTimeSet[0]) {
                    Date date = calendar.getTime();
                    Date currentTime = Calendar.getInstance().getTime();
                    if (currentTime.after(date)) {
                        Toast.makeText(MainActivity.this, "You must have to select a future date!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String timeFormat = CommonMethods.formatDate(date, "dd/MM/yyyy HH:mm:ss.SSS");
                    String deviceTimeFormat = CommonMethods.formatDate(currentTime, "dd/MM/yyyy HH:mm:ss.SSS");
                    sentRequestToInsertTime(GlobalAppAccess.URL_INSERT_TIME, lat, lang, MydApplication.deviceImieNumber, timeFormat, deviceTimeFormat);
                    dialog_start.dismiss();
                }
            }
        });

        img_close_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_start.dismiss();
            }
        });

        dialog_start.show();

    }

    public void sendRequestForGetTimes(String url) {

        //url = url + "?" + "email=" + email + "&password=" + password;
        // TODO Auto-generated method stub
        showProgressDialog("Loading..", true, false);

        final StringRequest req = new StringRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("DEBUG",response);


                        dismissProgressDialog();


                        TimeInfo timeInfo = MydApplication.gson.fromJson(response, TimeInfo.class);


                        if (timeInfo.getResult()) {
                            timeLocations.clear();
                            originalTimeLocations.clear();
                            timeLocations.addAll(timeInfo.getTimeLocations());
                            originalTimeLocations.addAll(timeInfo.getTimeLocations());
                            checkAllPermissionsAndSetUpMap();

                        } else {
                            AlertDialogForAnything.showAlertDialogWhenComplte(MainActivity.this, "Error", "Server problem while loading the timeLocations!", false);
                        }


                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();

                AlertDialogForAnything.showAlertDialogWhenComplte(MainActivity.this, "Error", "Network problem. please try again!", false);
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

    public void sentRequestToInsertTime(String url, final double lat, final double lang,
                                        final String deviceId, final String setTime, final String deviceTime) {

        //url = url + "?" + "email=" + email + "&password=" + password;
        // TODO Auto-generated method stub
        showProgressDialog("Loading..", true, false);

        final StringRequest req = new StringRequest(Request.Method.POST, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("DEBUG", response);


                        dismissProgressDialog();


                        try {

                            JSONObject jsonObject = new JSONObject(response);
                            boolean result = jsonObject.getBoolean("Result");

                            if (result) {
                                Time time = new Time();
                                time.setId(Integer.valueOf(jsonObject.getString("Id")));
                                time.setDeviceId(deviceId);
                                time.setTime(setTime);
                                if (selectedTimePosition != -1) {
                                    timeLocations.get(selectedTimePosition).getTimes().add(time);
                                    timeAdapter.notifyDataSetChanged();
                                } else {
                                    TimeLocation timeLocation = new TimeLocation();
                                    timeLocation.setLat(lat);
                                    timeLocation.setLang(lang);
                                    List<Time> times = new ArrayList<>();
                                    times.add(time);
                                    timeLocation.setTimes(times);
                                    timeLocations.add(timeLocation);
                                    userClickMarker.remove();
                                    userClickMarker = null;
                                    placeLastMarkerOfListInMap();
                                }

                                showInterstitialAds();
                            } else {
                                AlertDialogForAnything.showAlertDialogWhenComplte(MainActivity.this, "Error", "Server problem while loading the timeLocations!", false);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                //userId=XXX&routeId=XXX&selected=XXX
                Map<String, String> params = new HashMap<String, String>();
                params.put("lat", String.valueOf(lat));
                params.put("long", String.valueOf(lang));
                params.put("deviceId", deviceId);
                params.put("time", setTime);
                params.put("deviceTime", deviceTime);
                return params;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // TODO Auto-generated method stub
        MydApplication.getInstance().addToRequestQueue(req);
    }

    @Override
    public void receivedBroadcast() {
        billingHelper.receivedBroadcast();
    }


    /**
     * Demonstrates customizing the info window and/or its contents.
     */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        // private final View mWindow;

        //private final View mContents;

        CustomInfoWindowAdapter() {
            //mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
            // mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // render(marker, mContents);
            return null;
        }

        private void render(Marker marker, View view) {



            /*String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
                snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }*/
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RunnTimePermissions.PERMISSION_ALL) {
            // DeviceInfoUtils.checkMarshMallowPermission(this);
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            int result3 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
            if (result == PackageManager.PERMISSION_GRANTED
                    && result2 == PackageManager.PERMISSION_GRANTED
                    && result3 == PackageManager.PERMISSION_GRANTED) {
                //Log.d("DEBUG", "fragment attach");
                MydApplication.deviceImieNumber = DeviceInfoUtils.getDeviceImieNumber(this);

                setUpMap();
            }
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       // Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if (billingHelper.getmHelper() == null) return;

        // Pass on the activity result to the helper for handling
        if (!billingHelper.getmHelper().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            if (requestCode == GpsEnableTool.REQUEST_CHECK_SETTINGS) {
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        showProgressDialog("please wait..", true, false);
                        UserLastKnownLocation.LocationResult locationResult = new UserLastKnownLocation.LocationResult() {
                            @Override
                            public void gotLocation(Location location) {
                                dismissProgressDialog();

                                placeAllMarkersAndPlacementCamera(location.getLatitude(), location.getLongitude());


                            }
                        };
                        UserLastKnownLocation myLocation = new UserLastKnownLocation();
                        myLocation.getLocation(this, locationResult);

                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(MainActivity.this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }

            }
            if (requestCode == PLACE_PICKER_REQUEST) {
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(data, this);
                    String toastMsg = String.format("Place: %s", place.getName());
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                    if (userClickMarker != null) {
                        userClickMarker.remove();
                    }
                    userClickMarker = mMap.addMarker(getUserClickMarkerOptions(place.getLatLng(), String.valueOf(place.getName())));
                    onMarkerClick(userClickMarker);
                    zoomToSpecificLocation(place.getLatLng());
                    userClickMarker.showInfoWindow();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            //Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

  /*  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }*/


/*    public boolean onCreateOptionsMenu(Menu paramMenu) {
        getMenuInflater().inflate(R.menu.menu_main, paramMenu);
        return true;
    }*/

    public static final int PLACE_PICKER_REQUEST = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {

        switch (paramMenuItem.getItemId()) {

            case R.id.ic_poi:

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                //startActivity(new Intent(getActivity(), WishListActivity.class));
                // Toast.makeText(MainActivity.this,"Please publish your app on play store first!",Toast.LENGTH_LONG).show();
                break;

        }

        return false;
    }

    private String getTimesResponseDummy() {
        return "{\n" +
                "\t\"Result\": true,\n" +
                "\t\"TimeLocation\": [{\n" +
                "\t\t\t\"Lat\": 24.899786,\n" +
                "\t\t\t\"Lang\": 91.855263,\n" +
                "\t\t\t\"Times\": [{\n" +
                "\t\t\t\t\"Id\": 1,\n" +
                "\t\t\t\t\"deviceId\": \"357558066933272\",\n" +
                "\t\t\t\t\"time\": \"22/03/2019 05:06:07.000\"\n" +
                "\t\t\t}, {\n" +
                "\t\t\t\t\"Id\": 2,\n" +
                "\t\t\t\t\"deviceId\": \"357558066933272\",\n" +
                "\t\t\t\t\"time\": \"22/03/2020 05:06:07.000\"\n" +
                "\t\t\t}]\n" +
                "\n" +
                "\t\t}\n" +
                "\n" +
                "\t]\n" +
                "}";
    }

    private String getInsertTimeDummyResponse() {
        return "{\n" +
                "\"Result\" : true,\n" +
                " \"id\" : 1\n" +
                "} ";
    }

    public void sentRequestToCheckIfItIsValidPOI(final LatLng latLng) {

        String location = latLng.latitude + "," + latLng.longitude;

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?type=point_of_interest&rankby=distance&location=" + location + "&sensor=false&key=AIzaSyCHsF72opxJ7MfM5dqq4z_-2ujjujukI3E";
        Log.d("DEBUG", url);
        //url = url + "?" + "email=" + email + "&password=" + password;
        // TODO Auto-generated method stub
        showProgressDialog("Loading..", true, false);

        final StringRequest req = new StringRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Log.d("DEBUG", response);


                        dismissProgressDialog();

                        NearByPlaceInfo nearByPlaceInfo = MydApplication.gson.fromJson(response, NearByPlaceInfo.class);

                        if (nearByPlaceInfo.getResults().isEmpty()) {
                            Toast.makeText(MainActivity.this, "You daily quota for this api is finished!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String name = nearByPlaceInfo.getResults().get(0).getName();
                        double lat = nearByPlaceInfo.getResults().get(0).getGeometry().getLocation().getLat();
                        double lang = nearByPlaceInfo.getResults().get(0).getGeometry().getLocation().getLng();
                        LatLng nearByPlaceLatLng = new LatLng(lat, lang);

                        Location nearByPlaceLocation = new Location("nearByPlaceLocation");
                        nearByPlaceLocation.setLatitude(lat);
                        nearByPlaceLocation.setLongitude(lang);

                        Location userClickedLocation = new Location("userClickedLocation");
                        userClickedLocation.setLatitude(latLng.latitude);
                        userClickedLocation.setLongitude(latLng.longitude);

                        double distance = userClickedLocation.distanceTo(nearByPlaceLocation);

                        //Toast.makeText(MainActivity.this,distance + " ",Toast.LENGTH_LONG).show();

                        if (distance <= 10) {
                            if (userClickMarker != null) {
                                userClickMarker.remove();
                            }
                            userClickMarker = mMap.addMarker(getUserClickMarkerOptions(nearByPlaceLatLng, name));
                            onMarkerClick(userClickMarker);
                            zoomToSpecificLocation(nearByPlaceLatLng);
                            userClickMarker.showInfoWindow();
                        } else {
                            Toast.makeText(MainActivity.this, "wrong location - Please select a pick up zone!", Toast.LENGTH_LONG).show();
                        }


                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
            }
        });

        req.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // TODO Auto-generated method stub
        MydApplication.getInstance().addToRequestQueue(req);
    }




    private void loadAdview(){
        adview_banner = findViewById(R.id.adview_banner);
        AdRequest adRequestBanner = new AdRequest.Builder()
                .addTestDevice("554FD1C059BF37BF1981C59FF9E1DAE0")
                .build();
        adview_banner.loadAd(adRequestBanner);


        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        AdRequest adRequestInterstitial = new AdRequest.Builder().addTestDevice(
                "554FD1C059BF37BF1981C59FF9E1DAE0").build();
        mInterstitialAd.loadAd(adRequestInterstitial);
    }

    public void showInterstitialAds(){
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
    }
}
