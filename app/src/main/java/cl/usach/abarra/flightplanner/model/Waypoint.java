package cl.usach.abarra.flightplanner.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by AiBarr on 30/04/2017.
 */

public class Waypoint implements Parcelable {
    private LatLng position;
    private int  speed;
    private double height;
    private char type;

    public Waypoint() {
    }

    public Waypoint(LatLng position, int speed, double height, char type) {
        this.position = position;
        this.speed = speed;
        this.height = height;
        this.type = type;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    protected Waypoint(Parcel in) {
        position = in.readParcelable(LatLng.class.getClassLoader());
        speed = in.readInt();
        height = in.readDouble();
    }

    public static final Creator<Waypoint> CREATOR = new Creator<Waypoint>() {
        @Override
        public Waypoint createFromParcel(Parcel in) {
            return new Waypoint(in);
        }

        @Override
        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(position, flags);
        dest.writeInt(speed);
        dest.writeDouble(height);
    }
}
