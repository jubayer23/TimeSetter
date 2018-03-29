package com.creative.timesetter;

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
import android.view.View;
import android.widget.Button;
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
import com.creative.timesetter.Utility.CommonMethods;
import com.creative.timesetter.Utility.DeviceInfoUtils;
import com.creative.timesetter.Utility.GpsEnableTool;
import com.creative.timesetter.Utility.LastLocationOnly;
import com.creative.timesetter.Utility.RunnTimePermissions;
import com.creative.timesetter.Utility.UserLastKnownLocation;
import com.creative.timesetter.adapter.TimeAdapter;
import com.creative.timesetter.alertbanner.AlertDialogForAnything;
import com.creative.timesetter.appdata.GlobalAppAccess;
import com.creative.timesetter.appdata.MydApplication;
import com.creative.timesetter.fragment.DatePickerFragment;
import com.creative.timesetter.fragment.TimePickerFragment;
import com.creative.timesetter.model.Time;
import com.creative.timesetter.model.TimeLocation;
import com.creative.timesetter.model.TimeInfo;
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

import static com.creative.timesetter.appdata.MydApplication.deviceImieNumber;


public class MainActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, View.OnClickListener {
    private GoogleMap mMap;

    List<Marker> markers = new ArrayList<>();

    LinearLayout ll_bottom_sheet;

    BottomSheetBehavior sheetBehavior;

    private List<TimeLocation> timeLocations = new ArrayList<>();

    //bottom sheet ui item
    private TextView tv_num_of_time;
    private Button btn_set_time;
    private RecyclerView recyclerView;
    private TimeAdapter timeAdapter;
    private TextView tv_no_time_set_warning;
    //private List<Time> times = new ArrayList<>();


    private Marker userClickMarker;
    /*
    * This variable is very important
    * */
    public static int selectedTimePosition = 0;
    private Marker prevClickedMarker;
    private HashMap<Marker, Integer> hashMapMarker = new HashMap<>();
    LatLngBounds.Builder builder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();

        init();

        initBottomSheetListAdapter();

        if (savedInstanceState == null) {


            sendRequestForGetTimes(GlobalAppAccess.URL_GET_TIMES);

        }
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
        tv_no_time_set_warning = (TextView) findViewById(R.id.tv_no_time_set_warning);
        tv_no_time_set_warning.setVisibility(View.GONE);
        btn_set_time = (Button) findViewById(R.id.btn_set_time);
        btn_set_time.setOnClickListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

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


        placeAllMarkerOfListInMap();

        moveMapCameraToLoadAllMarker();


