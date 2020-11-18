package com.example.randomdatalogger;

import android.os.Parcel;
import android.os.Parcelable;

// TODO Change this if you want to change the database schema and what's being recorded
/**
 * Class representing a single signal strength reading.
 * Parcelable for sending from service to main activity for charting.
 */
public class ReadingDataPoint implements Parcelable {
    private long _datetime;
    private int _signalStrength;

    public ReadingDataPoint(long datetime, int signalStrength) {
        this._datetime = datetime;
        this._signalStrength = signalStrength;
    }

    private ReadingDataPoint(Parcel in) {
        _datetime = in.readLong();
        _signalStrength = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(_datetime);
        parcel.writeInt(_signalStrength);
    }

    public static final Parcelable.Creator<ReadingDataPoint> CREATOR =
            new Parcelable.Creator<ReadingDataPoint>() {
                public ReadingDataPoint createFromParcel(Parcel in) {
                    return new ReadingDataPoint(in);
                }

                @Override
                public ReadingDataPoint[] newArray(int i) {
                    return new ReadingDataPoint[i];
                }
            };

    public long get_datetime() {
        return _datetime;
    }

    public void set_datetime(long _datetime) {
        this._datetime = _datetime;
    }

    public float get_signalStrength() {
        return _signalStrength;
    }

    public void set_signalStrength(int _signalStrength) {
        this._signalStrength = _signalStrength;
    }
}

