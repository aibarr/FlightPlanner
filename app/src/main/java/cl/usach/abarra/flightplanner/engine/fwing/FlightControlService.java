/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */
package cl.usach.abarra.flightplanner.engine.fwing;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;


//TODO Implementar método onRestart()

/**
 * Servicio controlador de vuelo. Procesa los datos de sensores, navegación y misión y genera los
 * comandos respectivos para ser transmitidos al ICS o a través de los Lazos de Control del UAV.
 * <p>
 *     Dado que este servicio principalmente ejecuta un proceso cíclico, la tarea principal es ejecutada
 *     en un Thread propio del servicio.
 * <p>
 * El servicio debe ser detenido de manera manual por el operador a través de la aplicación.
 */
public class FlightControlService extends Service {
    private final String LogTag = "CTH_SVC"; //Es el TAG que utilizaremos para identificar los
    //mensajes en el Log provenientes desde
    //este servicio
    private static final int FCS_LOOP_FREQ = 50; //Cantidad de veces por segundo que el FCS debería ejecutar acciones
    private static final double UAV_CRUISE_VELOCITY = 15.0;//Velocidad crucero del UAV, en m/s. Sirve de referencia para el modo AIRSPEED_CONTROL
    private static final int UAV_PITCH_TRIM_ANGLE = 0;//¿Cuántos grados hacia arriba debe estar el UAV para ofrecer un vuelo recto y nivelado?

    public static boolean writeSensorDataToLogFile = false;
    File sensorDataFile;
    FileWriter fileWriter;

    long curTime=0;
    long lastTime=0;

    //Variables de control del FCS
    private static String flightMode;
    private static String currentStatus;
    private static String pidControllersSubset;
    private PhoneSensorsHandler psh=null;
    private Thread flightControlThread=null;
    private static Boolean isControlRunning=false;//Lazo de control andando?
    public static Boolean isFCSRunning=false;//¿Está corriendo el servicio?
    public static UAVControlLoops uavLoops;


    //Información de sensores del FCS
    private static double curLatitude=0;
    private static double curLongitude=0;
    private static double curGPSSatellites=0;
    private static double curGyroRollRate=0;
    private static double curGyroPitchRate=0;

    private static double curRoll=0;
    private static double curPitch=0;
    private static double curYaw=0;

    private static double curAirspeed=0;
    private static double curAltitude=0;
    private static double curThrottle=0;

    private static double expectedAirspeed=0;
    private static double expectedAltitude=0;
    private static double expectedHeading=0;

    Context ct;

    GenericPIDController genericPID;

    private double correctedPitch=0.0;
    private double correctedRoll=0.0;


    public FlightControlService() {
    }


    Toast svcToast;

