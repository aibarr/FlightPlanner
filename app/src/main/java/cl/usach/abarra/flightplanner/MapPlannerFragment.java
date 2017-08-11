package cl.usach.abarra.flightplanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

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
    private List<Polygon> polygonList;

    //Datos para enviar/recibir
    private ArrayList<Double> latitudes;
    private ArrayList<Double> longitudes;

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
            latitudes = (ArrayList<Double>) bundle.getSerializable("latitudes");
            longitudes = (ArrayList<Double>) bundle.getSerializable("longitudes");
            for(int i= 0; i < latitudes.size(); i++){
                ptsRoute.add(new LatLng(latitudes.get(i), longitudes.get(i)));
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
                intent.putExtra("camPos", target);
                intent.putExtra("camZoom", zoom);
                startActivityForResult(intent, NEW_PLAN);
                break;
            case R.id.edit_plan:
                Intent editIntent = new Intent(getActivity(), MapEditorActivity.class);
                if (ptsRoute!= null && !(ptsRoute.isEmpty())){
                    editIntent.putExtra("latitudes", latitudes);
                    editIntent.putExtra("longitudes", longitudes);
                }
                editIntent.putExtra("camPos", target);
                editIntent.putExtra("camZoom", zoom);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_planner, container, false);

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

        if (ptsRoute != null){
            latitudes = new ArrayList<Double>();
            longitudes = new ArrayList<Double>();
            for (LatLng point : ptsRoute){
                latitudes.add(point.latitude);
                longitudes.add(point.longitude);
            }
            outState.putSerializable("latitudes", latitudes);
            outState.putSerializable("longitudes", longitudes);
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
                    latitudes = (ArrayList<Double>) data.getSerializableExtra("latitudes");
                    longitudes = (ArrayList<Double>) data.getSerializableExtra("longitudes");
                    System.out.println(latitudes.toString());
                    target = (LatLng) bundle.get("camPos");
                    zoom = bundle.getFloat("camZoom");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));
                    optRuta = new PolylineOptions();
                    ptsRoute = new ArrayList<LatLng>();

                    for (int i=0; i < latitudes.size(); i++ ){
                        markCount++;
                        LatLng punto = new LatLng(latitudes.get(i), longitudes.get(i) );
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(markCount));
                        googleMap.addMarker(new MarkerOptions().position(punto).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        ptsRoute.add(punto);
                    }
                    ruta = googleMap.addPolyline(optRuta);
                    ruta.setPoints(ptsRoute);
                    break;
                case EDIT_PLAN:
                    googleMap.clear();
                    latitudes = (ArrayList<Double>) data.getSerializableExtra("latitudes");
                    longitudes = (ArrayList<Double>) data.getSerializableExtra("longitudes");
                    System.out.println(latitudes.toString());
                    target = (LatLng) bundle.get("camPos");
                    zoom = bundle.getFloat("camZoom");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target,zoom));
                    optRuta = new PolylineOptions();
                    ptsRoute = new ArrayList<LatLng>();

                    for (int i=0; i < latitudes.size(); i++ ){
                        markCount++;
                        LatLng punto = new LatLng(latitudes.get(i), longitudes.get(i) );
                        Bitmap bitmap = markerGenerator.makeBitmap(getContext(), String.valueOf(markCount));
                        googleMap.addMarker(new MarkerOptions().position(punto).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                        ptsRoute.add(punto);
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
                        target = (LatLng) bundle.get("camPos");
                        zoom = bundle.getFloat("camZoom");
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
