package cl.usach.abarra.flightplanner.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import cl.usach.abarra.flightplanner.util.GridPolygon;
import cl.usach.abarra.flightplanner.util.PointLatLngAlt;

public class FlightPolygon implements Parcelable{
    private List<LatLng> vertices;
    private Polyline polyline;
    private PolylineOptions lineOptions = new PolylineOptions().color(Color.GREEN);
    private Polygon polygon;
    private PolygonOptions options = new PolygonOptions().strokeColor(Color.RED).strokeWidth(5);

    private GridPolygon grid;

    public FlightPolygon(List<LatLng> vertices) {
        this.vertices = vertices;
    }


    protected FlightPolygon(Parcel in) {
        vertices = in.createTypedArrayList(LatLng.CREATOR);
        options = in.readParcelable(PolygonOptions.class.getClassLoader());
    }

    public static final Creator<FlightPolygon> CREATOR = new Creator<FlightPolygon>() {
        @Override
        public FlightPolygon createFromParcel(Parcel in) {
            return new FlightPolygon(in);
        }

        @Override
        public FlightPolygon[] newArray(int size) {
            return new FlightPolygon[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(vertices);
        dest.writeParcelable(options, flags);
    }

    void addToMap (GoogleMap map){
        if (this.vertices.size() > 2){
            if (this.polygon != null){
                polygon.setPoints(this.vertices);
            }else{
                this.polygon = map.addPolygon(this.options.addAll(this.vertices));
            }
        }
    }

    void removeFromMap (){
        this.polygon.remove();
        this.polygon = null;
    }

    void addPoint (LatLng point, GoogleMap map){
        this.vertices.add(point);
        if(this.vertices.size()>2) this.addToMap(map);
    }

    void removePoint (LatLng point, GoogleMap map){
        this.vertices.remove(point);
        if (this.vertices.size()<3) this.removeFromMap();
    }

    private void addGrid (GoogleMap map){
        this.grid = new GridPolygon();
        this.grid.setVertices(this.vertices);
        this.grid.calculateGridMP(100.0, 10.0, 0.0, 45.0, 0,0, GridPolygon.StartPosition.Home, new PointLatLngAlt(0.0,0.0));
    }

}
