/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */
package cl.usach.abarra.flightplanner.engine.fwing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.codec.binary.BinaryCodec;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * Servicio de control de interfaz de vuelo.
 * Es la clase encargada de enviar instrucciones a la interfaz de vuelo y recibir datos desde
 * sus sensores.
 */
public class InterfaceControlService extends IOIOService {

    /*
    Constantes
     */
    private static final int AILERON_CENTER_PWM_PERIOD = 1500; //Período en el que los alerones se encuentran a 0°
    private static final int AILERON_MAX_PWM_PERIOD = 2860; //Alerón izquierdo deflectado al máximo hacia abajo, el derecho hacia arriba
    private static final int AILERON_MIN_PWM_PERIOD = 1130; //Alerón izquierdo deflectado al máximo hacia arriba, el derecho hacia abajo
    private static final int ENGINE_SETUP_MINIMUM_PWM_PERIOD = 750; //Este valor sólo se utiliza para incializar el acelerador electrónico
    private static final int ENGINE_MINIMUM_THROTTLE_PWM_PERIOD = 800; //Período donde el motor se encuentra en reposo
    private static final int ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD = 1700; //Valor máximo de inicialización del acelerador electrónico, y período donde el motor desarrolla su máxima potencia
    private static final int ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD = 900;//Valor máximo del período para el motor con limitador activo, aprox. un 10% de su capacidad.
    //Pines del IOIO
    private static final int IOIO_THROTTLE_PIN = 1;
    private static final int IOIO_LEFT_AILERON_PIN = 2;
    private static final int IOIO_RIGHT_AILERON_PIN = 3;
    private static final int IOIO_LAUNCHING_LED_PIN = 5; //IOIO no armado
    private static final int IOIO_ARMED_LED_PIN = 6; //IOIO armado
    private static final int IOIO_READY_LED_PIN = 7; //IOIO en modo de vuelo, inminente o en ejecución
    private static final int IOIO_APT_SENSOR_I2C_PORT = 2; //Puerto I2C del IOIO a usar, por defecto 2 (pines 25 y 26)
    private static final int IOIO_AIRSPEED_SENSOR_PIN = 34;
    private static final int IOIO_APT_I2C_ADDRESS = (byte)0x60;//Dirección del sensor A.P.T.
    //Sensor de presión dinámica
    private static final int AIRSPEED_SENSOR_VOLTS_TO_PASCAL = 819;
    private static final float AIRSPEED_SENSOR_VOLTAGE_OFFSET = 2.5f;


    /*
    * Variables de salidas y entradas del IOIO
    * */
    private static DigitalOutput statLed;
    private static DigitalOutput launchLed;//Rojo, indica que el avión entró a conteo regresivo de lanzamiento
    private static DigitalOutput armedLed;//Amarillo, indica el estado de armado del motor
    private static DigitalOutput readyLed;//Verde, indica si el avión está listo para volar
    private static PwmOutput leftAileronControl;
    private static PwmOutput rightAileronControl;
    private static PwmOutput engineControl;
    private static AnalogInput airspeedSensor;
    private static TwiMaster aptSensor;
    private static PulseInput manualControlPWM;

    /*
    Arreglos de bytes para la comunicación I2C con el sensor de altitud, presión estática, y temperatura
     */
    private static byte[] request;
    private static byte[] response;

    /*
    Variables varias, valores de PWM, estado del led status de IOIO, etcétera.
     */
    static Boolean ledOn = false;//Esta variable NO sigue la lógica invertida del led STATUS del IOIO. La lógica se invierte en el método que actualiza el LED.
    static Boolean ledOn_old = false;
    static Boolean isReady = true; //¿Está listo para volar el UAV? (Todas las pruebas realizadas exitosamente)
    static Boolean isArmed = false; //¿Está armado el motor?
    static Boolean isLaunching = false; //¿El UAV está en cuenta regresiva para despegar?
    static Boolean icsEnabled = false; //Está funcionando el servicio con el IOIO?
    private static Boolean isManualMode = false;//¿Modo de mando manual?

    static Boolean engineSafe = true;//Seguro de motor, si está activo, el PWM siempre será 0
    static Boolean aileronSafe = true;//Si está activo, los alerones no se pueden (simula armado/desarmado alerones)
    static Boolean engineMinimalThrottle = false;//Si está activo, sólo acelerará a un 10% de su capacidad total.
    static Boolean isECSCalibrated = false;//¿Está calibrado el Acelerador Electrónico?

