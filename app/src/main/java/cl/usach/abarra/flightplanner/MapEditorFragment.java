package cl.usach.abarra.flightplanner;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
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
import android.text.Editable;
import android.text.TextWatcher;
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


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


import cl.usach.abarra.flightplanner.model.FlightLine;
import cl.usach.abarra.flightplanner.model.FlightPlan;
import cl.usach.abarra.flightplanner.model.FlightPolygon;
import cl.usach.abarra.flightplanner.util.GridPolygon;
import cl.usach.abarra.flightplanner.util.MarkerGenerator;
import cl.usach.abarra.flightplanner.util.PlanArchiver;
import cl.usach.abarra.flightplanner.model.Waypoint;
import cl.usach.abarra.flightplanner.util.PointLatLngAlt;
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

    private FlightPlan fPlan;
    private List<Waypoint> waypoints;
    private List<FlightLine> fLines;
    private List<FlightPolygon> fPolygons;
    private List<Character> order;

    private Marker homeMarker;
    private MarkerOptions homeMarkerOpt;
    private int ptsCount;


    private Stack  undoStack;

    private static final int POINT = 0;
    private static final int POLYGON_POINT = 1;
    private static final int POLYGON = 2;

    private static final int WRITE_REQUEST = 0;

    private int permissionCheck;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private static final int LOCATION_REQUEST = 0;

    private static final LatLng OFICINA = new LatLng(-33.4258741,-70.6185903);

    //Key para la elevacion
    private static final String API_KEY = "AIzaSyBITRYAdWiqqRk_lj_JeVFDDKG2degBZyE";

    private LatLng lastLocation;

    //Lista de polígonos creados durante la creación del plan de vuelo

    //Listas para paso entre activities
    private Bundle bundle;
    private Bundle saved;

    //Para guardado de instancias
    private LatLng target;
    private float zoom;

    //Botonería del fragment
    private Button createLine;
    private Button createPolygon;
    private Button undo;
    private Button  homeButton;


    //Textos del fragment
    TextView statusBar;
    TextView distanceText;

    //Para el calculo de la grilla
    private Double gridOrientation;
    private Double gridDistance = 10.0d;

    //Utilitarios del Bottom Sheet
    private LinearLayout bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private Button bEraseMarker;
    private Button bApplyChanges;

    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etSpeed;
    private EditText etHeight;

    //otros
    SharedPreferences preferences;

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

    public static MapEditorFragment newInstance(LatLng camPos, float camZoom, FlightPlan fPlan){
        MapEditorFragment fragment = new MapEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable("plan", fPlan);
        args.putParcelable("target", camPos);
        args.putFloat("zoom", camZoom);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //obtengamos las preferencias
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        gridOrientation = Double.valueOf(preferences.getString(SettingsFragment.DEFAULT_ANGLE, "45.0"));
        gridDistance = Double.valueOf(preferences.getString(SettingsFragment.DEFAULT_DISTANCE, "10.0"));
        undoStack = new Stack();
        fPlan = new FlightPlan();
        fPolygons = new ArrayList<FlightPolygon>();
        fLines = new ArrayList<FlightLine>();
        order = new ArrayList<Character>();

        waypoints = new ArrayList<Waypoint>();

        //iniciemos el marcador de Home
        homeMarkerOpt = new MarkerOptions().draggable(false).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        //Variables para el Bottom Sheet
        bottomSheet = (LinearLayout) getActivity().findViewById(R.id.bs_bottom_sheet);
        etLatitude = (EditText) getActivity().findViewById(R.id.et_latitud);
        etLongitude = (EditText) getActivity().findViewById(R.id.et_longitud);
        etSpeed = (EditText) getActivity().findViewById(R.id.et_speed);
        etHeight = (EditText) getActivity().findViewById(R.id.et_height);
        bEraseMarker = (Button) getActivity().findViewById(R.id.bs_erase_marker);
        bApplyChanges = (Button) getActivity().findViewById(R.id.bs_apply_marker);

        //Set Bottom Sheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        ptsCount = 0;

        if (getArguments() != null) {
            bundle = getArguments();
            target = (LatLng)bundle.get("target");
            zoom = bundle.getFloat("zoom");
            FlightPlan auxFplan =(FlightPlan) bundle.get("plan");
            if(auxFplan != null){
                fPlan = auxFplan;
                waypoints = fPlan.getRoute();
                //TODO: añadir objetos a mapa
            }
        }

        //TODO: saved instance

        if (savedInstanceState != null){
            saved = savedInstanceState;

            target = (LatLng) saved.getParcelable("target");
            zoom = saved.getFloat("zoom");

            waypoints = saved.getParcelableArrayList("waypoints");
            if(waypoints==null){
                waypoints = new ArrayList<Waypoint>();
            }

        }

        setHasOptionsMenu(true);

        permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        } else {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    lastLocation = new LatLng(location.getLatitude(),location.getLongitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    //TODO: Manejar en caso de que cambien los estados o la ubicación no esté disponible


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
                    //TODO: Guardar Mapa
                    if(!waypoints.isEmpty()){
                        PlanArchiver planArchiver = new PlanArchiver(waypoints, fLines, fPolygons);
                        String texToast;
                        if ((texToast = planArchiver.savePlan(Environment.getExternalStorageDirectory().getPath()+"/FlightPlanner/"))!=null){
                            String textToast = "Se ha creado el archivo "+ texToast;
                            Toast toast = Toast.makeText(getActivity(), textToast, Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }else{
                        String textToast = "No hay nada para guardar";
                        Toast toast = Toast.makeText(getActivity(), textToast, Toast.LENGTH_LONG);
                        toast.show();
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
        distanceText = (TextView) rootView.findViewById(R.id.distance_text);
        calculateDistance();


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
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
                }

            }
        });


        //region line
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

                //Crear variable

                final FlightLine fLine = new FlightLine();

                statusBar.setText("Creando Ruta");

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        ptsCount++;
                        Waypoint waypoint = new Waypoint(latLng, 0, 0.0, Waypoint.WAYPOINT);
                        waypoints.add(waypoint);
                        fPlan.setRoute(waypoints);
                        calculateDistance();
                        fLine.addPoint(latLng, map);
                        undoStack.push(POINT);
                        if (undo.getVisibility()==Button.INVISIBLE) {
                            undo.setVisibility(Button.VISIBLE);
                            setUndoButton();
                        }
                        calculateDistance();
                        getElevation(waypoint);
                    }
                });
                fLines.add(fLine);
                order.add(FlightPlan.LINE);
            }
        });

        //endregion

        //region Polygon
        //Flag para Poligono "2"
        final Button finishButton = (Button) rootView.findViewById(R.id.finish_Button);
        final EditText orientationInput = (EditText) rootView.findViewById(R.id.orienation_input);
        orientationInput.setText(gridOrientation.toString());
        createPolygon = (Button) rootView.findViewById(R.id.create_polygon);
        createPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Seteamos Flag para avisar que estoy creando un polígono
                buttonFlag = 2;

                final FlightPolygon polygon = new FlightPolygon();
                orientationInput.setVisibility(EditText.VISIBLE);
                orientationInput.setBackgroundColor(Color.WHITE);

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        polygon.addPoint(latLng, map);
                    }
                });

                undoStack.add(POLYGON_POINT);

                orientationInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!s.toString().isEmpty()){
                            if(s != null && ((Double.parseDouble(s.toString())<=360)||(Double.parseDouble(s.toString())>=0))){
                                gridOrientation = Double.parseDouble(s.toString());
                            } else if (s != null && ((Double.parseDouble(s.toString())>360)||(Double.parseDouble(s.toString())<0))){
                                gridOrientation = 45.0d;
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

                        switch (polygon.size()){
                            case 0:
                                map.setOnMapClickListener(null);

                                statusBar.setText("");
                                finishButton.setVisibility(View.INVISIBLE);
                                orientationInput.setVisibility(View.INVISIBLE);
                                buttonFlag  =  0;
                                break;
                            case 1:
                                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        map.setOnMapClickListener(null);
                                        statusBar.setText("");
                                        finishButton.setVisibility(View.INVISIBLE);
                                        orientationInput.setVisibility(View.INVISIBLE);
                                        buttonFlag  =  0;
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
                                        if(fPolygons != null){
                                            fPolygons.add(polygon);
                                        } else {
                                            fPolygons = new ArrayList<>();
                                            fPolygons.add(polygon);
                                        }
                                        order.add(FlightPlan.POLYGON);
                                        map.setOnMapClickListener(null);
                                        statusBar.setText("");
                                        polygon.addGrid(map ,gridDistance, gridOrientation, GridPolygon.StartPosition.Home, new PointLatLngAlt(lastLocation.latitude,lastLocation.longitude));
                                        List<LatLng> auxP = new ArrayList<>(polygon.getGrid());
                                        for (LatLng point: auxP){
                                            waypoints.add(new Waypoint(point, 0,0, Waypoint.WAYPOINT));
                                        }
                                        fPlan.setRoute(waypoints);
                                        calculateDistance();
                                        finishButton.setVisibility(View.INVISIBLE);
                                        orientationInput.setVisibility(View.INVISIBLE);
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

        //endregion

        //region clear

        Button clear = (Button) rootView.findViewById(R.id.clear_button);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Limpio el mapa
                map.setOnMapClickListener(null);
                map.clear();
                if(waypoints != null) waypoints.clear();
                if(order != null) order.clear();
                if(fLines != null) fLines.clear();
                if(fPolygons != null) fPolygons.clear();

                ptsCount = 0;

                //desactivo cosas
                finishButton.setVisibility(Button.INVISIBLE);
                undo.setVisibility(Button.INVISIBLE);
                orientationInput.setVisibility(EditText.INVISIBLE);
                calculateDistance();
            }
        });

        //endregion

        undo = (Button) rootView.findViewById(R.id.undo_button);

        //region HOME
        homeButton = (Button) rootView.findViewById(R.id.set_home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (map != null){
                    if (lastLocation==null){
                        //TODO:indicar que no tengo una ultima posición
                        lastLocation = OFICINA;
                    }
                    System.out.println("Last Location is" + lastLocation.toString());
                    if (homeMarker != null){
                        homeMarker.setPosition(lastLocation);
                    }else{
                        homeMarker = map.addMarker(homeMarkerOpt.position(lastLocation));
                    }
                    if (fPlan != null){
                        fPlan.setHome(new LatLng(lastLocation.latitude, lastLocation.longitude), 0, 0);
                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 17));
                }
            }
        });
        //endregion

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

    //region memAdmin

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

        target = map.getCameraPosition().target;
        zoom = map.getCameraPosition().zoom;
        outState.putParcelable("target", target);
        outState.putFloat("zoom", zoom);

        outState.putParcelableArrayList("waypoints", (ArrayList<? extends Parcelable>) waypoints);
        outState.putParcelableArrayList("lines", (ArrayList<? extends Parcelable>) fLines);
        outState.putParcelableArrayList("polygons", (ArrayList<? extends Parcelable>) fPolygons);
        outState.putParcelable("plan", fPlan);

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

    //endregion

    //Funcion para cerrar editor y terminar mapa
    public void closeEditor(){
        if (fLines.isEmpty() && fPolygons.isEmpty()){
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
                            if(fLines != null){
                                fPlan.setfLines(fLines);
                            }

                            if(fPolygons != null){
                                fPlan.setfPolygons(fPolygons);
                            }

                            if (waypoints != null){
                                fPlan.setRoute(waypoints);
                            }

                            if(order != null){
                                fPlan.setOrder(order);
                            }

                            mListener.onMapEditorFragmentFinishResult(fPlan, map.getCameraPosition().target, map.getCameraPosition().zoom);
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
                switch ((int)undoStack.peek()){
                    case POINT:

                        break;
                    case POLYGON:
                        //TODO: agregar algoritmo cuando implemente ruta de polígono

                        break;
                }
                ptsCount--;
                calculateDistance();
                if (ptsCount<=0) undo.setVisibility(Button.INVISIBLE);
            }
        });
    }


    //TODO:optimizar cargador
    private boolean loadPlan(PlanArchiver planLoader){
        waypoints = new ArrayList<>(planLoader.getWaypoints());
        for (Waypoint waypoint: waypoints){
        }
        int markCount = 0;
        return true;
    }

    //Funciones para la edicion de marcadores
    private void editMarker(final Marker marker){
        LatLng position = marker.getPosition();
        int index = -1;
        //Ubiquemos en qué linea está
        for (FlightLine fLine: fLines){
            if (fLine.markerBelongs(marker)) index = fLines.indexOf(fLine);
        }

        if (!(index < 0)){
            Waypoint waypoint = waypoints.get(index);
            etLatitude.setText(Double.toString(position.latitude), TextView.BufferType.EDITABLE);
            etLongitude.setText(Double.toString(position.longitude), TextView.BufferType.EDITABLE);
            etSpeed.setText(Integer.toString(waypoint.getSpeed()), TextView.BufferType.EDITABLE);
            etHeight.setText(Double.toString(waypoint.getHeight()), TextView.BufferType.EDITABLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bEraseMarker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    killMarker(marker);
                }
            });
            final int finalIndex = index;
            bApplyChanges.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Waypoint waypoint = new Waypoint();
                    waypoint.setPosition(new LatLng(Double.parseDouble(String.valueOf(etLatitude.getText())), Double.parseDouble(String.valueOf(etLongitude.getText()))));
                    waypoint.setHeight(Double.parseDouble(String.valueOf(etHeight.getText())));
                    waypoint.setSpeed(Integer.parseInt(String.valueOf(etSpeed.getText())));
                    fLines.get(finalIndex).movePoint(marker, waypoint.getPosition());
                }
            });
        }
    }

    private void killMarker(Marker marker){
        for ( FlightLine fline: fLines){
            if(fline.markerBelongs(marker)){
                fline.removePoint(marker, map);
            }
        }
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

//    private void moveMarker(Marker marker, Waypoint waypoint){
//        int index = ptsRoute.indexOf(marker.getPosition());
//        ptsRoute.set(index, waypoint.getPosition());
//        marker.setPosition(waypoint.getPosition());
//        waypoints.set(index, waypoint);
//        route.setPoints(ptsRoute);
//        calculateDistance();
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
//    }

    //Funciones para calcular distancia

    private void calculateDistance(){
        if (distanceText != null){
            String measureUnit = preferences.getString(SettingsFragment.DEFAULT_UNITS, "0" );
            switch (measureUnit){
                case "0":
                    distanceText.setText("Distancia: " + String.format( "%.2f", fPlan.calculateDistance() ) + "(m)");
                    break;
                case "1":
                    distanceText.setText("Distancia: " + String.format( "%.2f", fPlan.calculateDistance()/1000 ) + "(km)");
                    break;
                case "2":
                    break;
                default:
            }

        }
    }

    private final void getElevation(final Waypoint waypoint){
        String url = "https://maps.googleapis.com/maps/api/elevation/json?locations="+ waypoint.getPosition().latitude+","+waypoint.getPosition().longitude+"&key="+API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast toast;
                        try {
                            String status = response.getString("status");
                            switch (status){
                                case "OK":
                                    JSONArray results = response.getJSONArray("results");
                                    JSONObject result = results.getJSONObject(0);
                                    Double elevation = result.getDouble("elevation");

                                    waypoint.setHeight(elevation+ Double.valueOf(preferences.getString("min_height", "15.0")));

                                    toast = Toast.makeText(getContext(), elevation.toString(), Toast.LENGTH_LONG );
                                    toast.show();
                                    break;
                                case "REQUEST_DENIED":
                                    toast = Toast.makeText(getContext(), "Solicitud Denegada", Toast.LENGTH_LONG );
                                    toast.show();
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        queue.add(jsObjRequest);
    }

    private void addPoint(GoogleMap map, int pointClass){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public interface OnMapEditorFragmentListener {

        void onMapEditorFragmentInteraction(List<LatLng> route, List<Polygon> polygonList);

        void onMapEditorFragmentCanceled(LatLng camPos, float camZoom);

        void onMapEditorFragmentFinishResult(FlightPlan fPlan, LatLng camPos, Float camZoom);
    }


}