    /*
    * Siempre un servicio debe incorporar su "código útil" dentro de onStartCommand.
    * Esto significa que, cualquier código que
    * represente trabajo para el cual el servicio fue construido, debería estar
    * dentro de este comando.
    *
    * En nuestro caso particular, a partir de aquí crearemos los hilos de control.
    * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ct = this;

        writeSensorDataToLogFile = false;

        if(writeSensorDataToLogFile){
            GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
            String dateOfToday = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
            dateOfToday = dateOfToday + String.valueOf(calendar.get(Calendar.MONTH));
            dateOfToday = dateOfToday + String.valueOf(calendar.get(Calendar.YEAR));
            dateOfToday = dateOfToday + "_" + String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
            dateOfToday = dateOfToday + String.valueOf(calendar.get(Calendar.MINUTE));
            dateOfToday = dateOfToday + String.valueOf(calendar.get(Calendar.SECOND));

            sensorDataFile = new File(this.getExternalFilesDir(null),"Icarus Sensor Data " + dateOfToday);
            try {
                sensorDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        //    Log.i("FCS","Directorio: "+ this.getExternalFilesDir(null).toString());
            sensorDataFile.setWritable(true);
        }
        initializeInternalSensors();
        initializeControlThread();
        svcToast = Toast.makeText(getApplicationContext(),"[FCS]: Servicio FCS iniciado", Toast.LENGTH_SHORT);
        svcToast.show();

        pidControllersSubset = "PITCH_CONTROL";
        //TEST
        uavLoops = new UAVControlLoops();
        try {
            InterfaceControlService.doArming(true,true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //</TEST>

        isFCSRunning = true;



        try {
            writeLogFileHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            startFlightControl();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i("FCS", "Servicio FlightControlService iniciado.");
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {

        svcToast = Toast.makeText(getApplicationContext(),"[FCS]: Servicio FCS creado", Toast.LENGTH_SHORT);
        svcToast.show();
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        psh.unRequestSensor("SENSOR_GYRO");
    //    psh.unRequestSensor("SENSOR_GPS");
        psh.unRequestSensor("SENSOR_ORIENTATION");

        //Si estamos controlando y el servicio es destruido, detener el hilo de control.
        if(isControlRunning){
            try {
                stopFlightControl();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isControlRunning = false;
        }

        Log.i("FCS", "Servicio FlightControlService finalizado.");
        isFCSRunning = false;
        svcToast = Toast.makeText(getApplicationContext(),"[FCS]: Servicio destruido por sistema.", Toast.LENGTH_SHORT);
        svcToast.show();
        stopSelf();
        super.onDestroy();
    }





    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Deprecated
    private void updateServiceStatus(Boolean isOn){
        Intent i = new Intent("CTH_SVC_STATUS_UPD");
        if(isOn){
            i.putExtra("message", "Estado del servicio: Iniciado");
            svcToast = Toast.makeText(getApplicationContext(),"Servicio iniciado", Toast.LENGTH_SHORT);
            svcToast.show();
        }else{
            i.putExtra("message", "Estado del servicio: Detenido");
            svcToast = Toast.makeText(getApplicationContext(),"Servicio detenido", Toast.LENGTH_SHORT);
            svcToast.show();
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    /**
     * Si se detecta que la batería del teléfono está bajo el porcentaje de seguridad, el Servicio de
     * Control de Vuelo anulará la ruta de vuelo actual y ordenará regresar a la base de inmediato.
     */
    private void invokeBatteryEmergencyState(){
        //TODO Evaluar si es conveniente desplazar esta función a la clase Emergency como método estático
    }

    /**
     * Medida más extrema que {@link FlightControlService#invokeBatteryEmergencyState()}. Se ejecuta cuando el Servicio de Control de
     * Vuelo detecta que el nivel de la batería del teléfono puede no garantizar el retorno seguro a la
     * base.
     * <p>
     *     Cuando la condición crítica de batería se cumple, el FCS realiza las siguientes acciones:
     *     <ol>
     *      <li>Envía un mensaje de texto a un número predefinido en base informando la última ubicación GPS conocida</li>
     *      <li>Apaga el motor</li>
     *      <li>Posiciona los alerones para vuelo recto a fin de hacerlo planear y disminuir la velocidad de impacto</li>
     *     </ol>
     * </p>
     */
    private void invokeBatteryCriticalState(){
        //TODO Evaluar si es conveniente desplazar esta función a la clase Emergency como método estático
    }

