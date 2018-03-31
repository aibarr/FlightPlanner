package cl.usach.abarra.flightplanner.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by AiBarr on 30/04/2017.
 */

public class Waypoint implements Parcelable {
    private LatLng position;
    private int  speed;
    private double height;
    private char type;

    public static double rad2deg = (180 / Math.PI);
    public static double deg2rad = (1.0 / rad2deg);

    public Waypoint() {
    }

    public Waypoint(LatLng position, int speed, double height, char type) {
        this.position = position;
        this.speed = speed;
        this.height = height;
        this.type = type;
    }

//    public Waypoint fromUTM(int zone, Double longitude, Double latitud){
//
//    }

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

    public int getUTMZone(){
        int zone = (int)((position.longitude - -186.0) / 6.0);
        if (position.latitude < 0) zone *= -1;
        return zone;
    }

//    public Double[] toUTM(){
//        return new Double[];
//    }

    public Waypoint newPosition(Double bearing, Double distance){
        Double earthRadius = 6378100.0;

        Double lat1 = deg2rad * (this.position.latitude);
        Double lon1 = deg2rad * (this.position.longitude);
        Double brng = deg2rad * (bearing);
        Double dr = distance / earthRadius;

        Double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dr) +
                Math.cos(lat1) * Math.sin(dr) * Math.cos(brng));
        Double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
                Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2));

        Double latout = rad2deg * (lat2);
        Double lonout = rad2deg * (lon2);

        return new Waypoint(new LatLng(latout, lonout), 0, 0.0, 'p');


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
