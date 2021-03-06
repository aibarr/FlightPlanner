package cl.usach.abarra.flightplanner;

import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.model.FlightPlan;
import cl.usach.abarra.flightplanner.model.Waypoint;

public class MapEditorActivity extends AppCompatActivity implements MapEditorFragment.OnMapEditorFragmentListener{



    private Bundle intentData;



    private MapEditorFragment mapEditorFragment;

    @Override
    protected void onStart() {
        super.onStart();



    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intentData = getIntent().getExtras();
        LatLng target = (LatLng) intentData.get("target");
        float zoom = intentData.getFloat("zoom");
        FlightPlan fPlan = (FlightPlan) intentData.get("plan");
        if (fPlan!=null){
            mapEditorFragment = MapEditorFragment.newInstance(target, zoom, fPlan);
        } else {
            mapEditorFragment = MapEditorFragment.newInstance(target, zoom);
        }

        setContentView(R.layout.activity_map_editor);

        getSupportFragmentManager().beginTransaction().replace( R.id.editor_container, mapEditorFragment).commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mapEditorFragment.closeEditor();
    }

    @Override
    public void onMapEditorFragmentInteraction(List<LatLng> route, List<Polygon> polygonList) {

    }

    @Override
    public void onMapEditorFragmentCanceled(LatLng target, float zoom) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("target", target);
        returnIntent.putExtra("zoom", zoom);
        setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
        finish();
    }

    @Override
    public void onMapEditorFragmentFinishResult(FlightPlan fPlan, LatLng target, Float zoom) {
        System.out.println("Finish recibido");

        Intent returnIntent = new Intent();
        returnIntent.putExtra("plan", fPlan);
        returnIntent.putExtra("target", target);
        returnIntent.putExtra("zoom", zoom);
        setResult(AppCompatActivity.RESULT_OK, returnIntent);
        finish();
    }
}
