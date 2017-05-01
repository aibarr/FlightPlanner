package cl.usach.abarra.flightplanner.model;

/**
 * Created by Usuario on 30/04/2017.
 */

class gpsPosition {
    private float   posX;
    private float   posY;

    public gpsPosition(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public void movePos(float x, float  y){
        this.posX = x;
        this.posY = y;
    }
}