    /**
     * Configura, según el subconjunto especificado de PIDs, los PID que se utilizarán para el
     * control del vuelo.
     */
    private void workingPIDsSetup(){
        switch(pidControllersSubset){

            /*
            FULL CONTROL NAVIGATION:
                - Controla la estabilidad completa del UAV
                - Depende del heading deseado
             */
            case "FULL_CONTROL_NAVIGATION":
                uavLoops.setExpectedAirspeed(expectedAirspeed);
                uavLoops.setExpectedAltitude(expectedAltitude);
                uavLoops.setExpectedHeading(expectedHeading);
                uavLoops.executeStabilityControlLoops(curRoll,curGyroRollRate,curPitch,curGyroPitchRate,curYaw,curAirspeed,curAltitude);
                break;
            /*
            FULL CONTROL:
                - Controla la estabilidad completa del UAV
                - No depende del heading deseado
             */
            case "FULL_CONTROL":
                uavLoops.setExpectedAirspeed(expectedAirspeed);
                uavLoops.setExpectedAltitude(expectedAltitude);
                uavLoops.innerLoopRollToActuator(0,curRoll,curGyroRollRate);
                uavLoops.longitudinalControlLoop(expectedAirspeed,curAirspeed,expectedAltitude,curAltitude,curPitch,curGyroPitchRate,curRoll);
                break;

            /*
            LONG CONTROL:
                - Controla la estabilidad longitudinal del UAV: Airspeed y Pitch
                - No depende del heading deseado
             */
            case "LONG_CONTROL":
                uavLoops.setExpectedAirspeed(expectedAirspeed);
                uavLoops.setExpectedAltitude(expectedAltitude);
                uavLoops.longitudinalControlLoop(expectedAirspeed,curAirspeed,expectedAltitude,curAltitude,curPitch,curGyroPitchRate,curRoll);
                break;

            /*
                - Controla la estabilidad lateral del UAV: Roll
                - No depende del heading deseado
             */
            case "LAT_CONTROL":
                uavLoops.setExpectedAirspeed(expectedAirspeed);
                uavLoops.setExpectedAltitude(expectedAltitude);
                uavLoops.setExpectedHeading(expectedHeading);
                uavLoops.lateralControlLoop(expectedHeading,curYaw,curRoll,curGyroRollRate);
                break;

            /*
            ROLL AND PITCH CONTROL:
                - Sólo regula roll y pitch, haciéndolos tender a cero
                - No regula el airspeed
                - Sirve para ejecutar la prueba de balance
                - No depende del heading deseado
             */
            case "ROLL_AND_PITCH_CONTROL":
               // Log.i("FCS", "Roll: " + curRoll +" - GyroRollRate: " + curGyroRollRate);
                uavLoops.innerLoopRollToActuator(0, curRoll, curGyroRollRate);
                uavLoops.innerLoopPitchToActuator(0, curPitch, curGyroPitchRate, curRoll);

                //genericPID.setInputValue(curRoll);
                //genericPID.doControl();
               /* double temp = genericPID.getOutputValue();
                if(temp < -15){
                    temp = -15;
                } else if (temp > 15){
                    temp = 15;
                }
                Log.i("FCS","PID Output (Roll angle correction): " + temp);*/
                //Ordena al Servicio de Control de Interfaz que adopte el giro calculado
              //  if(temp < 0){ //Se debe girar a la izquierda
                   // InterfaceControlService.turnLeft(Math.abs(temp));
              //  } else if (temp > 0){ //Se debe girar a la derecha
                    //InterfaceControlService.turnRight(temp);
              //  }
                break;

            /*
            ROLL CONTROL:
                - Sólo regula el roll, haciéndolo tender a 0
             */
            case "ROLL_CONTROL":
                uavLoops.innerLoopRollToActuator(0,curRoll,curGyroRollRate);
                break;

            /*
            PITCH CONTROL:
                - Sólo regula el pitch, haciéndolo tender a 0
             */
            case "PITCH_CONTROL":
                uavLoops.innerLoopPitchToActuator(0,curPitch,curGyroPitchRate,curRoll);
                break;

            /*
            AIRSPEED CONTROL:
                - Sólo regula la velocidad del UAV a través del motor
                - La velocidad objetivo es la constante de velocidad crucero
             */
            case "AIRSPEED_CONTROL":
                uavLoops.innerLoopAirspeedToActuator(UAV_CRUISE_VELOCITY,curAirspeed);
                break;

            case "PITCH_FROM_AIRSPEED":
                // correctedPitch=uavLoops.outerLoopPitchFromAirspeed(UAV_CRUISE_VELOCITY,curAirspeed);
                 uavLoops.innerLoopPitchToActuator(0,curPitch,curGyroPitchRate,curRoll);
                break;

            case "ROLL_FROM_HEADING":
                correctedRoll = uavLoops.outerLoopRollFromHeading(0,curYaw);
                uavLoops.innerLoopRollToActuator(correctedRoll,curRoll,curGyroRollRate);
                break;
        }
    }



    private void initializeInternalSensors(){
        if(psh==null){
            psh = new PhoneSensorsHandler(this);
        }
        psh.requestSensor("SENSOR_GYRO");
    //    psh.requestSensor("SENSOR_GPS");
        psh.requestSensor("SENSOR_ORIENTATION");
    }


    /*
     * Thread que ejecuta el ciclo de gobernar al UAV
     */

