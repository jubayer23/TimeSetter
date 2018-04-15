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
    public final static Parcelable.Creator<TimeLocation> CREATOR = new Creator<TimeLocation>() {


        @SuppressWarnings({
                "unchecked"
        })
        public TimeLocation createFromParcel(Parcel in) {
            return new TimeLocation(in);
        }

        public TimeLocation[] newArray(int size) {
            return (new TimeLocation[size]);
        }

    };

    protected TimeLocation(Parcel in) {
        this.lat = ((Double) in.readValue((Double.class.getClassLoader())));
        this.lang = ((Double) in.readValue((Double.class.getClassLoader())));
        in.readList(this.times, (Time.class.getClassLoader()));
    }

    public TimeLocation() {
    }

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

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(lat);
        dest.writeValue(lang);
        dest.writeList(times);
    }

    public int describeContents() {
        return 0;
    }

}
