package cl.usach.abarra.flightplanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.TextViewCompat;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class MapEditorFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    int buttonFlag = 0;

    MapView mapEditorView;
    private GoogleMap map;
    private Polyline route;
    private PolylineOptions optRoute;

    private List<Polygon> polygonList;
    List<LatLng> ptsRoute;

    public static final CameraPosition MAIPU =
            new CameraPosition.Builder().target(new LatLng(-33.509838, -70.756432))
                    .zoom(15.5f)
                    .bearing(0)
                    .tilt(25)
                    .build();

    private PolygonOptions polygonOptions;

    LocationManager locationManager;
    LocationListener locationListener;
    Location loc;

    Button createLine;
    Button createPolygon;
    Button undo;

    TextView statusBar;

    public MapEditorFragment() {
        // Required empty public constructor
    }


    public static MapEditorFragment newInstance(String param1, String param2) {
        MapEditorFragment fragment = new MapEditorFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        polygonOptions = new PolygonOptions().fillColor(0x4d84ce85);
        polygonList = new ArrayList<Polygon>();
        if (getArguments() != null) {

        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.map_editor_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        mapEditorView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;

                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                //Verificamos ubicacion actual
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                //map.setMyLocationEnabled(true);

                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (loc==null){
                    locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            loc = location;
                            System.out.println(loc.toString());
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
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 500, locationListener);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 500, locationListener);
                } else {
                }
                //Acerquemos un poco la cámara
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-33.509838, -70.756432), 10));
            }
        });

        //Flag para Linea "1"
        createLine = (Button) rootView.findViewById(R.id.create_line);
        createLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (buttonFlag){
                    case 2:
                        rootView.findViewById(R.id.finish_Button).performClick();
                        break;
                    default:
                        break;
                }

                statusBar.setText("Creando Ruta");
                optRoute = new PolylineOptions();

                if (route==null){
                    route = map.addPolyline(optRoute);
                    ptsRoute = new ArrayList<LatLng>();
                }else {
                    ptsRoute = new ArrayList<LatLng>(route.getPoints());
                }

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        map.addMarker(new MarkerOptions().position(latLng));
                        ptsRoute.add(latLng);
                        route.setPoints(ptsRoute);
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
                final List<LatLng> vertices = new ArrayList<LatLng>();

                if  (ptsRoute==null && route==null){
                    ptsRoute = new ArrayList<LatLng>();
                    route = map.addPolyline(optRoute = new PolylineOptions());
                    System.out.println("haciendo lista");
                }

                map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    Polygon tempPolygon;
                    PolygonOptions tempPolygonOptions = new PolygonOptions();

                    @Override
                    public void onMapClick(LatLng latLng) {
                        map.addMarker(new MarkerOptions().position(latLng));
                        vertices.add(latLng);
                        if (vertices.size()>2){
                            if(tempPolygon==null){
                                tempPolygon = map.addPolygon(tempPolygonOptions.addAll(vertices));
                            }else {
                                tempPolygon.setPoints(vertices);
                            }

                        }
                    }
                });
                finishButton.setVisibility(View.VISIBLE);

                finishButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonFlag  =    0;

                        switch (vertices.size()){
                            case 0:
                                break;
                            case 1:
                                ptsRoute.addAll(vertices);
                                break;
                            default:
                                ptsRoute.addAll(vertices);
                                route.setPoints(ptsRoute);

                        }
                        map.setOnMapClickListener(null);
                        finishButton.setVisibility(View.GONE);
                    }
                });

            }
        });

        Button clear = (Button) rootView.findViewById(R.id.clear_button);

        undo = (Button) rootView.findViewById(R.id.undo_button);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                route = null;
                ptsRoute = null;
                map.setOnMapClickListener(null);
                map.clear();
            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
