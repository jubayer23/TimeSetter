package com.morydes.rideshare.fragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;

import java.util.Calendar;

/**
 * Created by jubayer on 8/9/2017.
 */


/**
 * this class is for picking date from a calender
 * */
public class TimePickerFragment extends DialogFragment {
    TimePickerDialog.OnTimeSetListener onTimeSet;

    public TimePickerFragment() {

    }

    public void callBack(TimePickerDialog.OnTimeSetListener ontime) {
        this.onTimeSet = ontime;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of DatePickerDialog and return it
        return new TimePickerDialog(getActivity(), onTimeSet, hour, minute,  DateFormat.is24HourFormat(getActivity()));
    }

}