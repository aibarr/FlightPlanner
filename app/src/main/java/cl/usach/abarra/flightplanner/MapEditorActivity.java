package cl.usach.abarra.flightplanner;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

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
}
