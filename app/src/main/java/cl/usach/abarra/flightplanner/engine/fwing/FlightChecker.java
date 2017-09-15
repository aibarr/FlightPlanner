/*
 *  © 2016, Armando Rojas
 *  Código licenciado de acuerdo a los términos
 *  de la Licencia MIT.
 *
 */
package cl.usach.abarra.flightplanner.engine.fwing;

/**
 * Ejecuta pruebas sobre los componentes del vehículo aéreo y la interfaz.<br><br>
 *
 * No autorizará el vuelo a menos que la totalidad de las pruebas haya sido realizada y sus
 * resultados sean indicados como Correctos. <br><br>
 *<p>
 *Para la ejecución de una prueba, existen dos formas de verificar un componente:
 * <ul>
 *     <li>Por verificación visual de una funcionalidad (movimiento de servomotores, motor)
 *     o comportamiento (estabilización automática ante perturbación inducida)</li><br>
 *     <li>Por verificación del sistema a través de la obtención de información de estado de un
 *     componente</li>
 * </ul>
 */
public class FlightChecker {
    //TODO considerar un test automático de sensores durante el armado que aborte éste si hay algún problema
    private static boolean pruebaAcelerometro = false;
    private static boolean pruebaAlerones = false;
    private static boolean pruebaBalance = false;
    private static boolean pruebaControlRemoto = false;
    private static boolean pruebaGiros = false;
    private static boolean pruebaGPS = false;
    private static boolean pruebaMagneto = false;
    private static boolean pruebaMotor = false;
    private static boolean pruebaSensoresExt = false;
    private static boolean pruebaBateria = false;

    /**
     * Determina si todas las pruebas han sido marcadas como satisfactorias
     * @return <ul>
     *          <li><b>true</b> si todas las pruebas han sido correctas</li>
     *          <li><b>false</b> si una o más de las pruebas son incorrectas o no han sido realizadas</li>
     * </ul>
     */
    public static boolean allTestsOK(){
        if(pruebaAcelerometro && pruebaAlerones && pruebaBalance && pruebaBateria && pruebaGiros
                && pruebaControlRemoto && pruebaGPS
                && pruebaMagneto && pruebaMotor && pruebaSensoresExt){
            InterfaceControlService.isReady = true;
            return true;
        } else {
            InterfaceControlService.isReady = false;
            return false;
        }
    }

    /**
     * Obtiene un arreglo con los resultados de cada prueba en el siguiente orden:
     * <ul>
     *     <li>Acelerómetro</li> <li>Alerones</li> <li>Balance</li> <li>Baterías</li> <li>Control remoto</li>
     *     <li>Giroscopios</li> <li>GPS</li> <li>Magnetómetro</li> <li>Motor</li> <li>Sensores externos</li>*
     * </ul>
     * @return Un arreglo de longitud 10, conteniendo los resultados de las pruebas, <b>true</b> para
     * aquellas pruebas que han sido indicadas como correctas y <b>false</b> si han sido marcadas como
     * incorrectas o no se han ejecutado.
     */
    public boolean[] getTestResults(){
        boolean[] result = new boolean[10];
        result[0] = pruebaAcelerometro;
        result[1] = pruebaAlerones;
        result[2] = pruebaBalance;
        result[3] = pruebaBateria;
        result[4] = pruebaControlRemoto;
        result[5] = pruebaGiros;
        result[6] = pruebaGPS;
        result[7] = pruebaMagneto;
        result[8] = pruebaMotor;
        result[9] = pruebaSensoresExt;

        return result;
    }

    public static boolean isPruebaAcelerometro() {
        return pruebaAcelerometro;
    }

    public static void setPruebaAcelerometro(boolean pruebaAcelerometro) {
        FlightChecker.pruebaAcelerometro = pruebaAcelerometro;
    }

    public static boolean isPruebaAlerones() {
        return pruebaAlerones;
    }

    public static void setPruebaAlerones(boolean pruebaAlerones) {
        FlightChecker.pruebaAlerones = pruebaAlerones;
    }

    public static boolean isPruebaBalance() {
        return pruebaBalance;
    }

    public static void setPruebaBalance(boolean pruebaBalance) {
        FlightChecker.pruebaBalance = pruebaBalance;
    }

    public static boolean isPruebaControlRemoto() {
        return pruebaControlRemoto;
    }

    public static void setPruebaControlRemoto(boolean pruebaControlRemoto) {
        FlightChecker.pruebaControlRemoto = pruebaControlRemoto;
    }

    public static boolean isPruebaGiros() {
        return pruebaGiros;
    }

    public static void setPruebaGiros(boolean pruebaGiros) {
        FlightChecker.pruebaGiros = pruebaGiros;
    }

    public static boolean isPruebaGPS() {
        return pruebaGPS;
    }

    public static void setPruebaGPS(boolean pruebaGPS) {
        FlightChecker.pruebaGPS = pruebaGPS;
    }

    public static boolean isPruebaMagneto() {
        return pruebaMagneto;
    }

    public static void setPruebaMagneto(boolean pruebaMagneto) {
        FlightChecker.pruebaMagneto = pruebaMagneto;
    }

    public static boolean isPruebaMotor() {
        return pruebaMotor;
    }

    public static void setPruebaMotor(boolean pruebaMotor) {
        FlightChecker.pruebaMotor = pruebaMotor;
    }

    public static boolean isPruebaSensoresExt() {
        return pruebaSensoresExt;
    }

    public static void setPruebaSensoresExt(boolean pruebaSensoresExt) {
        FlightChecker.pruebaSensoresExt = pruebaSensoresExt;
    }

    public static boolean isPruebaBateria() {
        return pruebaBateria;
    }

    public static void setPruebaBateria(boolean pruebaBateria) {
        FlightChecker.pruebaBateria = pruebaBateria;
    }
}
