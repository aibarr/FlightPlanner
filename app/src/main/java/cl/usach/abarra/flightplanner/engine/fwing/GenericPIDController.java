/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */

package cl.usach.abarra.flightplanner.engine.fwing;

import android.util.Log;

/**
 * Controlador PID genérico para ser utilizado por lazos de control
 */
public class GenericPIDController {
    // Código del controlador PID basado en
    // http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/
    // Creado originalmente por Brett Beauregard
    // Licenciado bajo CC-BY-SA 3.0 : http://creativecommons.org/licenses/by-sa/3.0/
    // Algunos cambios fueron realizados para mejorar la legibilidad

    private long lastTime, curTime, deltaTime;
    private double inputValue, lastInputValue, outputValue, desiredValue;
    private double integralError, curError, lastError;
    private double kp,ki,kd;
    private double multiplicativeConst, additiveConst; //Valores que "transforman" la salida del PID
    private double minOutput, maxOutput;
    private double sampleTime = 20.0; //milisegundos entre cada cálculo
    private boolean activatedController; //Si PID está funcionando o no. En caso de no estar funcionando
    //equivale al modo manual de vuelo con radiocontrol

    /**
     * Crea un nuevo controlador PID.
     * Importante: debe llamarse a init() posteriormente.
     */
    public GenericPIDController(){

        curTime = System.currentTimeMillis();
        lastTime = curTime;
        deltaTime = -1;
        inputValue = lastInputValue = outputValue = desiredValue = 0.0;
        integralError = curError = lastError = 0.0;
        kp = ki = kd = 0.5;
        minOutput = 0.0; //Valor genérico! Cámbielo de ser necesario!
        maxOutput = 1.0; //Valor genérico! Cámbielo de ser necesario!
        activatedController = true;
        multiplicativeConst = 1.0;
        additiveConst = 0.0;
    }

    public boolean init(double kpGain, double kiGain, double kdGain, boolean startsActivated, double initialDesiredValue, double minOut, double maxOut){
       if((kpGain<0.0)||(kiGain<0.0)||(kdGain<0.0)){
            Log.e("PID Controller", "Error: los parámetros de ganancia de este controlador están mal configurados. Imposible controlar.");
            return false;
        }else if(minOut > maxOut){
            Log.e("PID Controller", "Error: el valor mínimo de salida es mayor al valor máximo. Imposible controlar.");
            return false;
       }

        this.kp = kpGain;
        this.ki = kiGain;
        this.kd = kdGain;
        this.activatedController = startsActivated;
        desiredValue = initialDesiredValue;
        maxOutput = maxOut;
        minOutput = minOut;
        return true;
    }


    /**
     * Ejecuta el proceso de control.
     * @return -1 si el controlador está inactivo
     *         0 si el controlador fue llamado antes de tiempo
     *         1 si el controlador realizó cálculo
     */
    public int doControl(){
        /*if(!activatedController){//No controlar, estamos en modo manual
            outputValue = 0;
            return -1;
        }*/
/*        curTime = System.currentTimeMillis();
        if(deltaTime==-1){ //Primera vez que se ejecuta el control
            lastTime = curTime;
            deltaTime = 0;
        } else {
            deltaTime = curTime - lastTime;
        }*/
      //  Log.i("PIDControl","Delay " + String.valueOf(deltaTime));
        if(true){
            curError = desiredValue - inputValue;
            integralError += ki*curError;

            //Límite de rango de salidas para el controlador integral (evita el wind-up)
            if(integralError > maxOutput){
                integralError = maxOutput;
            } else if(integralError < minOutput) {
                integralError = minOutput;
            }

            double derivatedInput = inputValue - lastInputValue;

            outputValue = kp*curError + integralError - kd*derivatedInput;

            //Transformando el valor original de salida
            outputValue = outputValue*multiplicativeConst + additiveConst;

            //Límite de rango de salidas
            if(outputValue > maxOutput){
                outputValue = maxOutput;
            } else if (outputValue < minOutput){
                outputValue = minOutput;
            }

            lastInputValue = inputValue;
            lastError = curError;
            lastTime = curTime;
            return 1;
        }
        return 0;
    }

    public void setDesiredValue(double desiredValue) {
        this.desiredValue = desiredValue;
    }

    public double getKd() {
        return kd;
    }

    public void setKd(double newKd) {
        double sampleInSecs = (double)sampleTime/1000;
        this.kd = newKd/sampleInSecs;
    }

    public double getKi() {
        return ki;
    }

    public void setKi(double newKi) {
        double sampleInSecs = (double)sampleTime/1000;
        this.ki = newKi*sampleInSecs;
    }

    public double getKp() {
        return kp;
    }

    public void setKp(double kp) {
        this.kp = kp;
    }

    public double getInputValue() {
        return inputValue;
    }

    public void setInputValue(double inputValue) {
        this.inputValue = inputValue;
    }

    public double getOutputValue() {
        return outputValue;
    }

    public void setSampleTime(int newSampleTime) {
        if(newSampleTime > 0){
            double ratio = (double)newSampleTime/(double)sampleTime;
            ki *= ratio;
            kd /= ratio;
            sampleTime = newSampleTime;
        }
    }

    public void setOutputLimits (double minOutputValue, double maxOutputValue){
        if(minOutputValue > maxOutputValue) return;
        this.minOutput = minOutputValue;
        this.maxOutput = maxOutputValue;

        if (outputValue > maxOutput){
            outputValue = maxOutput;
        } else if (outputValue < minOutput){
            outputValue = minOutput;
        }

        if (integralError > maxOutput){
            integralError = maxOutput;
        } else if (integralError < minOutput){
            integralError = minOutput;
        }
    }

    public void setMode(boolean isActivated){
        if(isActivated && !activatedController){
            initialize();
        }
        activatedController = isActivated;
    }

    public void initialize(){
        lastInputValue = inputValue;
        integralError = outputValue;
        if (integralError > maxOutput){
            integralError = maxOutput;
        } else if (integralError < minOutput){
            integralError = minOutput;
        }
    }

    public double getMultiplicativeConst() {
        return multiplicativeConst;
    }

    public void setMultiplicativeConst(double multiplicativeConst) {
        this.multiplicativeConst = multiplicativeConst;
    }

    public double getAdditiveConst() {
        return additiveConst;
    }

    public void setAdditiveConst(double additiveConst) {
        this.additiveConst = additiveConst;
    }
}
