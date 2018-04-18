package cl.usach.abarra.flightplanner;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;



public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String DEFAULT_DISTANCE = "default_distance";
    public static final String DEFAULT_ANGLE = "default_angle";
    public static final String DEFAULT_UNITS = "distance_units";


    private OnSettingsInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }


    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnSettingsInteractionListener {
        // TODO: Update argument type and name
        void onSettingsInteraction(Uri uri);
    }
}
