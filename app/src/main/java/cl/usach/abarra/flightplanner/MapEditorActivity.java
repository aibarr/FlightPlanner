package cl.usach.abarra.flightplanner;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

public class MapEditorActivity extends Activity implements MapEditorFragment.OnFragmentInteractionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_editor);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