    private static long leftAileronPWMPeriod=AILERON_CENTER_PWM_PERIOD;
    private static long rightAileronPWMPeriod=AILERON_CENTER_PWM_PERIOD;
    private static long elevatorPWMPeriod=0;
    private static long enginePWMPeriod;
    private static long pitchDelta=0, oldPitchDelta=0;
    private static Float altDelta =0.0f;
    private static Float oldAlt = 0.0f, newAlt = 0.0f; //Altitudes ABSOLUTAS del sensor
    private static Float refAlt = 0.0f;
    private static Float relativeAlt = 0.0f;//Altitud RELATIVA, que usa el UAV en base a refAlt
    private static Float ambientTemp = 0.0f;//Temperatura ambiente medido por el sensor A.P.T.
    private static Float gpsAltitude = 0.0f;//Altitud GPS en caso de emergencia
    private static Double airspeed = 0.0d;//Airspeed real del UAV según sensor de presión dinámica
    private static Float gpsAirspeed = 0.0f;//Airspeed aparente según GPS para caso de emergencia
    private static Double throttleLevel = 0.0;//Porcentaje de válvula (acelerador)

    //Si el IOIO llegara a desconectarse, los valores de altitud y velocidad pueden obtenerse desde el GPS mientras se
    private static boolean ioioEmergency = false;

    private static InterfaceControlService instanciaICS;

    /**
     * Entrega la instancia única del Servicio de Control de Interfaz ICS.
     *
     * @return la instancia del InterfaceControlService
     */
    public static InterfaceControlService getICSInstance() {
        if (instanciaICS == null)
            instanciaICS = new InterfaceControlService();
        return instanciaICS;
    }

