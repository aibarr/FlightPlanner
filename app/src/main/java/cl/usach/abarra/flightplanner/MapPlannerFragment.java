package cl.usach.abarra.flightplanner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class MapPlannerFragment extends Fragment {

    MapView mapPlannerView;
    private GoogleMap googleMap;
    private OnMapReadyCallback mapReady;
    private Polyline ruta;
    private PolylineOptions optRuta;

    private Polygon zona;
    private PolygonOptions optZona;

    private OnFragmentInteractionListener mListener;

    //Elementos del Fragment
    Button drawPolygon;

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
        System.out.println("Se infló el menú");
        switch (item.getItemId()){
            case R.id.start_editor:
                Intent intent = new Intent(getActivity(), MapEditorActivity.class);
                startActivity(intent);
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

                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                /*//Ruta
                optRuta= new PolylineOptions();
                ruta = googleMap.addPolyline(optRuta);

                //Poligono
                optZona = new PolygonOptions();
                //zona = googleMap.addPolygon(optZona);

                final List<LatLng> ptsRuta = new ArrayList<LatLng>();

                googleMap.setOnMapClickListener(new OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        System.out.println("Tocado en "+ latLng.toString());
                        googleMap.addMarker(new MarkerOptions().position(latLng));
                        ptsRuta.add(latLng);
                        ruta.setPoints(ptsRuta);
                    }
                });*/
            }
        });

        /*drawPolygon = (Button) rootView.findViewById(R.id.drawPolygon);

        drawPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zona = null;
                googleMap.clear();

                googleMap.setOnMapClickListener(new OnMapClickListener() {
                    final List<LatLng> ptsZona = new ArrayList<LatLng>();

                    @Override
                    public void onMapClick(LatLng latLng) {
                        ptsZona.add(latLng);
                        googleMap.addMarker(new MarkerOptions().position(latLng));
                        if (ptsZona.size()>2){
                            if(zona != null){
                                zona.setPoints(ptsZona);

                            }else {
                                optZona = new PolygonOptions();
                                optZona.addAll(ptsZona);
                                zona = googleMap.addPolygon(optZona);
                            }
                        }
                    }
                });
            }
        });*/

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
