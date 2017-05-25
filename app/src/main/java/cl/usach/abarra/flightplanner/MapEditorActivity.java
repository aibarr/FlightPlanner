package cl.usach.abarra.flightplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
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
    public void onBackPressed() {
        super.onBackPressed();
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("Cerrando Editor")
                .setMessage("Está cerrando el editor, se perderán todos sus progresos. ¿Está seguro que desea continuar?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_CANCELED, null);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                })
                .show();

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
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
