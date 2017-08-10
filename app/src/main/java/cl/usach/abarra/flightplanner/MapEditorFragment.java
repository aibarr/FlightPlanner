package cl.usach.abarra.flightplanner;

import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;

import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;


import cl.usach.abarra.flightplanner.util.MarkerGenerator;
import cl.usach.abarra.flightplanner.util.PlanArchiver;
import ir.sohreco.androidfilechooser.ExternalStorageNotAvailableException;
import ir.sohreco.androidfilechooser.FileChooserDialog;


public class MapEditorFragment extends Fragment {

    private OnMapEditorFragmentListener mListener;

    //Flag para botones, con esto sabemos en que estado se encuentra el editor
    // 0 : no se está editando plan de vuelo
    // 1 : se está creando una ruta lineal
    // 2 : se está creando un polígono
    int buttonFlag = 0;

    //Variables globales para el mapa, la ruta de vuelo (route)
    private MapView mapEditorView;
    private GoogleMap map;
    private Polyline route;
    private List<LatLng> ptsRoute;
    private List<Marker> markers;
    private int ptsCount;
    private PolylineOptions optRoute;
    private Float[] heights;
    private Float[] speeds;

    private Stack   undoStack;

    private MarkerGenerator markerGenerator;

    private static final int POINT = 0;
    private static final int POLYGON_POINT = 1;
    private static final int POLYGON = 2;

    private static final int WRITE_REQUEST = 0;

    //Lista de polígonos creados durante la creación del plan de vuelo
    private List<Polygon> polygonList;
    private ArrayList<ArrayList<LatLng>> polygons;

    //TODO: revisar utilidad de esta variable como global
    private PolygonOptions polygonOptions;

    //Listas para paso entre activities
    private Bundle bundle;
    private Bundle saved;

    //Para guardado de instancias
    private LatLng target;
    private float zoom;

    private ArrayList<Double> latitudes;
    private ArrayList<Double> longitudes;

    //Botonería del fragment
    private Button createLine;
    private Button createPolygon;
    private Button undo;
    private Button  homeButton;

    //Textos del fragment
    TextView statusBar;

    //Utilitarios del Bottom Sheet
    private LinearLayout bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private Button bEraseMarker;
    private Button bApplyChanges;

    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etSpeed;

    public MapEditorFragment() {
        // Required empty public constructor
    }

    public static MapEditorFragment newInstance(LatLng camPos, float camZoom) {
        MapEditorFragment fragment = new MapEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable("target", camPos);
        args.putFloat("zoom", camZoom);
        fragment.setArguments(args);
        return fragment;
    }

