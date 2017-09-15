/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */
package cl.usach.abarra.flightplanner.engine.fwing;

import android.util.Log;

/**
 * Provee los lazos de control necesarios para el manejo del UAV
 */
public class UAVControlLoops {

    //Controladores PID del eje transversal
    public GenericPIDController rollPI;
    public GenericPIDController rollRateP;
    public GenericPIDController headingPID;

    //Controladores PID del eje longitudinal
    public GenericPIDController pitchPI;
    public GenericPIDController pitchRateP;
    public GenericPIDController rollP;
    public GenericPIDController airSpeedPID;
    public GenericPIDController altitudePID;
    public GenericPIDController outerAirspeedPID;

    //Valores objetivo desde navegación
    private double expectedAirspeed;
    private double expectedHeading;
    private double expectedAltitude;




    public void innerLoopRollToActuator(double desiredRoll, double actualRoll, double rollRate){
        if(rollPI == null) Log.e("UAVControllerLoops","Mi rollPI es nulo!");

        //0.25,0.0001,0
        rollPI.init(0.25, 0.00001, 0.0, true, desiredRoll, -30.0, 30.0); //Los valores de alerón están limitados a un ángulo de +- 15°

        //Valor máximo del Roll Rate: 15 grados/seg.

        //0.025,0.001,0
        rollRateP.init(0.015, 0.00001, 0.0, true, 0.0, -30.0, 30.0);// Fijar valor deseado en cero para que el UAV retorne siempre a su punto centro.

        rollPI.setInputValue(actualRoll);
        rollRateP.setInputValue(rollRate);
        double rollFromControl;
        rollPI.doControl();
        rollRateP.doControl();
        rollFromControl = rollPI.getOutputValue()+ rollRateP.getOutputValue();

        //Se debe limitar a rollFromControl a un rango de +-15 grados, que es lo que soportan los alerones del UAV
        //Si bien cada controlador está limitado a ese rango, la suma de ambos hace que el valor final se pueda exceder.
        if(rollFromControl < -30.0){
            rollFromControl = -30.0;
        } else if (rollFromControl > 30.0){
            rollFromControl = 30.0;
        }

        //Ordena al Servicio de Control de Interfaz que adopte el giro calculado
        if(rollFromControl < 0.0){ //Se debe girar a la izquierda
            InterfaceControlService.turnLeft(Math.abs(rollFromControl));
        } else if (rollFromControl > 0.0){ //Se debe girar a la derecha
            InterfaceControlService.turnRight(rollFromControl);
        }
        /*Nota: opcionalmente en vez de llamar por separado a
        métodos del estilo turnLeft y turnRight, se puede unificar a un solo método turn()
        guardando las convenciones de signo entre alerones.
         */
    //    Log.i("RollLoop", "RollFromControl: " + rollFromControl);
    }


