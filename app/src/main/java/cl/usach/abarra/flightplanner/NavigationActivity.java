package cl.usach.abarra.flightplanner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.ThemedSpinnerAdapter;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.R;
import cl.usach.abarra.flightplanner.engine.NavigationService;
import cl.usach.abarra.flightplanner.model.Waypoint;

public class NavigationActivity extends AppCompatActivity {

    List<Waypoint> waypoints;
    LatLng lastLocation;

    private TextView goalView;
    private TextView actualPosView;
    private TextView gapView;



    NavigationService navigationService;
    boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            NavigationService.NavigationBinder navigationBinder = (NavigationService.NavigationBinder) service;
            navigationService = navigationBinder.getService();
            mBound = true;
            runThread();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        waypoints = bundle.getParcelableArrayList("waypoints");
        lastLocation = bundle.getParcelable("lastLoc");


        System.out.println("Estoy aqui");

        actualPosView = (TextView) findViewById(R.id.actual_orientation);
        gapView = (TextView) findViewById(R.id.left_distance);
        goalView = (TextView) findViewById(R.id.goal);

        Intent intent = new Intent(NavigationActivity.this, NavigationService.class);
        intent.putParcelableArrayListExtra("waypoints", (ArrayList<? extends Parcelable>) waypoints);
        intent.putExtra("lastLoc", lastLocation);
        //startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void runThread(){

    }
}
