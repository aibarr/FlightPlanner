package cl.usach.abarra.flightplanner.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alfredo Barra on 30/04/2017. Pre-grade project.
 */


//Arreglo de puntos para el plan de vuelo
public class FlightPlan implements Parcelable {
    private Waypoint home;
    private List<Waypoint> route;

    private List<FlightPolygon> fPolygons;
    private List<FlightLine> fLines;

    private List<Character> order;

    public static final Character POLYGON = 'p';
    public static final Character LINE = 'l';

    private int pointer;

    //region CONSTRUCTOR

    //Constructor de plan de vuelo
    public FlightPlan(){
        this.route = new ArrayList<>();
        this.fLines = new ArrayList<>();
        this.fPolygons = new ArrayList<>();
        this.order = new ArrayList<>();
        pointer = 0;
    }

    public FlightPlan(Waypoint home, List<Waypoint> route, List<FlightPolygon> fPolygons, List<FlightLine> fLines, List<Character> order) {
        this.home = home;
        this.route = route;
        this.fPolygons = fPolygons;
        this.fLines = fLines;
        this.order = order;
    }

    protected FlightPlan(Parcel in) {
        home = in.readParcelable(Waypoint.class.getClassLoader());
        route = in.createTypedArrayList(Waypoint.CREATOR);
        fPolygons = in.createTypedArrayList(FlightPolygon.CREATOR);
        fLines = in.createTypedArrayList(FlightLine.CREATOR);
        pointer = in.readInt();
    }

    public static final Creator<FlightPlan> CREATOR = new Creator<FlightPlan>() {
        @Override
        public FlightPlan createFromParcel(Parcel in) {
            return new FlightPlan(in);
        }

        @Override
        public FlightPlan[] newArray(int size) {
            return new FlightPlan[size];
        }
    };

    //endregion

    //region SETTER_GETTER

    public void setHome(Waypoint home) {
        this.home = home;
    }

    public void setHome (LatLng home, int speed, double height){
        this.home = new Waypoint(home, speed, height, Waypoint.HOME);
    }

    public List<Waypoint> getRoute() {
        return route;
    }

    public void setRoute(List<Waypoint> route) {
        this.route = route;
    }

    public void setfPolygons(List<FlightPolygon> fPolygons) {
        this.fPolygons = fPolygons;
    }

    public void setfLines(List<FlightLine> fLines) {
        this.fLines = fLines;
    }

    public void setOrder(List<Character> order) {
        this.order = order;
    }

    //endregion

    public Waypoint nextPoint() {
        this.pointer++;
        return this.route.get(pointer);
    }

    public void addPolygon(FlightPolygon fPoly){
        this.fPolygons.add(fPoly);
        this.order.add(POLYGON);
    }

    public void addLine(FlightLine fLine){
        this.fLines.add(fLine);
        this.order.add(LINE);
    }

    public List<FlightPolygon> getfPolygons() {
        return fPolygons;
    }

    //TODO:recrear mapa
    public void reCreateMap(GoogleMap map){


    }

    public Double calculateDistance(){
        List<LatLng> calcRoute = new ArrayList<LatLng>();

        Double distance = 0.0d;

        if(this.route != null){
            for(Waypoint wp : this.route){
                calcRoute.add(wp.getPosition());
            }
            distance = SphericalUtil.computeLength(calcRoute);

            return distance;

        }else{
            return distance;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(home, flags);
        dest.writeTypedList(route);
        dest.writeTypedList(fPolygons);
        dest.writeTypedList(fLines);
        dest.writeInt(pointer);
    }
}
