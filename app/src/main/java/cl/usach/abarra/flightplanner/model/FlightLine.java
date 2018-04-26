package cl.usach.abarra.flightplanner.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class FlightLine implements Parcelable{
    private List<Marker> markers;
    private List<LatLng> vertices;
    private Polyline line;
    private PolylineOptions polylineOptions;
    private MarkerOptions mOptions;



    private int pointCounter;

    public FlightLine(){
        pointCounter = 0;
        this.markers = new ArrayList<Marker>();
        this.vertices = new ArrayList<LatLng>();
        mOptions = new MarkerOptions();
        polylineOptions = new PolylineOptions().color(Color.BLUE).width(7);

    }

    public FlightLine (List<LatLng> points, GoogleMap map){
        pointCounter = 0;
        this.vertices = new ArrayList<>(points);
        this.markers = new ArrayList<>();

        //TODO: darle opciones a marcadores
        mOptions = new MarkerOptions();
        polylineOptions = new PolylineOptions().color(Color.BLUE).width(7);

        //añado marcadores para cada punto
        for (LatLng point : points)
        {
            pointCounter++;
            this.markers.add(map.addMarker(mOptions.position(point)));
        }
        line = map.addPolyline(polylineOptions.addAll(this.vertices));
    }

    public List<LatLng> getVertices() {
        return vertices;
    }

    protected FlightLine(Parcel in) {
        vertices = in.createTypedArrayList(LatLng.CREATOR);
        polylineOptions = in.readParcelable(PolylineOptions.class.getClassLoader());
        mOptions = in.readParcelable(MarkerOptions.class.getClassLoader());
        pointCounter = in.readInt();
    }

    public static final Creator<FlightLine> CREATOR = new Creator<FlightLine>() {
        @Override
        public FlightLine createFromParcel(Parcel in) {
            return new FlightLine(in);
        }

        @Override
        public FlightLine[] newArray(int size) {
            return new FlightLine[size];
        }
    };

    public void addPoint(LatLng point, int count, GoogleMap map){
        this.markers.add(map.addMarker(mOptions.position(point)));//TODO: añadir numero al marcador
        this.vertices.add(point);
        this.pointCounter++;
        count++;
        this.addToMap(map);
    }

    public void addPoint(LatLng point, GoogleMap map){
        this.markers.add(map.addMarker(mOptions.position(point)));//TODO: añadir numero al marcador
        this.vertices.add(point);
        this.pointCounter++;
        this.addToMap(map);
    }

    public void removePoint(Marker marker, GoogleMap map){
        this.markers.remove(marker);
        this.vertices.remove(marker.getPosition());
        this.addToMap(map);
    }

    public void addToMap (GoogleMap map){
        if(pointCounter == 0 || line == null){
            line = map.addPolyline(polylineOptions.addAll(this.vertices));
        } else {
            line.setPoints(this.vertices);
        }
    }

    public void movePoint(Marker marker, LatLng destination){
        int result = this.markers.indexOf(marker);
        if (!(result<0)){
            this.markers.get(result).setPosition(destination);
        }
    }

    public boolean markerBelongs(Marker marker){
        if(!(this.markers.indexOf(marker)<0)){
            return true;
        } else return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(vertices);
        dest.writeParcelable(polylineOptions, flags);
        dest.writeParcelable(mOptions, flags);
        dest.writeInt(pointCounter);
    }
}