    public static MapEditorFragment newInstance(LatLng camPos, float camZoom, ArrayList<Double> latitudes, ArrayList<Double> longitudes) {
        MapEditorFragment fragment = new MapEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable("target", camPos);
        args.putFloat("zoom", camZoom);
        args.putSerializable("latitudes", latitudes);
        args.putSerializable("longitudes", longitudes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        latitudes = new ArrayList<Double>();
        longitudes = new ArrayList<Double>();
        ptsRoute= new ArrayList<LatLng>();
        undoStack = new Stack();
        markers = new ArrayList<Marker>();
        polygons = new ArrayList<ArrayList<LatLng>>();

        //Abrimos un generador de marcadores
        markerGenerator = new MarkerGenerator();

        //Variables para el Bottom Sheet
        bottomSheet = (LinearLayout) getActivity().findViewById(R.id.bs_bottom_sheet);
        etLatitude = (EditText) getActivity().findViewById(R.id.et_latitud);
        etLongitude = (EditText) getActivity().findViewById(R.id.et_longitud);
        etSpeed = (EditText) getActivity().findViewById(R.id.et_speed);
        bEraseMarker = (Button) getActivity().findViewById(R.id.bs_erase_marker);
        bApplyChanges = (Button) getActivity().findViewById(R.id.bs_apply_marker);

        //Set Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        polygonOptions = new PolygonOptions();
        polygonList = new ArrayList<Polygon>();

        ptsCount = 0;

        if (getArguments() != null) {
            bundle = getArguments();
            target = (LatLng)bundle.get("target");
            zoom = bundle.getFloat("zoom");
            latitudes = (ArrayList<Double>) bundle.getSerializable("latitudes");
            longitudes = (ArrayList<Double>) bundle.getSerializable("longitudes");
            if (latitudes!=null && !(latitudes.isEmpty())){
                for(int i = 0; i < longitudes.size(); i++){
                    ptsCount++;
                    ptsRoute.add(new LatLng( latitudes.get(i), longitudes.get(i)));
                }
            }
        }

        if (savedInstanceState != null){
            saved = savedInstanceState;
            System.out.println("Bundle rec: "+saved.toString());
            target = (LatLng) saved.getParcelable("target");
            System.out.println("Target rec: "+target.toString());
            zoom = savedInstanceState.getFloat("zoom");
            latitudes = (ArrayList<Double>) saved.getSerializable("latitudes");
            longitudes = (ArrayList<Double>) saved.getSerializable("longitudes");
            System.out.println("Lat Rec "+latitudes.toString());
            for(int i = 0; i < longitudes.size(); i++){
                ptsCount++;
                ptsRoute.add(new LatLng( latitudes.get(i), longitudes.get(i)));
            }
            System.out.println("pts rec: "+ptsRoute.toString());
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map_editor_menu, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int permissionCheck = 0;
        switch (item.getItemId()){
            case R.id.save_plan:
                permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED){
                    PlanArchiver planArchiver = new PlanArchiver(ptsRoute, heights, speeds, polygons );
                    String texToast;
                    if ((texToast = planArchiver.savePlan(Environment.getExternalStorageDirectory().getPath()+"/FlightPlanner/"))!=null){
                        Toast toast = Toast.makeText(getActivity(), texToast, Toast.LENGTH_LONG);
                    }
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST);
                }

                break;

            case R.id.load_plan:
                permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                final PlanArchiver planLoader = new PlanArchiver();
                String [] fileList;
                if (permissionCheck == PackageManager.PERMISSION_GRANTED){
                    FileChooserDialog.Builder builder = new FileChooserDialog.Builder(FileChooserDialog.ChooserType.FILE_CHOOSER, new FileChooserDialog.ChooserListener() {
                        @Override
                        public void onSelect(String path) {
                            System.out.println(path);
                            planLoader.loadPlan(path);
                            loadPlan(planLoader);
                        }
                    });
                    builder.setInitialDirectory(new File(Environment.getExternalStorageDirectory().getPath()+"/FlightPlanner/"));
                    try {
                        builder.build().show(getActivity().getSupportFragmentManager(), null);
                    } catch (ExternalStorageNotAvailableException e) {
                        e.printStackTrace();
                    }

                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST);
                }
                break;
            case R.id.close_editor:
                closeEditor();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_map_editor, container, false);

        statusBar = (TextView) rootView.findViewById(R.id.status_barr);

        //Obteniendo el mapa
        mapEditorView = (MapView) rootView.findViewById(R.id.map_editor_view);
        mapEditorView.onCreate(savedInstanceState);
        mapEditorView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (savedInstanceState != null){

        }

        optRoute = new PolylineOptions();