    public InterfaceControlService() {
        //TODO analizar conveniencia de patrón singleton en ICS
    } /*Necesitamos que el constructor sea private para poder
    utilizar el patrón de diseño Singleton*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                Log.i("ICS", "Comienza la configuración del IOIO");
                statLed = ioio_.openDigitalOutput(0, true); //true significa que el led de status comenzará apagado, esto debido a su lógica invertida
                //donde true = OFF y false = ON

                statLed.write(false);
                Thread.sleep(1000);
                statLed.write(true);
                Thread.sleep(1000);

                leftAileronControl = ioio_.openPwmOutput(IOIO_LEFT_AILERON_PIN, 50);
                rightAileronControl = ioio_.openPwmOutput(IOIO_RIGHT_AILERON_PIN, 50);
                engineControl = ioio_.openPwmOutput(IOIO_THROTTLE_PIN, 50);
                launchLed = ioio_.openDigitalOutput(IOIO_LAUNCHING_LED_PIN);
                armedLed = ioio_.openDigitalOutput(IOIO_ARMED_LED_PIN);
                readyLed = ioio_.openDigitalOutput(IOIO_READY_LED_PIN);
                aptSensor = ioio_.openTwiMaster(IOIO_APT_SENSOR_I2C_PORT, TwiMaster.Rate.RATE_400KHz, false);
                airspeedSensor = ioio_.openAnalogInput(IOIO_AIRSPEED_SENSOR_PIN);
                manualControlPWM = ioio_.openPulseInput(37, PulseInput.PulseMode.POSITIVE);

                Log.i("ICS","Declarados los pines físicos.");

                statLed.write(false);
                Thread.sleep(1000);
                statLed.write(true);
                Thread.sleep(1000);

                boolean aptCalibrated = aptSensorSetup("MODE_ALT");
                if(aptCalibrated){
                    Log.i("ICS","Sensor APT calibrado correctamente.");
                }else{
                    Log.w("ICS","Sensor APT no ha sido calibrado.");
                }

                //Valores PWM de actuadores son 0 --> UAV desarmado.
                leftAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
                rightAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
                enginePWMPeriod = 0;

                launchLed.write(true);
                Thread.sleep(200);
                launchLed.write(false);
                armedLed.write(true);
                Thread.sleep(200);
                armedLed.write(false);
                readyLed.write(true);
                Thread.sleep(200);
                launchLed.write(true);
                armedLed.write(true);
                Thread.sleep(200);
                launchLed.write(false);
                readyLed.write(false);
                armedLed.write(false);

                statLed.write(false);
                Thread.sleep(1000);
                statLed.write(true);
                Thread.sleep(1000);

                //ServiceControlActivity.isInterfaceLinked = true;
                icsEnabled = true;
                Thread.currentThread().setPriority(10);
                //TODO play a nice sound when setup is completed
                Log.i("ICS","Interfaz configurada correctamente.");
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                if(icsEnabled){
                    getExternalSensorsInfo();
                 //   isManualControlActive();
                    Thread.sleep(20);//20ms, frecuencia de refresco de 50Hz aprox.
                    if(!isManualMode){
                        executeActuatorActions();
                    }


                }


            }

            @Override
            public void disconnected() {
                super.disconnected();
                //ServiceControlActivity.isInterfaceLinked = false;
                icsEnabled = false;
                isManualMode = false;
                stopSelf();
            }

            private void isManualControlActive() throws ConnectionLostException, InterruptedException {
                float pulsePeriod = manualControlPWM.getDuration();
            //    Log.i("ICS", "manualControl Pulse Period:" + String.valueOf(pulsePeriod));
                if(pulsePeriod < 0.001){
                    if(!isManualMode){
                        isManualMode = true;
                        engineControl.close();
                        leftAileronControl.close();
                        rightAileronControl.close();
                    }

                } else {
                    if(isManualMode){
                        isManualMode = false;
                        engineControl = ioio_.openPwmOutput(IOIO_THROTTLE_PIN,50);
                        leftAileronControl = ioio_.openPwmOutput(IOIO_LEFT_AILERON_PIN,50);
                        rightAileronControl = ioio_.openPwmOutput(IOIO_RIGHT_AILERON_PIN,50);
                    }
                }
            }
        };
    }

    private void updateStatusLed() throws ConnectionLostException {
        if (ledOn_old != ledOn) {
            statLed.write(!ledOn);
            ledOn_old = ledOn;
        }//Si ledOn_old == ledOn, no se hace nada
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //LocalBroadcastManager.getInstance(this).registerReceiver(ledStatBCReceiver, new IntentFilter("ToggleButtonStatus"));
        Toast svcToast;
        svcToast = Toast.makeText(getApplicationContext(), "[ICS]: Servicio IOIO creado", Toast.LENGTH_SHORT);
        svcToast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        oldPitchDelta = 0;
        elevatorPWMPeriod = 0;
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(ledStatBCReceiver);
        icsEnabled = false;
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "[ICS]: Servicio IOIO iniciado", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    //Receptor del interruptor del LED de status
    private BroadcastReceiver ledStatBCReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ledOn = intent.getBooleanExtra("ToggleButtonStatusValue", false);
        }
    };

    /**
     * Comanda al UAV a realizar un giro hacia la izquierda.
     *
     * @param degrees Número de grados del giro deseado.
     *                El valor debe ser un número entre 0 y 15 por
     *                limitaciones de diseño.
     */
    public static void turnLeft(double degrees) { //Implica package-local
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;

        leftAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD - 370 * degrees / 30 - elevatorPWMPeriod);
        rightAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD - 370 * degrees / 30 + elevatorPWMPeriod);
    }

    /**
     * Comanda al UAV a realizar un giro hacia la derecha.
     *
     * @param degrees Número de grados del giro deseado.
     *                El valor debe ser un número entre 0 y 15 por
     *                limitaciones de diseño.
     */
    public static void turnRight(double degrees) { //Implica package-local
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;
        leftAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD + 370 * degrees / 30 - elevatorPWMPeriod);
        rightAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD + 370 * degrees / 30 + elevatorPWMPeriod);
    }

    /**
     * Comanda al UAV a elevarse o descender sus elevadores en la cantidad de grados especificada.
     * @param degrees la cantidad de grados que se deben deflectar los alerones. Valor debe ser entre
     *                -15 y 15.
     */
    public static void controlPitch(double degrees) {
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;
        elevatorPWMPeriod = Math.round(370 * degrees / 30); //Si grados > 0, asciende con respecto al alerón der, el izq hay que invertir el signo
        calculateElevonPWM();
    }


    /**
     * Controla las RPM del motor propulsor del UAV.
     *
     * @param rpmFactor Número entre 0 y 1 indicando el porcentaje de RPM del motor, donde
     *                  0 indica que el motor está en reposo y 1 que el motor estará
     *                  a las máximas RPM posibles.
     */
    public static void controlEngine(double rpmFactor) {
    //    Log.i("ICS","RPMFactor: " + String.valueOf(rpmFactor));
        if(rpmFactor>1){
            rpmFactor = 1.0;
        } else if(rpmFactor < 0.0){
            rpmFactor = 0.0;
        }
        if (engineSafe) { //Con el seguro activado el motor no se arma.
            enginePWMPeriod = 0;
            throttleLevel = 0.0;
        }else if(engineMinimalThrottle){ //El limitador está activado

            //Si la configuración del Acelerador Electrónico dejó bajo el período mínimo de acelerador al motor, hay que
            //restaurarlo
            if(enginePWMPeriod < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;

            long calculatedPWM = Math.round((double)ENGINE_MINIMUM_THROTTLE_PWM_PERIOD + rpmFactor * 900.0);
        //    Log.i("ICS","CalculatedPWM: " + String.valueOf(calculatedPWM));
            //Limita el rango de PWM que se pueden utilizar
            if (calculatedPWM < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.0;
        //        Log.i("ICS","calculado < minimo");
            } else if (calculatedPWM > ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.1;
        //        Log.i("ICS","calculado > limitador");
            } else {
                enginePWMPeriod = calculatedPWM;
            }
        }else{
            //Si la configuración del Acelerador Electrónico dejó bajo el período mínimo de acelerador al motor, hay que
            //restaurarlo
            if(enginePWMPeriod < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;

            long calculatedPWM = Math.round((double)ENGINE_MINIMUM_THROTTLE_PWM_PERIOD + rpmFactor * 900.0);
        //    Log.i("ICS","CalculatedPWM: " + String.valueOf(calculatedPWM));

            //Limita el rango de PWM que se pueden utilizar
            if (calculatedPWM < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.0;
        //        Log.i("ICS","calculado < minimo");
            } else if (calculatedPWM > ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 1.0;
        //        Log.i("ICS","calculado > maximo");
            } else {
                enginePWMPeriod = calculatedPWM;
                throttleLevel = rpmFactor;
            }
        }

    }

    /**
     * Realiza la rutina de calibrado del acelerador electrónico.
     */
    public static void calibrateECS() throws InterruptedException {
        if(!isECSCalibrated){
            enginePWMPeriod = ENGINE_SETUP_MINIMUM_PWM_PERIOD;
            Thread.sleep(2000);
            enginePWMPeriod = ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD;
            Thread.sleep(2000);
            enginePWMPeriod = ENGINE_SETUP_MINIMUM_PWM_PERIOD;
            Log.i("ICS","Acelerador electrónico calibrado");
            isECSCalibrated = true;
        } else {
            Log.i("ICS","El acelerador electrónico ya ha sido calibrado previamente.");
        }

    }

    /**
     * Entrega la lectura de la presión estática según la lectura del sensor externo instalado en
     * el UAV.
     *
     * @return el valor de la presión, en bares.
     */
    static float obtenerPresion() {
        return 0;
    }

    static double obtenerPorcentajeValvula(){
        return throttleLevel;
    }

    /**
     * Entrega la lectura de la altitud referencial del UAV por sobre una altitud de referencia previamente
     * fijada. Esta lectura puede ser de signo positivo o negativo según el UAV se encuentre por sobre
     * o debajo de esta altitud de referencia, respectivamente.
     *
     * @return la altitud relativa del UAV, en metros.
     */
    static float obtenerAltitud() throws InterruptedException {
        if(!ioioEmergency){
            try {
                aptSensorGetAltitudeAndTemp();
            } catch (ConnectionLostException e) {
                e.printStackTrace();
                Log.e("ICS", "Error al obtener la altitud: se ha perdido la conexión con la interfaz.");
                invokeIOIOEmergencyState();
                return refAlt;
            }
            return refAlt;
        } else {
            Log.w("ICS", "Se ha detectado que no hay comunicación con IOIO. Usando altitud GPS en su lugar...");
            return gpsAltitude;
        }

    }

    /**
     * Entrega la lectura de la temperatura ambiente detectada por el sensor externo asociado al UAV.
     *
     * @return el valor de la temperatura, expresado en grados Celsius.
     */
   static float obtenerTemperatura() throws InterruptedException {

        try {
            aptSensorGetAltitudeAndTemp();
        } catch (ConnectionLostException e) {
            e.printStackTrace();
            Log.e("ICS", "Error al obtener la temperatura: se ha perdido la conexión con la interfaz.");
            invokeIOIOEmergencyState();
        }
        return ambientTemp;
    }

    /**
     * Entrega la lectura de la velocidad relativa del UAV en el aire, según la entrada del sensor
     * de presión dinámica conectado a éste.
     *
     * @return la velocidad relativa del UAV, en metros por segundo.
     */
    static double obtenerAirspeed() {
      //  Log.i("ICS","Airspeed: " + airspeed);
        return airspeed;
    }


    /**
     * Inicializa el sensor para funcionar en un modo especificado. Por defecto toma muestras sin
     * guardarlas en un búfer de ningún tipo.
     * @param mode El modo de funcionamiento. Las alternativas son MODE_ALT y MODE_PRES para medir altitud
     *             y presión estática, respectivamente. La temperatura se puede medir en ambos modos. El modo
     *             por defecto es MODE_ALT
     */
    private boolean aptSensorSetup(String mode) throws InterruptedException {
        boolean result = false;
        switch (mode){
            case "MODE_ALT":
            /*
            En primer lugar, es necesario llamar al registro de control 1
	        para configurarlo en modo altímetro y en captura de datos permanente.

	        CTRL_REG1 se encuentra en 0x26 según el datasheet
            */

	        /*
	        CTRL_REG (0x26) BIT MAP

	        ALT | RAW | OS_2 | OS_1 | OS_0 | RST (Reset) | OST (One Shot) | SBYB (Standby Bit)

	        */
                //Colocamos el sensor en modo STANDBY
                request = new byte[]{(byte)0x26,(byte)0b10111010};
                response = new byte[1];
                try {
                    result = aptSensor.writeRead(IOIO_APT_I2C_ADDRESS, false, request, request.length, response, 1);
                } catch (ConnectionLostException e) {
                    e.printStackTrace();
                    Log.e("ICS", "Error al configurar el sensor APT: Se ha perdido la conexión con la interfaz.");
                    invokeIOIOEmergencyState();
                    return false;
                }
                if(result){ //Sensor configurado correctamente
                    Log.i("IOIO","APT Sensor in Altitude Mode is configuring... " + BinaryCodec.toAsciiString(response));
                    //Quitaremos el sensor del modo STANDY para que pueda obtener lecturas
                    request = new byte[]{(byte)0x26,(byte)0b10111011};
                    response = new byte[1];
                    try {
                        result = aptSensor.writeRead(IOIO_APT_I2C_ADDRESS, false, request, request.length, response, 1);
                    } catch (ConnectionLostException e) {
                        e.printStackTrace();
                        Log.e("ICS","Error al configurar el sensor APT: Se ha perdido la conexión con la interfaz.");
                        invokeIOIOEmergencyState();
                        return false;
                    }
                    //Los otros registros de control se dejan con sus valores por defecto
                    if(!result){//Si transactResult es falso, desplegar mensaje de error
                        Log.e("IOIO", "Error during APT Sensor Setup while trying to disable STANDBY mode.");
                        return false;
                    }else{
                        Log.i("APTSensor","APTSensor set up and taking samples: "+ BinaryCodec.toAsciiString(response));
                    }
                    //Introducimos una pausa para leer el primer valor
                    Thread.sleep(1000);
                    try {
                        aptSensorGetAltitudeAndTemp();//Toma automáticamente la primera lectura en la inicialización como cero
                    } catch (ConnectionLostException e) {
                        e.printStackTrace();
                        Log.e("ICS","Error al intentar leer datos del sensor APT: Se ha perdido la conexión con la interfaz.");
                        invokeIOIOEmergencyState();
                        return false;
                    }
                    try {
                        aptSensorGetAltitudeAndTemp();//Descartamos la primera lectura y utilizamos la segunda, como precaución
                    } catch (ConnectionLostException e) {
                        e.printStackTrace();
                        Log.e("ICS","Error al intentar leer datos del sensor APT: Se ha perdido la conexión con la interfaz.");
                        invokeIOIOEmergencyState();
                        return false;
                    }
                    Log.i("IOIO","APT Sensor configured sucessfully");
                    return true;
                }else{
                    Log.i("IOIO","Error configuring sensor in Altitude Mode: " + BinaryCodec.toAsciiString(response));
                    return false;
                }


            case "MODE_PRES":
                return false;

            default:
                aptSensorSetup("MODE_ALT");
                return true;

        }
    }

    /**
     * Lee el sensor APT desde el IOIO obteniendo Altitud y Temperatura. <b>Se asume que previamente
     * el sensor ha sido inicializado a modo Altitud</b>
     * <p>
     * Nota: el método es bloqueante.
     *
     */
    private static void aptSensorGetAltitudeAndTemp() throws ConnectionLostException, InterruptedException {
        Short wholeTemp;
        float fractTemp;
        Short wholeAltitude;
        float fractAltitude;
        Boolean firstReading=true;

        //Lectura del sensor
        request = new byte[]{(byte)0x01};
        response = new byte[5];//3 bytes para Altitud, 2 bytes para Temperatura
        aptSensor.writeRead(IOIO_APT_I2C_ADDRESS, false, request, request.length, response, response.length);
        byte[] altBits = new byte[3];
        byte[] tmp = new byte[1];
        tmp[0]=(byte)response[3];
        //System.out.println("RAW TMP BITS:" +  BinaryCodec.toAsciiString(tmp));
        tmp[0]=(byte)response[4];
        //System.out.println("RAW FRAC TMP BITS:" +  BinaryCodec.toAsciiString(tmp));
        char chartmp1[];
        char chartmp2[];
        chartmp1 = BinaryCodec.toAsciiChars(tmp);
        //System.out.println(chartmp1.length);
        chartmp2 = Arrays.copyOfRange(chartmp1, 0, 4);
        //System.out.println(chartmp2.length);
        chartmp1[0] = '0';
        chartmp1[1] = '0';
        chartmp1[2] = '0';
        chartmp1[3] = '0';
        chartmp1[4] = chartmp2[0];
        chartmp1[5] = chartmp2[1];
        chartmp1[6] = chartmp2[2];
        chartmp1[7] = chartmp2[3];
        tmp = BinaryCodec.fromAscii(chartmp1);
        //System.out.println("RAW ALT BITS DESPL.:" +  BinaryCodec.toAsciiString(tmp));
        ByteBuffer bb;
        //Es necesario convertir los valores
//	System.out.println(BinaryCodec.toAsciiString(wholeAltBytes));
        bb = ByteBuffer.allocate(2);
        bb.clear();
        bb.put((byte) 0);
        bb.put(tmp[0]);
        bb.rewind();
//	System.out.println(bb.asShortBuffer().get());
        wholeTemp = bb.getShort();
        fractTemp = wholeTemp/16.0f;

        bb.clear();
        bb.put((byte) 0);
        bb.put(response[3]);//bb.put((BYTE)response[3]);
        bb.rewind();
        wholeTemp = bb.getShort();

       // Log.i("APTSensor","TEMP: " + (wholeTemp + fractTemp) + " °C");
        ambientTemp = wholeTemp + fractTemp;

        //Altitude

        tmp[0] = response[2];
        chartmp1 = BinaryCodec.toAsciiChars(tmp);
        //System.out.println(chartmp1.length);
        chartmp2 = Arrays.copyOfRange(chartmp1, 0, 4);
        //System.out.println(chartmp2.length);
        chartmp1[0] = '0';
        chartmp1[1] = '0';
        chartmp1[2] = '0';
        chartmp1[3] = '0';
        chartmp1[4] = chartmp2[0];
        chartmp1[5] = chartmp2[1];
        chartmp1[6] = chartmp2[2];
        chartmp1[7] = chartmp2[3];
        tmp = BinaryCodec.fromAscii(chartmp1);
        bb.clear();
        bb.put((byte)0);
        bb.put(tmp[0]);
        bb.rewind();

        wholeAltitude = bb.getShort();
        fractAltitude = wholeAltitude/16.0f;

        bb.clear();
        bb.put(response[0]);
        bb.put(response[1]);
        bb.rewind();
        wholeAltitude = bb.getShort();
        //Log.i("APTSensor","ALT: " + (wholeAltitude + fractAltitude) + " m." );
        if (firstReading){//Para primera lectura
            oldAlt = wholeAltitude + fractAltitude;
            altDelta = 0.0f;

          //  Log.i("APTSensor","DELTA ALT:" + altDelta + " m");
        } else {
            newAlt = wholeAltitude + fractAltitude;
            altDelta = newAlt - oldAlt;
           // Log.i("APTSensor","DELTA ALT:" + altDelta + " m");
            oldAlt = newAlt;
        }
        // Log.i("APTSensor","Relative ALT:" + (oldAlt - refAlt) + " m.");
        relativeAlt = oldAlt - refAlt;
    }

    /**
     * Informa al Servicio de Control de Vuelo que la comunicación con la placa controladora se ha
     * perdido, y por ende, es necesario dirigirse inmediatamente a la base, utilizando como apoyo
     * los datos del sistema GPS.
     */
    private static void invokeIOIOEmergencyState(){
        //TODO Evaluar si es conveniente desplazar esta función a la clase Emergency como método estático
    }

    /**
     * Configura el valor de la altitud de referencia a partir de la lectura por parte del sensor
     * APT en el punto donde se invoca a esta función.
     * @return <ul>
     *     <li><b>true</b> cuando se ha fijado exitosamente la referencia</li>
     *     <li><b>false</b> en caso contrario</li>
     * </ul>
     */
    public static boolean setReferenceAltitude() throws InterruptedException {
        if(!ioioEmergency){

            try {
                aptSensorGetAltitudeAndTemp();
            } catch (ConnectionLostException e) {
                e.printStackTrace();
                Log.e("ICS", "Error al fijar la altitud de referencia: se ha perdido la conexión con la interfaz.");
                invokeIOIOEmergencyState();
                return false;
            }
            refAlt = oldAlt;
            return true;
        } else {
            Log.e("ICS","No se puede fijar la altitud de referencia. Se reporta que no hay comunicación con la interfaz.");
            return false;
        }
    }

    /**
     * Obtiene el valor de airspeed a partir del sensor de presión dinámica.
     * @throws ConnectionLostException
     * @throws InterruptedException
     */
    private void airspeedSensorGetAirspeed() throws ConnectionLostException, InterruptedException {
        double Ao = 340.29;       //speed of sound at standard sea level in m/s
        double Po = 101325.0;       //static air pressure at standard sea level in Pascals
        double To = 288.15;       //standard sea level temperature in K
        double AD7918 = 0.0048828;      //Multiplier for AD Converter
        double Vs = 5;        //Supply voltage in V
        double power = (double)2/7;
        double measuredPascals;
        double normalizedSensorVoltage;
        double rawSensorVoltage = (double)airspeedSensor.getVoltage();
        normalizedSensorVoltage = rawSensorVoltage - 2.55;
       // Log.i("ICS","NormalizedSensorVolts: " + String.valueOf(normalizedSensorVoltage));
        measuredPascals = normalizedSensorVoltage*1000.0;
       // Log.i("ICS","Measured pascals: " + String.valueOf(measuredPascals));

        if(measuredPascals < 0.0){
        //    Log.i("ICS","Negative pascals!");
            measuredPascals = 0.0;
        }
        airspeed = Ao* Math.sqrt(5.0*(Math.pow((measuredPascals/Po)+1.0,power)-1.0));
      //  Log.i("ICS","Airspeed:" + String.valueOf(airspeed));
    }

    /**
     * Refresca los valores de los sensores externos.
     * <p>
     * Alternativamente, se pueden invocar {@link InterfaceControlService#aptSensorGetAltitudeAndTemp()} y {@link InterfaceControlService#airspeedSensorGetAirspeed()}
     * por separado.
     * @throws ConnectionLostException
     * @throws InterruptedException
     */
    private void getExternalSensorsInfo() throws ConnectionLostException, InterruptedException {
        if(!ioioEmergency){
            airspeedSensorGetAirspeed();
            aptSensorGetAltitudeAndTemp();
        } else {
            Log.e("ICS","Imposible obtener valores de sensores externos: Se reporta que no hay comunicación con la interfaz.");
        }

    }

    /**
     * Combina los valores PWM de elevador y alerón para generar la salida final para cada alerón
     */
    private static void calculateElevonPWM(){
        pitchDelta = elevatorPWMPeriod;
        long tempDelta = pitchDelta - oldPitchDelta;
        rightAileronPWMPeriod = rightAileronPWMPeriod + tempDelta;
        leftAileronPWMPeriod = leftAileronPWMPeriod - tempDelta;

        //Limitando el período para no sobresforzar los alerones ni su mecanismo
        if(leftAileronPWMPeriod > AILERON_MAX_PWM_PERIOD){
            leftAileronPWMPeriod = AILERON_MAX_PWM_PERIOD;
        } else if(leftAileronPWMPeriod < AILERON_MIN_PWM_PERIOD){
            leftAileronPWMPeriod = AILERON_MIN_PWM_PERIOD;
        }

        if(rightAileronPWMPeriod > AILERON_MAX_PWM_PERIOD){
            rightAileronPWMPeriod = AILERON_MAX_PWM_PERIOD;
        } else if (rightAileronPWMPeriod < AILERON_MIN_PWM_PERIOD){
            rightAileronPWMPeriod = AILERON_MIN_PWM_PERIOD;
        }

        oldPitchDelta = pitchDelta;

    }

    /**
     * Actualiza los actuadores con los nuevos valores computados
     */
    private void executeActuatorActions() throws ConnectionLostException {

        /*
        Estas comprobaciones de engineSafe y aileronSafe son redundantes, en caso de falla de programación
        de los valores del período PWM para cada uno de los actuadores por alguno de los métodos que
        gobierna directamente al motor o a los servomotores.
         */
        if(engineSafe){
           // Log.i("ICS","Engine safe is on, pulse width is 0");
            engineControl.setPulseWidth(0);
        }else{
            engineControl.setPulseWidth(enginePWMPeriod);
        }

        if(!aileronSafe){
            //Log.i("ICS", "pwm aleron izq " + String.valueOf(leftAileronPWMPeriod));
            leftAileronControl.setPulseWidth(leftAileronPWMPeriod);
            rightAileronControl.setPulseWidth(rightAileronPWMPeriod);

        }

        readyLed.write(isReady);
        launchLed.write(isLaunching);
        armedLed.write(isArmed);
    }

    /**
     * Activa o desactiva el seguro de motor. Si el seguro está activado, el período del motor
     * siempre será 0.
     * @param active <b>true</b> si se desea activar el seguro, <b>false</b> si se desea desactivar
     */
    public static void setEngineSafe(boolean active){
        if(active){
            enginePWMPeriod = 0;
            engineSafe = true;
            isArmed = false;
        }else{
            engineSafe = false;
        }

    }

    /**
     * Activa o desactiva el limitador del acelerador de motor. Si está activo, el motor sólo acelerará
     * hasta un 10% de su capacidad máxima.
     * @param active <b>true</b> si se desea activar el limitador, <b>false</b> si se desea desactivar
     */
    public static void setEngineMinimalThrottle(boolean active){
        engineMinimalThrottle = active;
        Log.i("ICS","MinimalThrottle:"+ String.valueOf(active));
    }


    public static boolean doArming(boolean engineArming, boolean aileronsArming) throws InterruptedException {
        if(engineArming){
            if(engineSafe){
                isArmed = false;
                enginePWMPeriod = 0;
                if(engineArming){
                    Log.e("ICS","Imposible armar motor: Seguro de motor está activado");
                }
            }else{
                isArmed = true;
                calibrateECS();
                enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;
                //ArmadoActivity.uavState = "Armado";
                Log.w("ICS","ADVERTENCIA: El motor está armado. ¡RETIRE SUS MANOS DEL PROPULSOR!");
            }
        }else{
                isArmed = false;
                enginePWMPeriod = 0;
                Log.i("ICS", "Motor desarmado.");
                //ArmadoActivity.uavState = "Desarmado";
        }




        if(aileronsArming){
            leftAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
            rightAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
            elevatorPWMPeriod = 0;
            oldPitchDelta = 0;
            aileronSafe = false;
            Log.i("ICS","Alerones armados.");
        }else{
            leftAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
            rightAileronPWMPeriod = AILERON_CENTER_PWM_PERIOD;
            aileronSafe = true;
            elevatorPWMPeriod = 0;
            oldPitchDelta = 0;
            Log.i("ICS", "Alerones desarmados.");
        }
        return isArmed;


    }

    public static void moveLeftAileron (double degrees){
        if(!aileronSafe){
            if (degrees < -30) degrees = -30;
            else if (degrees > 30) degrees = 30;
            leftAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD + 370 * degrees / 30);
        } else {
            Log.e("ICS", "No se puede mover el alerón izquierdo: el seguro de alerón está activado");
        }

    }

    public static void moveRightAileron (double degrees){
        if(!aileronSafe){
            if (degrees < -30) degrees = -30;
            else if (degrees > 30) degrees = 30;
            rightAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD - 370 * degrees / 30);
        } else {
            Log.e("ICS","No se puede mover el alerón derecho: el seguro de alerón está activado");
        }
    }







}