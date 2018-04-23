package com.morydes.rideshare.model;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TimeLocation implements Parcelable {

    @SerializedName("Lat")
    @Expose
    private Double lat;
    @SerializedName("Long")
    @Expose
    private Double lang;
    @SerializedName("Times")
    @Expose
    private List<Time> times = null;

    public TimeLocation(){

    }

    protected TimeLocation(Parcel in) {
        times = in.createTypedArrayList(Time.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(times);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TimeLocation> CREATOR = new Creator<TimeLocation>() {
        @Override
        public TimeLocation createFromParcel(Parcel in) {
            return new TimeLocation(in);
        }

        @Override
        public TimeLocation[] newArray(int size) {
            return new TimeLocation[size];
        }
    };

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLang() {
        return lang;
    }

    public void setLang(Double lang) {
        this.lang = lang;
    }

    public List<Time> getTimes() {
        return times;
    }

    public void setTimes(List<Time> times) {
        this.times = times;
    }


}
