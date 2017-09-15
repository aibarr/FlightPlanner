/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */
package cl.usach.abarra.flightplanner.engine.fwing;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * El Manejador de Sensores del Teléfono responde a requerimientos de datos de los sensores
 * internos del teléfono.
 */
//TODO ¿Usar clases internas para acceder a cada sensor?
    //TODO revisar el nivel de acceso de las clases (public o package-protected)
class PhoneSensorsHandler{

    SensorManager phoneSensorsMan;
    LocationManager phoneLocationMan;
    Sensor accelerometer;
    Sensor gyro;
    Sensor magnet;
    Sensor orientation;
    Context context;
    SensorEventListener sel;
    LocationListener locListener;
    long curTime,lastUpdateTime;
    double[] locationValues;//0: Latitud, 1: Longitud
    int numSat=0;
    double[] angles;//0: Roll, 1: Pitch, 2:Yaw
    float[] anglesTemp;

    public PhoneSensorsHandler(Context ct) {
        phoneSensorsMan = (SensorManager) ct.getSystemService(Context.SENSOR_SERVICE);
        phoneLocationMan = (LocationManager) ct.getSystemService(Context.LOCATION_SERVICE);
        context = ct;
        curTime = lastUpdateTime = 0;
        Log.i("SENSORS",phoneSensorsMan.getSensorList(Sensor.TYPE_ALL).toString());
        sel = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor changedSensor = event.sensor;
                Intent i;
                switch (changedSensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:
                        i = new Intent("SensorData_Gyro");
                        double[] rates = new double[3];
                        rates[0] = Math.toDegrees(event.values[0]);
                        rates[1] = Math.toDegrees(event.values[1]);
                        rates[2] = Math.toDegrees(event.values[2]);

                        i.putExtra("Values", rates);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        i = new Intent("SensorData_Acc");
                        i.putExtra("Values", event.values);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        i = new Intent("SensorData_Magnet");
                        i.putExtra("Values", event.values);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        //A diferencia de los otros sensores, en este caso se intenta enviar
                        //la orientación del teléfono en términos de ángulos aéreos,
                        // no su vector de rotación.

                        //Necesitamos limitar la tasa de refresco

                            float[] rotationMatrix = new float[16];
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                            angles = new double[3];
                            anglesTemp = new float[3];
                            anglesTemp = SensorManager.getOrientation(rotationMatrix, anglesTemp);

                        /*
                        * Ya que azimut(yaw) y pitch se calculan en torno a la parte negativa de sus
                        * ejes respectivos, es necesario reconvertirlos al signo correspondiente.
                        * En el caso de pitch, se hace para que pitch sea positivo al elevar la nariz
                        * del UAV, y en el caso de Yaw, para que 0 coincida con el norte magnético.
                        * */

                            //Transformación radianes -> grados
                            angles[0] = Math.toDegrees(anglesTemp[0]);
                            angles[1] = Math.toDegrees(anglesTemp[1]*-1);
                            angles[2] = Math.toDegrees(anglesTemp[2]);

                            //Transforma el rango de yaw de -180..180 a 0..359
                            if(angles[0]<0.0d){
                                angles[0]+=360.0d;
                            }
                            i = new Intent("SensorData_Orientation");
                            i.putExtra("Values",angles);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(i);




                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("Info:", "onLocationChanged: "+location.getProvider());
                Log.i("Info:", "onLocationChanged: "+location.getLatitude() + " " + location.getLongitude());
                Intent i = new Intent("SensorData_GPS");
                //locationValues[0] = location.getLatitude();
                //locationValues[1] = location.getLongitude();
                i.putExtra("Lat", location.getLatitude());
                i.putExtra("Lon", location.getLongitude());
                i.putExtra("Sat", location.getExtras().getInt("satellites"));
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i("Info:", "onStatusChanged: "+extras.getInt("satellites"));
                Intent i = new Intent("SensorData_GPS_Satellites");
                numSat = extras.getInt("satellites");
                i.putExtra("Value",numSat);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Intent i;
                i = new Intent("SensorHandlerMessage");
                i.putExtra("message","GPS activado");
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i;
                i = new Intent("SensorHandlerMessage");
                i.putExtra("message","GPS está desactivado. Por favor actívelo.");
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
        };
    }


    /**
     * Solicita al manejador activar el sensor requerido.
     * @param sensorType El tipo de sensor requerido. Sus valores pueden ser: SENSOR_ACC, SENSOR_GYRO,
     *                   SENSOR_MAGNET y SENSOR_GPS.
     */
    public void requestSensor(String sensorType){
        switch(sensorType){
            case "SENSOR_ACC":
                accelerometer = phoneSensorsMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                phoneSensorsMan.registerListener(sel,accelerometer, SensorManager.SENSOR_DELAY_GAME);
                break;
            case "SENSOR_GYRO":
                gyro = phoneSensorsMan.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                phoneSensorsMan.registerListener(sel,gyro, SensorManager.SENSOR_DELAY_GAME);
                break;
            case "SENSOR_MAGNET":
                magnet = phoneSensorsMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                phoneSensorsMan.registerListener(sel,magnet, SensorManager.SENSOR_DELAY_GAME);
                break;
            case "SENSOR_GPS":
                phoneLocationMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,1,locListener);
                break;
            case "SENSOR_ORIENTATION":
                orientation = phoneSensorsMan.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                phoneSensorsMan.registerListener(sel,orientation, SensorManager.SENSOR_DELAY_GAME);


        }
    }

    public void unRequestSensor(String sensorType){
        Intent i;
        switch(sensorType){
            case "SENSOR_ACC":
                phoneSensorsMan.unregisterListener(sel,accelerometer);
                i = new Intent("SensorHandlerMessage");
                i.putExtra("message","SENSOR_ACC_UNREQUESTED");
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                break;
            case "SENSOR_GYRO":
                phoneSensorsMan.unregisterListener(sel,gyro);
                //Envía una notificación de que el sensor se acaba de detener
                i = new Intent("SensorHandlerMessage");
                i.putExtra("message","SENSOR_GYRO_UNREQUESTED");
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                break;
            case "SENSOR_MAGNET":
                phoneSensorsMan.unregisterListener(sel,magnet);
                break;
            case "SENSOR_GPS":
                phoneLocationMan.removeUpdates(locListener);
                break;
            case "SENSOR_ORIENTATION":
                phoneSensorsMan.unregisterListener(sel,orientation);
                break;
        }
    }

}
