package cl.usach.abarra.flightplanner.engine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NavigationService extends Service {
    public NavigationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
