package cl.usach.abarra.flightplanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapEditorActivity extends Activity implements MapEditorFragment.OnMapEditorFragmentListener{

    private Bundle intentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intentData = getIntent().getExtras();
        System.out.println("Intent: "+intentData.toString());
        LatLng camPosi = (LatLng) intentData.get("camPos");
        float cameZoom = intentData.getFloat("camZoom");
        setContentView(R.layout.activity_map_editor);
        MapEditorFragment mapEditorFragment = MapEditorFragment.newInstance(camPosi, cameZoom);
        getFragmentManager().beginTransaction().replace( R.id.editor_container, mapEditorFragment).commit();
    }

    @Override
    public void onMapEditorFragmentInteraction(List<LatLng> route, List<Polygon> polygonList) {

    }

    @Override
    public void onMapEditorFragmentCanceled(LatLng camPos, float camZoom) {


    }

    @Override
    public void onMapEditorFragmentFinishResult(List<LatLng> route, List<Polygon> polygonList, LatLng camPos, Float camZoom) {
        System.out.println("Finish recibido");
        List<String>    latitudes = new ArrayList<String>();
        List<String>    longitudes = new ArrayList<String>();
        List<String>    polygons = new ArrayList<String>();
        for (LatLng point : route){
            latitudes.add(String.valueOf(point.latitude));
            longitudes.add(String.valueOf(point.longitude));
        }
        for (Polygon polygon : polygonList){
            polygons.add(polygon.getPoints().toString());
        }
        Intent returnIntent = new Intent();
        returnIntent.putStringArrayListExtra("latitudes", (ArrayList<String>) latitudes);
        returnIntent.putStringArrayListExtra("longitudes", (ArrayList<String>) longitudes);
        returnIntent.putStringArrayListExtra("polygons", (ArrayList<String>) polygons);
        returnIntent.putExtra("camPos", camPos);
        returnIntent.putExtra("camZoom", camZoom);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