    public double outerLoopRollFromHeading(double desiredHeading, double actualHeading){

        //TEST CODE

        //1.- Caso trivial, ambos puntos están entre 0 y 180, los PID se hacen cargo
        if((desiredHeading >= 0.0)&&(desiredHeading <= 180.0)&&(actualHeading >= 0.0)&&(actualHeading <= 180.0)){
            headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
            headingPID.setInputValue(actualHeading);

        //2.- Caso trivial, ambos puntos están entre 180 y 359, los PID se hacen cargo
        } else if((desiredHeading >180.0)&&(desiredHeading<360.0)&&(actualHeading >180.0)&&(actualHeading<360.0)){
            headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
            headingPID.setInputValue(actualHeading);


        //3.- Caso no trivial, un punto está en 0-180 y el otro está en 180-359
        //3.1.- Si ambos están entre 90 y 270, los PID se hacen cargo
        }else if((desiredHeading>=90.0)&&(desiredHeading<=270.0)&&(actualHeading>=90.0)&&(desiredHeading<=270.0)) {
            headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
            headingPID.setInputValue(actualHeading);

            //3.2.- Si ambos están entre 270 y 90 pasando por 0, debe recalcularse tomando el punto que esté
            //en el cuadrante 270-0 y restarle 180, luego calcular la distancia entre ese número y el que está entre
            //0-90 y a ese último número restarle esa distancia para volver a dejar el punto de 270-0 en su lugar
            //original, pero esta vez con un heading "negativo". Sobre esos datos, se aplica el PID.
        }else if(((desiredHeading>270.0)||(desiredHeading<90.0))&&
                ((actualHeading>270.0)||(actualHeading<90.0))){
            //3.2.1.- El heading deseado está entre 270 y 360, el heading actual entre 0 y 90.
            if((desiredHeading>270.0)&&(desiredHeading<360.0)&&(actualHeading>=0.0)&&(actualHeading<=90.0)){
                double angularDistance;
                desiredHeading = desiredHeading - 180.0;
                angularDistance = desiredHeading - actualHeading;
                desiredHeading = actualHeading - angularDistance;
                headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                headingPID.setInputValue(actualHeading);
            //3.2.2.- El heading deseado está entre 0 y 90, el heading actual entre 270 y 360
            } else if((desiredHeading>=0)&&(desiredHeading<=90.0)&&(actualHeading>270.0)&&(actualHeading<360.0)){
                double angularDistance;
                actualHeading = actualHeading - 180.0;
                angularDistance = actualHeading - desiredHeading;
                actualHeading = desiredHeading - angularDistance;
                headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                headingPID.setInputValue(actualHeading);
            }



        //3.3.- Si los puntos están en cuadrantes diagonales, debe recalcularse calculando la distancia entre el mayor
        //    y el menor. Si esta distancia es menor o igual a 180, proceder con un caso normal. En caso contrario,
        // se deberá aplicar la técnica descrita en 3.2.

        } else if(
                ((actualHeading>=0.0)&&(actualHeading<=90.0)&&(desiredHeading>=180.0)&&(desiredHeading<270.0))||
                ((actualHeading>=90.0)&&(actualHeading<=180.0)&&(desiredHeading>=270.0)&&(desiredHeading<360.0))||
                ((actualHeading>=180.0)&&(actualHeading<=270.0)&&(desiredHeading>=0.0)&&(desiredHeading<=90.0))||
                ((actualHeading>270.0)&&(actualHeading<360.0)&&(desiredHeading>=90.0)&&(desiredHeading<=180.0))
                ){
            double angularDistance;
                //3.3.1.- El heading deseado es mayor o igual al actual, por defecto se debería girar a la derecha
                if(desiredHeading >= actualHeading){
                    angularDistance = desiredHeading - actualHeading;
                    //3.3.1.1.- La distancia en un giro a la derecha es menor a 180°, debe girar hacia ese lado
                    if(angularDistance <= 180.0){
                        headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                        headingPID.setInputValue(actualHeading);
                    } else { //Sale más corto dar la vuelta hacia la izquierda
                        desiredHeading = desiredHeading - 180.0;
                        angularDistance = desiredHeading - actualHeading;
                        desiredHeading = actualHeading - angularDistance;
                        headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                        headingPID.setInputValue(actualHeading);
                    }
                } else if(desiredHeading < actualHeading){
                    angularDistance = actualHeading - desiredHeading;
                    if(angularDistance <= 180.0){
                        headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                        headingPID.setInputValue(actualHeading);
                    }else{
                        actualHeading = actualHeading - 180.0;
                        angularDistance = actualHeading - desiredHeading;
                        actualHeading = desiredHeading - angularDistance;
                        headingPID.init(0.2, 0.000001, 0.01, true, desiredHeading, -30.0, 30.0);
                        headingPID.setInputValue(actualHeading);
                    }
                } else {
                    Log.wtf("UAVControlLoops","El heading deseado no es mayor, ni menor, ni igual al heading actual!?");
                }
        }

        //END TEST CODE



        headingPID.doControl();
        Log.i("UAVControlLoops", "Roll from Heading:" + String.valueOf(headingPID.getOutputValue()));
        return headingPID.getOutputValue();
    }

