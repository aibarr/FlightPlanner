package cl.usach.abarra.flightplanner;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);
    }
}