        mapEditorView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                UiSettings mapSettings = map.getUiSettings();
                mapSettings.setMapToolbarEnabled(false);
                mapSettings.setRotateGesturesEnabled(false);
                mapSettings.setTiltGesturesEnabled(false);
                if (target != null){
                    System.out.println("Target play: "+target);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
                    System.out.println("Target actual "+map.getCameraPosition().toString());
                }
                System.out.println("Pts Play: "+ptsRoute.toString());
                route = map.addPolyline(optRoute);
                if (!ptsRoute.isEmpty()){
                    if (undo.getVisibility()==Button.INVISIBLE) {
                        undo.setVisibility(Button.VISIBLE);
                        setUndoButton();
                    }
                    int markCount = 0;
                    for (LatLng point : ptsRoute){
                        markCount++;
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(markCount));
                        Marker marker = map.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        markers.add(marker);
                        undoStack.push(POINT);

                    }
                }

                route.setPoints(ptsRoute);
                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        editMarker(marker);
                        return false;
                    }
                });
            }
        });



        //Flag para Linea "1"
        createLine = (Button) rootView.findViewById(R.id.create_line);
        createLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (buttonFlag){
                    case 2:
                        //TODO: Esta pasando del click y limpia todo
                        rootView.findViewById(R.id.finish_Button).performClick();
                        System.out.println("Ya hice clic");
                        break;
                    default:
                        break;
                }

                statusBar.setText("Creando Ruta");
                optRoute = new PolylineOptions();

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        ptsCount++;
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(ptsCount));
                        Marker marker = map.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        ptsRoute.add(latLng);
                        markers.add(marker);
                        undoStack.push(POINT);
                        route.setPoints(ptsRoute);
                        if (undo.getVisibility()==Button.INVISIBLE) {
                            undo.setVisibility(Button.VISIBLE);
                            setUndoButton();
                        }
                    }
                });




            }
        });

        //Flag para Poligono "2"
        final Button finishButton = (Button) rootView.findViewById(R.id.finish_Button);
        createPolygon = (Button) rootView.findViewById(R.id.create_polygon);
        createPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Seteamos Flag para avisar que estoy creando un polígono
                buttonFlag = 2;

                statusBar.setText("Creando Poligono");

                //Lista de vertices para el polígono
                final ArrayList<LatLng> vertices = new ArrayList<LatLng>();

                if  (route==null){
                    route = map.addPolyline(optRoute = new PolylineOptions());
                }

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    PolygonOptions tempPolygonOptions = new PolygonOptions();
                    Polygon tempPolygon;

                    @Override
                    public void onMapClick(LatLng latLng) {

                        Marker marker = map.addMarker(new MarkerOptions().position(latLng));
                        markers.add(marker);
                        undoStack.push(POLYGON_POINT);
                        vertices.add(latLng);
                        if (vertices.size()>2){
                            if(tempPolygon==null){
                                tempPolygon = map.addPolygon(tempPolygonOptions.addAll(vertices));
                                if (!polygonList.isEmpty()){
                                    polygonList.remove(polygonList.size()-1);
                                }
                                polygonList.add(tempPolygon);
                            }else {
                                tempPolygon.setPoints(vertices);
                                if (!polygonList.isEmpty()){
                                    polygonList.remove(polygonList.size()-1);
                                }
                                polygonList.add(tempPolygon);
                            }

                        }

                    }
                });

                finishButton.setVisibility(Button.VISIBLE);
                finishButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        AlertDialog.Builder builder;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
                        } else {
                            builder = new AlertDialog.Builder(getActivity());
                        }

                        builder.setTitle("Terminar Polígono")
                                .setMessage("Tiene un polígono sin completar. ¿Dejar de dibujar polígono actual?")
                                .setIcon(android.R.drawable.ic_dialog_alert);

                        switch (vertices.size()){
                            case 0:
                                break;
                            case 1:
                                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        buttonFlag  =  0;
                                        ptsRoute.addAll(vertices);
                                        map.setOnMapClickListener(null);
                                        statusBar.setText("");
                                        finishButton.setVisibility(View.GONE);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        return;
                                    }
                                })
                                .show();
                                break;
                            default:
                                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        buttonFlag  =  0;
                                        ptsRoute.addAll(vertices);
                                        polygons.add(vertices);
                                        route.setPoints(ptsRoute);
                                        map.setOnMapClickListener(null);
                                        statusBar.setText("");
                                        finishButton.setVisibility(View.GONE);
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
                    }
                });
            }
        });

        Button clear = (Button) rootView.findViewById(R.id.clear_button);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.setOnMapClickListener(null);
                map.clear();
                route = map.addPolyline(optRoute);
                ptsRoute.clear();
                ptsCount = 0;
                markers.clear();
                undo.setVisibility(Button.INVISIBLE);
            }
        });

        undo = (Button) rootView.findViewById(R.id.undo_button);

        homeButton = (Button) rootView.findViewById(R.id.set_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });




        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }


    /*// TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onMapEditorFragmentInteraction(uri);
        }
    }*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapEditorFragmentListener) {
            mListener = (OnMapEditorFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (ptsRoute!= null){
            for (LatLng point: ptsRoute){
                latitudes.add(point.latitude);
                longitudes.add(point.longitude);
            }
        }
        System.out.println("Guardando");
        target = map.getCameraPosition().target;
        System.out.println("Target "+ target.toString());
        zoom = map.getCameraPosition().zoom;
        System.out.println("Zoom"+zoom);
        outState.putParcelable("target", target);
        outState.putFloat("zoom", zoom);
        outState.putSerializable("latitudes", latitudes);
        outState.putSerializable("longitudes", longitudes);
        System.out.println(outState.toString());
        mapEditorView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }



    @Override
    public void onResume() {
        super.onResume();
        mapEditorView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapEditorView.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapEditorView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapEditorView.onDestroy();
    }

    //Funcion para cerrar editor y terminar mapa
    public void closeEditor(){
        if (ptsRoute.isEmpty() || ptsRoute==null){
            mListener.onMapEditorFragmentCanceled(map.getCameraPosition().target, map.getCameraPosition().zoom);
        }else{
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Cerrar Editor")
                    .setMessage("Está cerrando el editor, ¿Qué desea hacer?")
                    .setPositiveButton("Guardar y cerrar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onMapEditorFragmentFinishResult(ptsRoute, polygonList, map.getCameraPosition().target, map.getCameraPosition().zoom);
                        }
                    })
                    .setNegativeButton("Cerrar sin guardar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onMapEditorFragmentCanceled(map.getCameraPosition().target, map.getCameraPosition().zoom);
                        }
                    })
                    .setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    })
            .show();
        }
    }

    private void setUndoButton(){
        //undo.setVisibility(Button.VISIBLE);
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = markers.size()-1;
                switch ((int)undoStack.peek()){
                    case POINT:
                        markers.get(index).remove();
                        markers.remove(index);
                        ptsRoute.remove(index);
                        route.setPoints(ptsRoute);
                        break;
                    case POLYGON:
                        //TODO: agregar algoritmo cuando implemente ruta de polígono
                        break;
                }
                ptsCount--;
                if (ptsCount<=0) undo.setVisibility(Button.INVISIBLE);
            }
        });
    }

    private boolean loadPlan(PlanArchiver planLoader){
        ptsRoute = new ArrayList<LatLng>(planLoader.getRoute());
        route = map.addPolyline(optRoute);
        route.setPoints(ptsRoute);
        for (LatLng point : ptsRoute){
            Marker marker = map.addMarker(new MarkerOptions().position(point));
            markers.add(marker);
        }
        return true;
    }

    private void editMarker(final Marker marker){
        LatLng position = marker.getPosition();
        etLatitude.setText(Double.toString(position.latitude), TextView.BufferType.EDITABLE);
        etLongitude.setText(Double.toString(position.longitude), TextView.BufferType.EDITABLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bEraseMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killMarker(marker);
            }
        });
        bApplyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveMarker(marker, Double.parseDouble(etLatitude.getText().toString()), Double.parseDouble(etLongitude.getText().toString()) );
            }
        });
    }

    public void killMarker(Marker marker){
        LatLng position = marker.getPosition();
        markers.remove(marker);
        marker.remove();
        ptsRoute.remove(position);
        route.setPoints(ptsRoute);
        ptsCount--;
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void moveMarker(Marker marker, Double latitude, Double longitude){
        LatLng position = marker.getPosition();
        LatLng newPosition =new LatLng(latitude, longitude);
        marker.setPosition(newPosition );
        ptsRoute.set(ptsRoute.indexOf(position), newPosition);
        route.setPoints(ptsRoute);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }





    public interface OnMapEditorFragmentListener {

        void onMapEditorFragmentInteraction(List<LatLng> route, List<Polygon> polygonList);

        void onMapEditorFragmentCanceled(LatLng camPos, float camZoom);

        void onMapEditorFragmentFinishResult(List<LatLng> route, List<Polygon> polygonList, LatLng camPos, Float camZoom);
    }
}
