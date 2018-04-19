package cl.usach.abarra.flightplanner.model;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class FlightLine {
    private List<Marker> markers;
    private Polyline line;
    private PolylineOptions polylineOptions;
    private MarkerOptions mOptions;

    public FlightLine(){
        this.markers = new ArrayList<>();
    }

    public FlightLine (List<LatLng> points, GoogleMap map){
        this.markers = new ArrayList<>();
        mOptions = new MarkerOptions();
        for (LatLng point : points)
        {
            this.markers.add(map.addMarker(mOptions.position(point)));
        }
    }

    public void addPoint(LatLng point, int count, GoogleMap map){

    }

    public void addPoint(LatLng point, GoogleMap map){

    }

    public void addToMap (GoogleMap map){

    }
}