    public void lateralControlLoop(double desiredHeading, double actualHeading, double actualRollFromSensor, double rollRateFromSensor){
        double desiredRoll = outerLoopRollFromHeading(desiredHeading,actualHeading);
        innerLoopRollToActuator(desiredRoll, actualRollFromSensor, rollRateFromSensor);
    }

    public void innerLoopPitchToActuator(double desiredPitch, double actualPitch, double pitchRate, double actualRoll){

        //0.6,0.0001,0
        pitchPI.init(0.8,0.00001,0.0,true,desiredPitch,-30.0,30.0);//Valores máximos de ángulo del elevón: +-30 grados.
       //0.0125,0,0
        pitchRateP.init(0.025,0.0,0.0,true,0.0,-30.0,30.0);//Pitch rate entre 0 y 30 grados/seg
       //0.25,0.00001,0
        rollP.init(0.25, 0.00001, 0.0, true, 0.0, -30.0, 30.0);
        pitchPI.setDesiredValue(desiredPitch);
        pitchPI.setInputValue(actualPitch);
        pitchRateP.setInputValue(pitchRate);
        if(actualRoll>0.0){
            actualRoll = actualRoll*-1;
        }
        rollP.setInputValue(actualRoll);
        pitchPI.doControl();
        pitchRateP.doControl();
        rollP.doControl();
        double pitchFromControl;
        pitchFromControl = pitchPI.getOutputValue() + pitchRateP.getOutputValue() + rollP.getOutputValue();

        //Saturación
        if(pitchFromControl > 30.0){
            pitchFromControl = 30.0;
        } else if (pitchFromControl < -30.0){
            pitchFromControl = -30.0;
        }

        //Ordenamos al Servicio de Control de Interfaz que realice el ajuste correspondiente en el elevador
        InterfaceControlService.controlPitch(pitchFromControl);
     //   Log.i("PitchLoop","PitchFromControl: "+pitchFromControl);

    }

    public void innerLoopAirspeedToActuator(double desiredSpeed,double actualSpeed){
        airSpeedPID.init(1.0,0.0,0.3,true,desiredSpeed,0.0,1.0);
        airSpeedPID.setInputValue(actualSpeed);
        airSpeedPID.doControl();
        double airSpeedFromControl = airSpeedPID.getOutputValue();
        /*
        Ordenamos al Servicio de Control de Interfaz acelerar o decelerar el motor
         */
        InterfaceControlService.controlEngine(airSpeedFromControl);
        Log.i("AirspeedLoop","AirspeedFromControl: "+airSpeedFromControl);
    }

    public double outerLoopPitchFromAltitude(double desiredAltitude, double actualAltitude){
        altitudePID.init(0.04, 0.004, 0.0, true, desiredAltitude, -30.0, 30.0);
        altitudePID.setInputValue(actualAltitude);
        altitudePID.doControl();
        double pitchFromAltitude = altitudePID.getOutputValue();
        return pitchFromAltitude;
    }

    public double outerLoopPitchFromAirspeed(double desiredSpeed, double actualSpeed){
        outerAirspeedPID.init(2.0,0.0,0.0,true,desiredSpeed,-30.0,30.0);
        outerAirspeedPID.setInputValue(actualSpeed);
        outerAirspeedPID.doControl();
        double pitchFromAirspeed = outerAirspeedPID.getOutputValue();
  //      Log.i("UAVLoops","Pitch from Airspeed: " + String.valueOf(pitchFromAirspeed*-1.0));
            return pitchFromAirspeed*-1.0;
    }

    public void longitudinalControlLoop(double desiredSpeed, double actualSpeed, double desiredAltitude, double actualAltitude,double actualPitchFromSensor, double pitchRateFromSensor, double actualRollFromSensor){
        double desiredPitch;
        desiredPitch = outerLoopPitchFromAltitude(desiredAltitude,actualAltitude);
        desiredPitch += outerLoopPitchFromAirspeed(desiredSpeed,actualSpeed);
        innerLoopPitchToActuator(desiredPitch, actualPitchFromSensor, pitchRateFromSensor, actualRollFromSensor);
        innerLoopAirspeedToActuator(desiredSpeed,actualSpeed);
    }

