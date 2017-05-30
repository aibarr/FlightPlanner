package cl.usach.abarra.flightplanner;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;



public class settingsFragment extends PreferenceFragmentCompat {


    private OnSettingsInteractionListener mListener;

    public settingsFragment() {
        // Required empty public constructor
    }


    public static settingsFragment newInstance() {
        settingsFragment fragment = new settingsFragment();
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
