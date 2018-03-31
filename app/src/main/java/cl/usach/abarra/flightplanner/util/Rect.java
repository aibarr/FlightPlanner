package cl.usach.abarra.flightplanner.util;

public class Rect {
    public Double top;
    public Double bottom;
    public Double left;
    public Double right;
    public Double width;
    public Double height;

    public Rect(Double left, Double top, Double width, Double height){
        this.left = left;
        this.top = top;
        this.right = left + width;
        this.bottom = top + height;
    }

    public Double getWidth() {
        return left - right;
    }

    public Double getHeight() {
        return bottom - top;
    }

    public Double MidWidth(){
        return ((right - left)/ 2) + left;
    }

    public Double MidHeight(){
        return ((top - bottom)/2) + bottom;
    }

    public Double DisgDistance() {
        //Pitagoras
        return  Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
    }
}
