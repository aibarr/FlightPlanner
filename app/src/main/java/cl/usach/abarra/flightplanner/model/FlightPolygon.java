package cl.usach.abarra.flightplanner.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.util.GridPolygon;
import cl.usach.abarra.flightplanner.util.PointLatLngAlt;

public class FlightPolygon implements Parcelable{
    private List<LatLng> vertices;
    private Polyline innerRoute;
    private PolylineOptions lineOptions = new PolylineOptions().color(Color.GREEN);
    private Polygon polygon;
    private PolygonOptions options = new PolygonOptions().strokeColor(Color.RED).strokeWidth(5);

    private GridPolygon grid;

    private List<Marker> mVertices;
    private List<Marker> mGrid;

    MarkerOptions mVerOptions;
    MarkerOptions mGridOptions;

    public FlightPolygon(List<LatLng> vertices) {
        this.vertices = vertices;
        mGrid = new ArrayList<Marker>();
        mVertices = new ArrayList<Marker>();
        mVerOptions = new MarkerOptions();
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
        this.mVertices.add(map.addMarker(mVerOptions.position(point)));
        if(this.vertices.size()>2) this.addToMap(map);
    }

    void removePoint (LatLng point, GoogleMap map){
        int ptnIndex = this.vertices.indexOf(point);
        this.mVertices.get(ptnIndex).remove();
        this.mVertices.remove(ptnIndex);
        this.vertices.remove(point);
        if (this.vertices.size()<3) this.removeFromMap();
    }

    private void addGrid (GoogleMap map, Double distance, Double angle, GridPolygon.StartPosition startpos, PointLatLngAlt homeLocation){
        this.grid = new GridPolygon();
        this.grid.setVertices(this.vertices);
        //calculamos la grilla
        this.grid.calculateGridMP(100.0, distance, 0.0, angle, 0,0, startpos, homeLocation);
        List<LatLng> auxL = new ArrayList<LatLng>(this.grid.getGrid());
        List<LatLng> removal = new ArrayList<LatLng>();
        //añadir marcadores al mapa
        for (LatLng point : auxL){
            if (!(point.latitude == 0.0 && point.longitude == 0.0)){
                mGrid.add(map.addMarker(mGridOptions.position(point)));
            }
            else removal.add(point);
        }
        auxL.removeAll(removal);
        //añado la grilla
        innerRoute = map.addPolyline(lineOptions.addAll(auxL));
    }

}
