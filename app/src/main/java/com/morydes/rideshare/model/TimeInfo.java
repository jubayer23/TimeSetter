
package com.morydes.rideshare.model;

import java.util.ArrayList;
import java.util.List;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TimeInfo implements Parcelable
{

    @SerializedName("Result")
    @Expose
    private Boolean result;
    @SerializedName("TimeLocation")
    @Expose
    private List<TimeLocation> timeLocations = new ArrayList<>();
    public final static Parcelable.Creator<TimeInfo> CREATOR = new Creator<TimeInfo>() {


        @SuppressWarnings({
            "unchecked"
        })
        public TimeInfo createFromParcel(Parcel in) {
            return new TimeInfo(in);
        }

        public TimeInfo[] newArray(int size) {
            return (new TimeInfo[size]);
        }

    }
    ;

    protected TimeInfo(Parcel in) {
        this.result = ((Boolean) in.readValue((Boolean.class.getClassLoader())));
        in.readList(this.timeLocations, (TimeLocation.class.getClassLoader()));
    }

    public TimeInfo() {
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public List<TimeLocation> getTimeLocations() {
        return timeLocations;
    }

    public void setTimeLocations(List<TimeLocation> timeLocations) {
        this.timeLocations = timeLocations;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(result);
        dest.writeList(timeLocations);
    }

    public int describeContents() {
        return  0;
    }

}
