
package com.morydes.rideshare.model;

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

    @SerializedName("rideshare")
    @Expose
    private String rideshare;
    @SerializedName("seats")
    @Expose
    private String seats;

    public Time(){

    }

    protected Time(Parcel in) {
        deviceId = in.readString();
        time = in.readString();
        rideshare = in.readString();
        seats = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeString(time);
        dest.writeString(rideshare);
        dest.writeString(seats);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Time> CREATOR = new Creator<Time>() {
        @Override
        public Time createFromParcel(Parcel in) {
            return new Time(in);
        }

        @Override
        public Time[] newArray(int size) {
            return new Time[size];
        }
    };

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

    public String getRideshare() {
        return rideshare;
    }

    public void setRideshare(String rideshare) {
        this.rideshare = rideshare;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }


}
