package cl.usach.abarra.flightplanner.util;

public class Rect {
    public Double top;
    public Double bottom;
    public Double left;
    public Double right;
    public Double width;
    public Double height;

    public Rect(){

    }

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

    public Double midWidth(){
        return ((right - left)/ 2) + left;
    }

    public Double midHeight(){
        return ((top - bottom)/2) + bottom;
    }

    public Double diagDistance() {
        //Pitagoras

        if (width== null) this.width = left - right;
        if (height == null) this.height =  bottom - top;

        return  Math.sqrt(Math.pow(this.width, 2) + Math.pow(this.height, 2));
    }
}
