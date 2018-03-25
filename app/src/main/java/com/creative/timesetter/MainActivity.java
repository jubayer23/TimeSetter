package com.creative.timesetter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.creative.timesetter.Utility.DeviceInfoUtils;
import com.creative.timesetter.Utility.GpsEnableTool;
import com.creative.timesetter.Utility.LastLocationOnly;
import com.creative.timesetter.Utility.RunnTimePermissions;
import com.creative.timesetter.alertbanner.AlertDialogForAnything;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity implements OnMapReadyCallback,GoogleMap.OnMapClickListener {
    private GoogleMap mMap;

    List<Marker> markers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();

        init();

        if (savedInstanceState == null) {

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
    }

    private void init(){

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
        mMap.setOnMapClickListener(this);
        LastLocationOnly lastLocationOnly = new LastLocationOnly(this);
        if (lastLocationOnly.canGetLocation()) {

            zoomToCurrentLocation(lastLocationOnly.getLatitude(), lastLocationOnly.getLongitude());

        }else{
            GpsEnableTool gpsEnableTool = new GpsEnableTool(this);
            gpsEnableTool.enableGPs();
        }

    }

    protected void zoomToCurrentLocation(double lat, double lang) {
        if(mMap == null) return;
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lang))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

        if (requestCode == GpsEnableTool.REQUEST_CHECK_SETTINGS) {
           LastLocationOnly lastLocationOnly = new LastLocationOnly(this);

            if (lastLocationOnly.canGetLocation()) {

                zoomToCurrentLocation(lastLocationOnly.getLatitude(), lastLocationOnly.getLongitude());

            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMap.addMarker(getUserClickMarkerOptions(latLng));
    }

    private MarkerOptions getUserClickMarkerOptions(LatLng position){
         return  new MarkerOptions()
                .position(position)
                .title("Brisbane")
                 .draggable(true);
    }





    /** Demonstrates customizing the info window and/or its contents. */
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
           return  null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            int badge;
            // Use the equals() method on a Marker to check for equals.  Do not use ==.
            if (marker.equals(mBrisbane)) {
                badge = R.drawable.badge_qld;
            } else if (marker.equals(mAdelaide)) {
                badge = R.drawable.badge_sa;
            } else if (marker.equals(mSydney)) {
                badge = R.drawable.badge_nsw;
            } else if (marker.equals(mMelbourne)) {
                badge = R.drawable.badge_victoria;
            } else if (marker.equals(mPerth)) {
                badge = R.drawable.badge_wa;
            } else if (marker.equals(mDarwin1)) {
                badge = R.drawable.badge_nt;
            } else if (marker.equals(mDarwin2)) {
                badge = R.drawable.badge_nt;
            } else if (marker.equals(mDarwin3)) {
                badge = R.drawable.badge_nt;
            } else if (marker.equals(mDarwin4)) {
                badge = R.drawable.badge_nt;
            } else {
                // Passing 0 to setImageResource will clear the image view.
                badge = 0;
            }
            ((ImageView) view.findViewById(R.id.badge)).setImageResource(badge);

            String title = marker.getTitle();
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
            }
        }
    }
}
