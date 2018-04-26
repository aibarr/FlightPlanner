package cl.usach.abarra.flightplanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.model.FlightPlan;
import cl.usach.abarra.flightplanner.model.FlightPolygon;
import cl.usach.abarra.flightplanner.model.Waypoint;
import cl.usach.abarra.flightplanner.util.MarkerGenerator;


public class MapPlannerFragment extends Fragment {

    MapView mapPlannerView;
    private GoogleMap googleMap;
    private OnMapReadyCallback mapReady;
    private Polyline ruta;
    private PolylineOptions optRuta;
    private LatLng target;
    private float zoom;

    //Datos a mostrar
    private List<LatLng> ptsRoute;
    private List<Waypoint> waypoints;

    private FlightPlan fPlan;

    private MarkerGenerator markerGenerator;
    private int permissionCheck;

    //Constantes de Inicio del editor
    public static final int NEW_PLAN = 0;
    public static final int EDIT_PLAN = 1;
    public static final int LOAD_PLAN = 2;

    private static final int LOCATION_REQUEST = 0;



    private OnMapPlannerInteractionListener mListener;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private LatLng lastLocation;

    private TextView distanceText;
    private TextView lastLocationText;
    private Button navTestBtn;

    //Constructor
    public MapPlannerFragment() {
        // Required empty public constructor
    }

    public static MapPlannerFragment newInstance(String param1, String param2) {
        MapPlannerFragment fragment = new MapPlannerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ptsRoute = new ArrayList<LatLng>();



        markerGenerator = new MarkerGenerator();

        //Chequeamos el permiso de la aplicación al GPS
        permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        } else {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    lastLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    if (lastLocationText != null){
                        lastLocationText.setText("Loc:" + lastLocation.longitude +" "+ lastLocation.latitude );
                    }
                    if (googleMap!=null){
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation,zoom));
                    }
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

        if (savedInstanceState != null){
            Bundle bundle = new Bundle(savedInstanceState);
            zoom = bundle.getFloat("zoom");
            target = (LatLng) bundle.get("target");
            waypoints = bundle.getParcelableArrayList("waypoints");
            if (waypoints!=null){
                for (Waypoint waypoint: waypoints){
                    ptsRoute.add(waypoint.getPosition());
                }                
            }            
        }else {
            if(lastLocation != null){
                target = lastLocation;
            }else{
                target = new LatLng(-33.512087318764834, -70.675048828125);
            }
            zoom = 5;
        }

        if (getArguments() != null) {
        }
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_screen_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        target = googleMap.getCameraPosition().target;
        zoom = googleMap.getCameraPosition().zoom;
        switch (item.getItemId()){
            case R.id.start_editor:
                Intent intent = new Intent(getActivity(), MapEditorActivity.class);
                intent.putExtra("target", target);
                intent.putExtra("zoom", zoom);
                startActivityForResult(intent, NEW_PLAN);
                break;
            case R.id.edit_plan:
                Intent editIntent = new Intent(getActivity(), MapEditorActivity.class);
                if (waypoints!=null){
                    editIntent.putParcelableArrayListExtra("waypoints", (ArrayList<? extends Parcelable>) waypoints);
                }
                editIntent.putExtra("target", target);
                editIntent.putExtra("zoom", zoom);
                startActivityForResult(editIntent, EDIT_PLAN);
                break;
            case R.id.load_plan:

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_planner, container, false);

        //Instanciamos los textos
        distanceText = (TextView) rootView.findViewById(R.id.distance_shower);
        lastLocationText = (TextView) rootView.findViewById(R.id.last_location);

        navTestBtn = (Button) rootView.findViewById(R.id.navigation_test);

        navTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waypoints!=null && !waypoints.isEmpty()){

                    Intent intent = new Intent(getContext(), NavigationActivity.class);
                    intent.putParcelableArrayListExtra("waypoints", (ArrayList<? extends Parcelable>) waypoints);
                    if(lastLocation!=null){
                        intent.putExtra("lastLoc", lastLocation);
                    }
                    startActivity(intent);
                }
            }
        });

        //Instanciamos la vista del mapa
        mapPlannerView = (MapView) rootView.findViewById(R.id.mapPlannerView);
        mapPlannerView.onCreate(savedInstanceState);
        mapPlannerView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }


        mapPlannerView.getMapAsync(mapReady = new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                System.out.println("Mapa Listo!");
                googleMap = mMap;
                UiSettings mapSettings = googleMap.getUiSettings();
                mapSettings.setMapToolbarEnabled(false);
                mapSettings.setRotateGesturesEnabled(false);
                mapSettings.setTiltGesturesEnabled(false);
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));

            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapPlannerView.onStart();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onMapPlannerInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapPlannerInteractionListener) {
            mListener = (OnMapPlannerInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapPlannerView.onSaveInstanceState(outState);
        if(googleMap != null){
            outState.putParcelable("target", googleMap.getCameraPosition().target);
            outState.putFloat("zoom", googleMap.getCameraPosition().zoom);
        }

        if (waypoints!=null){
            outState.putParcelableArrayList("waypoints", (ArrayList<? extends Parcelable>) waypoints);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapPlannerView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapPlannerView.onLowMemory();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapPlannerView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapPlannerView.onStop();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("reqCode "+ requestCode);
        Bundle bundle;
        if (resultCode== AppCompatActivity.RESULT_OK){
            bundle = data.getExtras();
            int markCount = 0;
            switch (requestCode){
                case NEW_PLAN:
                    fPlan = (FlightPlan) bundle.get("plan");
                    if (fPlan != null){
                        waypoints = fPlan.getRoute();
                    }
                    target = (LatLng) bundle.get("target");
                    zoom = bundle.getFloat("zoom");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));
                    optRuta = new PolylineOptions();
                    ptsRoute = new ArrayList<LatLng>();

                    for (FlightPolygon fPoly : fPlan.getfPolygons()){
                        fPoly.addPerimeter(googleMap);
                    }
                    
                    for (Waypoint waypoint: waypoints){
                        markCount++;                        
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(markCount));
                        googleMap.addMarker(new MarkerOptions().position(waypoint.getPosition()).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        ptsRoute.add(waypoint.getPosition());
                        
                    }
                    
                    ruta = googleMap.addPolyline(optRuta);
                    ruta.setPoints(ptsRoute);
                    break;
                
                case EDIT_PLAN:
                    googleMap.clear();
                    waypoints = bundle.getParcelableArrayList("waypoints");
                    target = (LatLng) bundle.get("target");
                    zoom = bundle.getFloat("zoom");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));
                    optRuta = new PolylineOptions();
                    ptsRoute = new ArrayList<LatLng>();

                    for (Waypoint waypoint: waypoints){
                        markCount++;
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(markCount));
                        googleMap.addMarker(new MarkerOptions().position(waypoint.getPosition()).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        ptsRoute.add(waypoint.getPosition());

                    }
                    
                    ruta = googleMap.addPolyline(optRuta);
                    ruta.setPoints(ptsRoute);
                    break;
                
                case LOAD_PLAN:
                    break;
            }
        }

        if (resultCode== AppCompatActivity.RESULT_CANCELED){
            if (data != null){
                bundle = data.getExtras();
                switch (requestCode){
                    case NEW_PLAN:
                        target = (LatLng) bundle.get("target");
                        zoom = bundle.getFloat("zoom");
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));
                        break;
                    case EDIT_PLAN:
                        break;
                    case LOAD_PLAN:
                        break;
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnMapPlannerInteractionListener {
        // TODO: Update argument type and name
        void onMapPlannerInteraction(Uri uri);
    }
}