    public UAVControlLoops(){
        rollPI = new GenericPIDController();
        rollP = new GenericPIDController();
        rollRateP = new GenericPIDController();
        headingPID = new GenericPIDController();
        pitchPI = new GenericPIDController();
        pitchRateP = new GenericPIDController();
        airSpeedPID = new GenericPIDController();
        altitudePID = new GenericPIDController();
        outerAirspeedPID = new GenericPIDController();

        //Valores genéricos
        this.expectedAirspeed = 1.0;
        this.expectedAltitude = 100.0;
        this.expectedHeading = 0.0;
    }

    /**
     * Ejecuta las medidas correctivas de estabilidad en base a la información entregada por los sensores y los parámetros
     * objetivo.
     * @param actualRoll valor del ángulo de Roll según el teléfono
     * @param actualRollRate valor de la velocidad angular para el ángulo de Roll según el teléfono
     * @param actualPitch valor del ángulo de Pitch según el teléfono
     * @param actualPitchRate valor de la velocidad angular para el ángulo de Pitch según el teléfono
     * @param actualHeading valor del Heading, o rumbo hacia donde se dirige el UAV (no es Yaw)
     * @param actualAirspeed valor del Airspeed según indicado por el sensor externo
     * @param actualAltitude valor de la Altitud según indicador por sensores
     */
    public void executeStabilityControlLoops(double actualRoll, double actualRollRate, double actualPitch, double actualPitchRate, double actualHeading, double actualAirspeed, double actualAltitude){
        lateralControlLoop(expectedHeading,actualHeading,actualRoll,actualRollRate);
        longitudinalControlLoop(expectedAirspeed,actualAirspeed,expectedAltitude,actualAltitude,actualPitch,actualPitchRate,actualRoll);
    }

    public double getExpectedAltitude() {
        return expectedAltitude;
    }

    public void setExpectedAltitude(double expectedAltitude) {
        this.expectedAltitude = expectedAltitude;
    }

    public double getExpectedHeading() {
        return expectedHeading;
    }

    public void setExpectedHeading(double expectedHeading) {
        this.expectedHeading = expectedHeading;
    }

    public double getExpectedAirspeed() {
        return expectedAirspeed;
    }

    public void setExpectedAirspeed(double expectedAirspeed) {
        this.expectedAirspeed = expectedAirspeed;
    }

    public void setRollPIGains(double kp, double ki, double kd){
        rollPI.setKp(kp);
        rollPI.setKi(ki);
        rollPI.setKd(kd);
    }

    public void setRollRatePGains(double kp, double ki, double kd){
        rollRateP.setKp(kp);
        rollRateP.setKi(ki);
        rollRateP.setKd(kd);
    }

    public void setHeadingPIDGains(double kp, double ki, double kd){
        headingPID.setKp(kp);
        headingPID.setKi(ki);
        headingPID.setKd(kd);
    }

    public void setPitchPIGains(double kp, double ki, double kd){
        pitchPI.setKp(kp);
        pitchPI.setKi(ki);
        pitchPI.setKd(kd);
    }

    public void setPitchRatePGains(double kp, double ki, double kd){
        pitchRateP.setKp(kp);
        pitchRateP.setKi(ki);
        pitchRateP.setKd(kd);
    }

    public void setRollPGains(double kp, double ki, double kd){
        rollP.setKp(kp);
        rollP.setKi(ki);
        rollP.setKd(kd);
    }

    public void airspeedPIDGains(double kp, double ki, double kd){
        airSpeedPID.setKp(kp);
        airSpeedPID.setKi(ki);
        airSpeedPID.setKd(kd);
    }

    public void setAltitudePIDGains(double kp, double ki, double kd){
        altitudePID.setKp(kp);
        altitudePID.setKi(ki);
        altitudePID.setKd(kd);
    }

    public void setOuterAirspeedPIDGains(double kp, double ki, double kd){
        outerAirspeedPID.setKp(kp);
        outerAirspeedPID.setKi(ki);
        outerAirspeedPID.setKd(kd);
    }

}