       /* if (mMap == null) return;
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lang))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
    }

    private void placeAllMarkerOfListInMap(){
        int count = 0;
        mMap.clear();
        builder = new LatLngBounds.Builder();
        for (TimeLocation timeLocation : timeLocations) {
            LatLng latLng = new LatLng(timeLocation.getLat(), timeLocation.getLang());
            Marker marker = mMap.addMarker(getTimeMarker(latLng));
            builder.include(latLng);
            hashMapMarker.put(marker, count);
            count++;
        }
    }

    private void placeLastMarkerOfListInMap(){
        int lastPosition = timeLocations.size() - 1;
        TimeLocation timeLocation = timeLocations.get(lastPosition);
        LatLng latLng = new LatLng(timeLocation.getLat(), timeLocation.getLang());
        Marker marker = mMap.addMarker(getTimeMarker(latLng));
        hashMapMarker.put(marker, lastPosition);
        onMarkerClick(marker);
    }

    private void moveMapCameraToLoadAllMarker(){
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }
    @Override
    public void onMapLongClick(LatLng latLng) {
        if (userClickMarker != null) {
            userClickMarker.remove();
        }
        userClickMarker = mMap.addMarker(getUserClickMarkerOptions(latLng));
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

        sheetBehavior.setPeekHeight(240);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        return false;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if(id == R.id.btn_set_time){
            if(prevClickedMarker == null) return;
            double lat = prevClickedMarker.getPosition().latitude;
            double lang = prevClickedMarker.getPosition().longitude;
            if(userClickMarker!= null && userClickMarker.equals(prevClickedMarker)){
                showTimeSetDialog(lat,lang);
            }else{
                showTimeSetDialog(lat,lang);
            }

        }
    }

    public void toggleBottomSheet() {

        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            sheetBehavior.setPeekHeight(240);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setPeekHeight(240);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }


    private MarkerOptions getUserClickMarkerOptions(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .title("Brisbane")
                .draggable(true);
    }

    private MarkerOptions getTimeMarker(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
    }

    public void removeMarkerAndSetAsUserClickedMarker(){
        showProgressDialog("Loading..", true, false);
        timeLocations.remove(selectedTimePosition);
        placeAllMarkerOfListInMap();
        userClickMarker = mMap.addMarker(getUserClickMarkerOptions(prevClickedMarker.getPosition()));
        selectedTimePosition = -1;
        dismissProgressDialog();
        updateBottomSheetUi(0);
        sheetBehavior.setPeekHeight(240);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void updateBottomSheetUi(int num_of_time_set){
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

    private void showTimeSetDialog(final double lat, final double lang){
        final Dialog dialog_start = new Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog_start.setCancelable(true);
        dialog_start.setContentView(R.layout.dialog_set_reminder);

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
                tv_set_date.setText(CommonMethods.formatDate(date, "y-MMM-d"));
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
                tv_set_time.setText(CommonMethods.formatDate(date, "HH:mm:ss"));
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
                if(isDateSet[0] && isTimeSet[0]){
                    Date date = calendar.getTime();
                    Date currentTime = Calendar.getInstance().getTime();
                    if(currentTime.after(date)){
                        Toast.makeText(MainActivity.this,"You must have to select a future date!",Toast.LENGTH_LONG).show();
                        return;
                    }

                    String timeFormat = CommonMethods.formatDate(date,"dd/MM/yyyy HH:mm:ss.SSS");
                    String deviceTimeFormat = CommonMethods.formatDate(currentTime,"dd/MM/yyyy HH:mm:ss.SSS");
                    sentRequestToInsertTime(GlobalAppAccess.URL_INSERT_TIME,lat,lang,MydApplication.deviceImieNumber,timeFormat,deviceTimeFormat);
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


                        try {
                            TimeInfo timeInfo = MydApplication.gson.fromJson(response, TimeInfo.class);

                            JSONObject jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("result");

                            if (timeInfo.getResult()) {
                                timeLocations.clear();
                                timeLocations.addAll(timeInfo.getTimeLocations());
                                checkAllPermissionsAndSetUpMap();

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
                Log.d("DEBUG","its here 1");
                Log.d("DEBUG",deviceImieNumber);
                dismissProgressDialog();

                //AlertDialogForAnything.showAlertDialogWhenComplte(MainActivity.this, "Error", "Network problem. please try again!", false);
                String response = getTimesResponseDummy();

                TimeInfo timeInfo = MydApplication.gson.fromJson(response, TimeInfo.class);

                if (timeInfo.getResult()) {
                    timeLocations.clear();
                    timeLocations.addAll(timeInfo.getTimeLocations());
                    checkAllPermissionsAndSetUpMap();

                }
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
                       Log.d("DEBUG",response);


                        dismissProgressDialog();


                        try {

                            JSONObject jsonObject = new JSONObject(response);
                            boolean result = jsonObject.getBoolean("Result");

                            if (result) {
                                Time time = new Time();
                                time.setId(Integer.valueOf(jsonObject.getString("id")));
                                time.setDeviceId(deviceId);
                                time.setTime(setTime);
                                if(selectedTimePosition != -1){
                                    timeLocations.get(selectedTimePosition).getTimes().add(time);
                                    timeAdapter.notifyDataSetChanged();
                                }else{
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

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(getInsertTimeDummyResponse());
                    boolean result = jsonObject.getBoolean("Result");

                    if (result) {
                        Time time = new Time();
                        time.setId(Integer.valueOf(jsonObject.getString("id")));
                        time.setDeviceId(deviceId);
                        time.setTime(setTime);
                        if(selectedTimePosition != -1){
                            timeLocations.get(selectedTimePosition).getTimes().add(time);
                            timeAdapter.notifyDataSetChanged();
                        }else{
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

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }){
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

    /**
     * Demonstrates customizing the info window and/or its contents.
     */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        // private final View mWindow;

        private final View mContents;

        CustomInfoWindowAdapter() {
            //mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
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
            if (result == PackageManager.PERMISSION_GRANTED
                    && result2 == PackageManager.PERMISSION_GRANTED) {
                Log.d("DEBUG", "fragment attach");

                setUpMap();
            }
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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

    private String getInsertTimeDummyResponse(){
        return "{\n" +
                "\"Result\" : true,\n" +
                " \"id\" : 1\n" +
                "} ";
    }
}
