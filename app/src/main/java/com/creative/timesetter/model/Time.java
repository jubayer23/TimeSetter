
package com.creative.timesetter.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Time implements Parcelable
{

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("deviceId")
    @Expose
    private String deviceId;
    @SerializedName("time")
    @Expose
    private String time;
    public final static Parcelable.Creator<Time> CREATOR = new Creator<Time>() {


        @SuppressWarnings({
            "unchecked"
        })
        public Time createFromParcel(Parcel in) {
            return new Time(in);
        }

        public Time[] newArray(int size) {
            return (new Time[size]);
        }

    }
    ;

    protected Time(Parcel in) {
        this.id = ((Integer) in.readValue((Integer.class.getClassLoader())));
        this.deviceId = ((String) in.readValue((String.class.getClassLoader())));
        this.time = ((String) in.readValue((String.class.getClassLoader())));
    }

    public Time() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(deviceId);
        dest.writeValue(time);
    }

    public int describeContents() {
        return  0;
    }

}
