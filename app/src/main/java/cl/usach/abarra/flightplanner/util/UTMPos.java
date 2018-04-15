package cl.usach.abarra.flightplanner.util;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.model.Waypoint;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.coords.UTMCoord;

public class UTMPos implements Parcelable{


    public Double x;
    public Double y;
    public int zone;
    public String tag;

    public double rad2deg = (180 / Math.PI);
    public double deg2rad = (1.0 / rad2deg);

    public static final UTMPos zero = new UTMPos(0.0,0.0,0);

    public UTMPos(){

    }

    public UTMPos(PointLatLngAlt pos)
    {
        Double[] dd = pos.toUTM();
        this.x = dd[0];
        this.y = dd[1];
        this.zone = pos.getUTMZone();
        this.tag = null;
    }

    public UTMPos(Double x, Double y, int zone) {
        this.x = x;
        this.y = y;
        this.zone = zone;
        this.tag = null;
    }

    public UTMPos (UTMPos pos){
        this.x = pos.x;
        this.y = pos.y;
        this.zone = pos.zone;
        this.tag = pos.tag;
    }

    public PointLatLngAlt toLLA2()
    {
        LatLon latLon = UTMCoord.locationFromUTMCoord(Math.abs(this.zone), (zone <0 ? AVKey.SOUTH : AVKey.NORTH ), this.x, this.y, null );
        PointLatLngAlt ans = new PointLatLngAlt(latLon.latitude.degrees, latLon.longitude.degrees);

        if (this.tag != null)
            ans.tag = this.tag;

        return ans;
    }

    public PointLatLngAlt toLLA()
    {
        System.out.println(this.toString());
        LatLon latLon = UTMCoord.locationFromUTMCoord(Math.abs(this.zone), (zone <0 ? AVKey.SOUTH : AVKey.NORTH ), this.x, this.y, null );

        PointLatLngAlt ans = new PointLatLngAlt(latLon.latitude.degrees, latLon.longitude.degrees);
        if (this.tag != null)
            ans.tag = this.tag;
        return ans;
    }

    //TODO: constructor desde ptnlatlng

    //Hacia LatLng

    public static List<UTMPos> toList(List<Double[]> input, int zone){

        List<UTMPos> list = new ArrayList<UTMPos>();

        for (Double[] inpt : input){
            UTMPos utmPos = new UTMPos(inpt[0], inpt[1], zone);
            list.add(utmPos);
            System.out.println("Input: " + inpt[0] +", "+ inpt[1] + " Output: " + utmPos.toString() + "\n");
        }

        return list;

    }

    protected UTMPos(Parcel in) {
        if (in.readByte() == 0) {
            x = null;
        } else {
            x = in.readDouble();
        }
        if (in.readByte() == 0) {
            y = null;
        } else {
            y = in.readDouble();
        }
        zone = in.readInt();
        tag = in.readString();
        rad2deg = in.readDouble();
        deg2rad = in.readDouble();
    }

    public static final Creator<UTMPos> CREATOR = new Creator<UTMPos>() {
        @Override
        public UTMPos createFromParcel(Parcel in) {
            return new UTMPos(in);
        }

        @Override
        public UTMPos[] newArray(int size) {
            return new UTMPos[size];
        }
    };

    //Distancias
    public Double GetDistance (UTMPos b){
        return Math.sqrt(Math.pow(Math.abs(x - b.x), 2) + Math.pow(Math.abs(y - b.y), 2));
    }

    //Angulo entre un punto y otro
    public Double getBearing(UTMPos b){
        double y = b.y - this.y;
        double x = b.x - this.x;

        return (rad2deg * (Math.atan2(x , y))+ 360) % 360;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof UTMPos)){
            return false;
        }
        return (((((UTMPos)obj).x == this.x) && (((UTMPos)obj).y == this.y)) && (((UTMPos)obj).zone == this.zone) && obj.getClass().equals(getClass()));
    }

    @Override
    public int hashCode() {
        int hashCode = x.hashCode();
        hashCode = (hashCode * 397) ^ y.hashCode();
        hashCode = (hashCode * 397) ^ zone;
        return hashCode;
    }

    @Override
    public String toString() {
        return "utmpos: " + x + "," + y;
    }

    public boolean isZero (){
        if (this == zero) return true;
        return false;
    }



    //TODO: Obtener UTM desde waypont (geo)
    public UTMPos(Waypoint waypoint){

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (x == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(x);
        }
        if (y == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(y);
        }
        dest.writeInt(zone);
        dest.writeString(tag);
        dest.writeDouble(rad2deg);
        dest.writeDouble(deg2rad);
    }
}
