package cl.usach.abarra.flightplanner.util;

import com.google.android.gms.maps.model.LatLng;

import cl.usach.abarra.flightplanner.model.Waypoint;

public class PointLatLngAlt {
    public Double lat;
    public Double lng;
    public Double alt;
    public String tag;
    public String tag2;

    PointLatLngAlt zero = new PointLatLngAlt(0.0,0.0,0.0, "");


    public PointLatLngAlt(){

    }

    public PointLatLngAlt(Double lat, Double lng, Double alt, String tag) {
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
        this.tag = tag;
    }

    public PointLatLngAlt(Double lat, Double lng){
        this.lat = lat;
        this.lng = lng;
    }

    public PointLatLngAlt(Waypoint waypoint){
        this.lat = waypoint.getPosition().latitude;
        this.lng = waypoint.getPosition().longitude;
        this.alt = waypoint.getHeight();
    }

    public PointLatLngAlt(Double[] dblarr){
        this.lat = dblarr[0];
        this.lng = dblarr[1];
        if (dblarr.length > 2)
            this.alt = dblarr[2];
    }

    public PointLatLngAlt(PointLatLngAlt plla){
        this.lat = plla.lat;
        this.lng = plla.lng;
        this.alt = plla.alt;
        this.tag = plla.tag;
    }

    public LatLng point(){
        LatLng latLng = new LatLng(lat, lng);
        return latLng;
    }

    public PointLatLngAlt(LatLng latLng){
        this.lat = latLng.latitude;
        this.lng = latLng.longitude;
    }
}
