package com.creative.timesetter.Utility;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

/**
 * Created by jubayer on 20-Aug-17.
 */

public class RunnTimePermissions {


    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int PERMISSION_ALL = 1;
    public static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
           };

    public static boolean requestForAllRuntimePermissions(Context context) {

        boolean has_permission = true;
        if (!hasPermissions(context, PERMISSIONS)) {
            has_permission = false;
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, PERMISSION_ALL);
        }
        return has_permission;
    }
}