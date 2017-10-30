package cl.usach.abarra.flightplanner.engine;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.model.Waypoint;

public class NavigationService extends Service {

    private List<Waypoint> waypoints;
    private LatLng actualPosition;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private int permissionCheck;

    private static String LOG_TAG = "NavigationService";

    private IBinder mBinder = new NavigationBinder();

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public LatLng getActualPosition() {
        return actualPosition;
    }

    public void setActualPosition(LatLng actualPosition) {
        this.actualPosition = actualPosition;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public LatLng getGoal() {
        return goal;
    }

    public void setGoal(LatLng goal) {
        this.goal = goal;
    }

    private double distance;
    private LatLng goal;

    private Bundle bundle;

    public NavigationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, " OnCreate");

        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            //ActivityCompat.requestPermissions(navigationService.getApplication(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    actualPosition = new LatLng(location.getLatitude(),location.getLongitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {


                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("Iniciando servicio on Bind");
        Log.v(LOG_TAG, "Iniciado OnBind");

        Toast.makeText(getApplicationContext(), "Hola", Toast.LENGTH_LONG).show();
        bundle = intent.getExtras();
        waypoints = bundle.getParcelableArrayList("waypoints");
        this.actualPosition = bundle.getParcelable("lastLoc");

        //Iniciar Thread que realiza la navegación
        Thread navigationThread = new Thread(){
            @Override
            public void run() {
                super.run();
                for (Waypoint waypoint: waypoints){
                    goal = waypoint.getPosition();
                    while (!goalIsNear(waypoint.getPosition(), actualPosition, 3.0)){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        };

        navigationThread.start();

        /*Handler handler = new Handler();
        handler.post(new NavigationHandler(waypoints, this, handler));*/
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "Hola", Toast.LENGTH_LONG).show();
        Log.v(LOG_TAG, "Iniciado OnStartCommand");
        System.out.println("Iniciando servicio On Command");
        bundle = intent.getExtras();
        waypoints = bundle.getParcelableArrayList("waypoints");

        //Iniciar Thread que realiza la navegación
        Handler handler = new Handler();
        handler.post(new NavigationHandler(waypoints, this, handler));
        return super.onStartCommand(intent, flags, startId);

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("Servicio muerto");
    }

    public class NavigationBinder extends Binder {
        public NavigationService getService(){
            return NavigationService.this;
        }
    }

    //Handler para la navegación

    public class NavigationHandler implements Runnable{

        List<Waypoint> waypoints;
        private Handler handler;
        private NavigationService navigationService;
        LatLng actual;




        public NavigationHandler(List<Waypoint> waypoints, NavigationService navigationService, Handler handler) {
            this.waypoints = waypoints;
            this.navigationService = navigationService;
            this.handler = handler;
            this.actual = navigationService.getActualPosition();
        }

        @Override
        public void run() {

            waypoints = new ArrayList<>(navigationService.getWaypoints());




            //Navegar cada punto en orden
            for (Waypoint waypoint: waypoints){
                while(!goalIsNear(waypoint.getPosition(),actual,3.0)){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }



        private boolean goalIsNear(LatLng actual, LatLng goal, double gap){
            double distance = SphericalUtil.computeDistanceBetween(actual,goal);
            System.out.println("estamos a " + String.valueOf(distance) + " metros");
            navigationService.setDistance(distance);
            if (distance < gap){
                return true;
            }else {
                return false;
            }
        }
    }

    private boolean goalIsNear(LatLng actual, LatLng goal, double gap){
        double distance = SphericalUtil.computeDistanceBetween(actual,goal);
        System.out.println("estamos a " + String.valueOf(distance) + " metros");
        this.setDistance(distance);
        if (distance < gap){
            return true;
        }else {
            return false;
        }
    }
}
