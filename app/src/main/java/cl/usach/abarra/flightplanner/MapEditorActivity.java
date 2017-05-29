package cl.usach.abarra.flightplanner;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapEditorActivity extends AppCompatActivity implements MapEditorFragment.OnMapEditorFragmentListener{

    private Bundle intentData;

    private MapEditorFragment mapEditorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intentData = getIntent().getExtras();
        System.out.println("Intent: "+intentData.toString());
        LatLng camPosi = (LatLng) intentData.get("camPos");
        float cameZoom = intentData.getFloat("camZoom");
        setContentView(R.layout.activity_map_editor);
        mapEditorFragment = MapEditorFragment.newInstance(camPosi, cameZoom);
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
    public void onMapEditorFragmentCanceled(LatLng camPos, float camZoom) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("camPos", camPos);
        returnIntent.putExtra("camZoom", camZoom);
        setResult(AppCompatActivity.RESULT_CANCELED, returnIntent);
        finish();


    }

    @Override
    public void onMapEditorFragmentFinishResult(List<LatLng> route, List<Polygon> polygonList, LatLng camPos, Float camZoom) {
        System.out.println("Finish recibido");
        ArrayList<Double>    latitudes = new ArrayList<Double>();
        ArrayList<Double>    longitudes = new ArrayList<Double>();
        List<String>    polygons = new ArrayList<String>();
        for (LatLng point : route){
            latitudes.add(point.latitude);
            longitudes.add(point.longitude);
        }
        for (Polygon polygon : polygonList){
            polygons.add(polygon.getPoints().toString());
        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra("latitudes", latitudes);
        returnIntent.putExtra("longitudes", longitudes);
        returnIntent.putStringArrayListExtra("polygons", (ArrayList<String>) polygons);
        returnIntent.putExtra("camPos", camPos);
        returnIntent.putExtra("camZoom", camZoom);
        setResult(AppCompatActivity.RESULT_OK, returnIntent);
        finish();
    }
}