    private void initializeControlThread() {
        if (flightControlThread == null) {
            flightControlThread = new Thread(new Runnable() {
                private BroadcastReceiver sensorDataRecv = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        //        curTime = System.currentTimeMillis();
                        if (intent.getAction() == "SensorData_GPS") {
                            //valoresDesdeSensor = intent.getDoubleArrayExtra("Values");
                            //Log.i("Info","Data GPS:"+valoresDesdeSensor.toString());
                            curLatitude = intent.getDoubleExtra("Lat", 0);
                            curLongitude = intent.getDoubleExtra("Lon", 0);
                            curGPSSatellites = intent.getIntExtra("Sat", 0);
                        } else if (intent.getAction() == "SensorData_GPS_Satellites") {
                            curGPSSatellites = intent.getIntExtra("Value", 0);
                        } else if(intent.getAction()=="SensorData_Gyro"){
                            double[] valoresDesdeSensor = intent.getDoubleArrayExtra("Values");
                            curGyroRollRate = valoresDesdeSensor[1];
                            curGyroPitchRate = valoresDesdeSensor[0];
                        } else if(intent.getAction()=="SensorData_Orientation"){
                            double[] angulos = intent.getDoubleArrayExtra("Values");
                            curRoll = angulos[2];
                            curPitch = angulos[1];
                            curYaw = angulos[0];
                            //   Log.i("ICS","curRoll:" + String.valueOf(curRoll));
                            //        lastTime = curTime;
                        }

                        try {
                            writeSensorDataToLogFile();
                            //  Log.i("FCS","Data is being written...");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            flightControlProcedure();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }

                    }
                };

                private void queryExternalSensors() throws InterruptedException {
                    curAirspeed = InterfaceControlService.obtenerAirspeed();
                    curAltitude = InterfaceControlService.obtenerAltitud();
                    curThrottle = InterfaceControlService.obtenerPorcentajeValvula();
                }

                private void flightControlProcedure() throws InterruptedException {
                    if(Thread.interrupted()){
                        Toast.makeText(ct,"El hilo de control de vuelo ha sido finalizado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    queryExternalSensors();
                    //workingPIDsSetup();
                    doPIDControl();
                }

                private void doPIDControl(){
                    uavLoops.innerLoopRollToActuator(0, curRoll, curGyroRollRate);
                    uavLoops.innerLoopPitchToActuator(0, curPitch, curGyroPitchRate, curRoll);
                }









                @Override
                public void run() {

                        LocalBroadcastManager.getInstance(ct).registerReceiver(sensorDataRecv, new IntentFilter("SensorData_Gyro"));
                        LocalBroadcastManager.getInstance(ct).registerReceiver(sensorDataRecv, new IntentFilter("SensorData_Orientation"));





                }

                public void cancelThread(){
                    Thread.currentThread().interrupt();
                    try {
                        Thread.currentThread().join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });

        }
    }

    /**
     * Inicia el Thread controlador de vuelo. Esto implica el inicio de la misión preconfigurada.
     */
    public void startFlightControl() throws InterruptedException {
        //TODO definir los elementos a utilizar según el modo de vuelo
        flightControlThread.start();
        isControlRunning = true;

    }

    /**
     * Detiene el Thread controlador de vuelo. A partir de este punto el control del UAV está
     * fuera de responsabilidad del FCS.
     * @throws InterruptedException
     */
    public void stopFlightControl() throws InterruptedException {

        flightControlThread.interrupt();
        flightControlThread.join();
        isControlRunning = false;
    }

    /**
     * Corresponde a la función que representa el ciclo de control de vuelo.
     */


    private void writeLogFileHeader() throws IOException {
        if(writeSensorDataToLogFile){

            fileWriter = new FileWriter(sensorDataFile,true);//Modo Append
            BufferedWriter bufWriter = new BufferedWriter(fileWriter);
            bufWriter.write("Icarus Sensor Data Log");
            bufWriter.newLine();
            bufWriter.write("======================");
            bufWriter.newLine();
            bufWriter.newLine();
            bufWriter.newLine();
            bufWriter.write("ROLL   PITCH   YAW     AIRSP   THROTTLE");
            bufWriter.newLine();
            bufWriter.write("====   =====   ===     =====   ========");
            bufWriter.newLine();
            bufWriter.close();
            fileWriter.close();


        }
    }

    private void writeSensorDataToLogFile() throws IOException {
        if(writeSensorDataToLogFile){
            fileWriter = new FileWriter(sensorDataFile,true);//Modo Append
            BufferedWriter bufWriter = new BufferedWriter(fileWriter);
            bufWriter.newLine();
            bufWriter.write(String.valueOf(curRoll));
            bufWriter.write("\t");
            bufWriter.write(String.valueOf(curPitch));
            bufWriter.write("\t");
            bufWriter.write(String.valueOf(curYaw));
            bufWriter.write("\t");
            bufWriter.write(String.valueOf(curAirspeed));
            bufWriter.write("\t");
            bufWriter.write(String.valueOf(curThrottle));
            bufWriter.close();
            fileWriter.close();
        }
    }



}
