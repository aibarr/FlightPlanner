package cl.usach.abarra.flightplanner.util;

import android.app.IntentService;
import android.content.Intent;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;


public class UtilityService extends IntentService {

    public boolean ioioFlag = false;
    private IOIOAndroidApplicationHelper ioioHelper;

    public UtilityService() {
        super("UtilityService");
    }



    @Override
    protected void onHandleIntent(Intent intent) {

        //Creo un apoyo para checkear el estado del IoIo exclusivamente
        ioioHelper = new IOIOAndroidApplicationHelper(getApplication(), new IOIOLooperProvider() {
            @Override
            public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
                return new BaseIOIOLooper(){
                    @Override
                    protected void setup() throws ConnectionLostException, InterruptedException {
                        super.setup();
                    }

                    @Override
                    public void loop() throws ConnectionLostException, InterruptedException {
                        super.loop();
                        try {
                            ioio_.waitForConnect();

                            //TODO: avisar que está conectado el ioio

                            ioioFlag = true;
                        } catch (IncompatibilityException e) {
                            e.printStackTrace();
                        }catch (ConnectionLostException e){
                            ioioFlag = false;
                        }
                    }
                };
            }
        });

        int tries = 0;

        boolean docheck = true;

        //chequear si hay conexión con el IOIO, por ahora revisaré 5 veces nada más

        while(docheck){
            if (tries>5) docheck=false;
            try {
                //Reviso Si el IOIO está conectado
                if(ioioFlag) {
                    System.out.println("Conectado");
                }

                else {
                    System.out.println("Desconectado");
                }
                Thread.sleep(5000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tries++;
        }

    }

    /*public void startNavigation(){
        Intent intent = new Intent(getApplicationContext(), NavigationService.class);
        startService(intent);
    }*/



}
