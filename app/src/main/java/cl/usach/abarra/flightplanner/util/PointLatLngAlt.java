package cl.usach.abarra.flightplanner.util;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.model.Waypoint;

public class PointLatLngAlt {
    public Double lat;
    public Double lng;
    public Double alt;
    public String tag;
    public String tag2;

    static PointLatLngAlt zero = new PointLatLngAlt(0.0,0.0,0.0, "");


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

    public Double[] toDoubleArray(){
        Double[] answer = {this.lng, this.lat};
        return answer;
    }

    public int getUTMZone (){
        int zone = (int)((lng - -186.0) / 6.0);
        if (lat < 0 ) zone *= -1;
        return zone;
    }

    //TODO: terminar
    public Double[] toUTM()
    {
        return toUTM(getUTMZone());
    }

    // force a zone
    public Double[] toUTM(int utmzone)
    {
        IProjectedCoordinateSystem utm = ProjectedCoordinateSystem.WGS84_UTM(Math.abs(utmzone), lat < 0 ? false : true);
        ICoordinateTransformation trans = ctfac.CreateFromCoordinateSystems(wgs84, utm);
        Double[] pll = { lng, lat };
        // get leader utm coords
        Double[] utmxy = trans.MathTransform.Transform(pll);
        return utmxy;
    }

    public static List<Double[]> ToUTM(int utmzone, List<PointLatLngAlt> list){
        IProjectedCoordinateSystem utm = ProjectedCoordinateSystem.WGS84_UTM(Math.Abs(utmzone), list.get(0).lat < 0 ? false : true);
        ICoordinateTransformation trans = ctfac.CreateFromCoordinateSystems(wgs84, utm);

        List<Double[]> data = new ArrayList<Double[]>();

        for (PointLatLngAlt dat : list){
            Double[] aux = { dat.lng, dat.lat };
            data.add( aux );
        }

        return trans.MathTransform.TransformList(data);
    }
}
