package com.morydes.rideshare.Utility;

import android.os.Environment;
import android.util.Log;


import com.morydes.rideshare.appdata.GlobalAppAccess;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by comsol on 21-Nov-16.
 */
public class AccessDirectory {
    /**
     * returning image / video
     */
    public static File getOutputMediaFile() {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                GlobalAppAccess.APP_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("DEBUG", "Oops! Failed create "
                        + GlobalAppAccess.APP_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "PILLAR_IMG_" + timeStamp + ".jpg");


        return mediaFile;
    }
}
