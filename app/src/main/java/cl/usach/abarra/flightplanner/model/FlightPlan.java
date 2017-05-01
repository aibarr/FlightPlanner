package cl.usach.abarra.flightplanner.model;

import java.io.File;

/**
 * Created by Usuario on 30/04/2017.
 */


//Arreglo de puntos para el plan de vuelo
class FlightPlan {
    private Waypoint home;
    private Waypoint[] route;

    //Constructor de plan de vuelo
    public FlightPlan(Waypoint home, Waypoint...    plan){
        this.home = new Waypoint();
        this.home = home;
        for (int i=0; i<=plan.length; i++){
            this.route[i]=plan[i];
        }
    }

    //Guardar el plan de vuelo en disco
    public int savePlan(){
        File plan;
        return 0;
    };
}
