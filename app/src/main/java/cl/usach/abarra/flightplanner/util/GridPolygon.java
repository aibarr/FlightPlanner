package cl.usach.abarra.flightplanner.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alfredo Barra on 30-08-2017. Pre-grade project.
 */


//TODO: añadir algoritmo para calcular poligonos
public class GridPolygon {
    List<LatLng> vertices;
    List<LatLng> finalRoute;
    Double altitude;
    List<LatLng> grid;
    Double area;
    LatLng center;


    public GridPolygon() {
        this.grid = new ArrayList<LatLng>();
    }

    public GridPolygon(List<LatLng> vertices) {
        this.vertices = vertices;
        this.calculateArea();
        this.calculateCenter();
        this.grid = new ArrayList<LatLng>();
    }

    public List<LatLng> getGrid() {
        return grid;
    }

    public LatLng getCenter() {
        return center;
    }

    private void calculateArea(){
        this.area= 0.0;
        LatLng[] vertices = this.vertices.toArray(new LatLng[this.vertices.size()]);

        for ( int i = 0; i < vertices.length - 1; i++) {
            this.area = this.area + 0.5 * (vertices[i].latitude * vertices[i+1].longitude - vertices[i+1].latitude * vertices[i].longitude);
        }
    }

    private void calculateCenter(){
        if (this.area == null || this.area == 0.0){
            this.calculateArea();
        }

        LatLng[] vertices = this.vertices.toArray(new LatLng[this.vertices.size()]);

        Double Cx = 0.0;
        Double Cy = 0.0;

        for (int i = 0; i < vertices.length - 1; i++ ){
            Double Comm = (vertices[i].latitude * vertices[i+1].longitude - vertices[i+1].latitude*vertices[i].longitude);
            Cx = Cx + (1/(6*this.area))*((vertices[i].latitude + vertices[i+1].latitude) * Comm);
            Cy = Cy + (1/(6*this.area))*((vertices[i].longitude + vertices[i+1].longitude) * Comm);
        }
        this.center = new LatLng(Cx, Cy);
    }

    public void calculateGrid(Double orientation){

        System.out.println("Calculando Grilla");
        if (this.center == null) this.center = getPolygonCenterPoint((ArrayList<LatLng>) this.vertices);

        //Ecuación de la recta
        Double m = orientation;  //deg or rad?

        Double x0 = this.center.latitude;

        List<LatLng> ptsIzq = new ArrayList<LatLng>();
        List<LatLng> ptsDer = new ArrayList<LatLng>();

        //Encontrar puntos ancho y largo del poligono

        Double x1, x2, y1, y2;

        y1 = -90.0;
        x1 = -180.0;
        y2 = 90.0;
        x2 = 180.0;

        //encajonemos el poligono
        for (LatLng vertice : this.vertices){
            if (y1 < vertice.latitude) y1 = vertice.latitude;
            if (x1 < vertice.longitude) x1 = vertice.longitude;

            if (y2 > vertice.latitude) y2 = vertice.latitude;
            if (x2 > vertice.longitude) x2 = vertice.longitude;
        }

        //calcular la separación en Y

        Double parallelGap = 0.1;//Cambiar por setting por defecto

        Double cscOrientation = csc(orientation);

        System.out.println(cscOrientation);

        Double yGap = cscOrientation * parallelGap;

        if (yGap < 0) yGap = yGap * (-1);

        Double B = y2 - m * x1;



        Double tempXder, tempYder, tempXizq, tempYizq;

        tempXder = x1;
        tempYder = y2;

        //Calcular cada recta desde una punta del cuadrilatero
        while(tempYder > y1){
            System.out.println("primera mitad");

            tempYder = tempXder * m + B;
            if(tempYder < y1) tempYder = y1;
            ptsDer.add(new LatLng(tempYder,tempXder));

            tempXizq = (y2 - B) / m;
            if (tempXizq > x2) tempXizq = x2;
            ptsIzq.add(new LatLng(y2, tempXizq));

            B = B - yGap;
        }



       /* while(tempXder < x2){
            System.out.println("segunda mitad");
            tempXder = (y1 - B)/m;
            if (tempXder < x1) tempXder = x1;
            ptsDer.add(new LatLng(tempXder, tempYder));

            tempXizq = x2;
            tempYizq = (m*x2 + B);
            if (tempYizq > y2) tempXizq = y2;
            ptsIzq.add(new LatLng(tempXizq,tempYizq));

            B -= yGap;
        }*/


        //TODO: recortar grilla


        //regenerar la grilla

        Boolean derecha = true;

        for (int i = 0; i< ptsDer.size(); i++){
            if(derecha){
                this.grid.add(ptsDer.get(i));
                this.grid.add(ptsIzq.get(i));
                derecha = false;
            }else{
                this.grid.add(ptsIzq.get(i));
                this.grid.add(ptsDer.get(i));
                derecha = true;
            }
        }

    }

    /**
     * compute cosecant
     * @param theta angle in radians
     * @return cosecant of theta.
     */
    public double csc ( double theta )
    {
        return 1.0 / Math.sin( theta );
    }

    private LatLng getPolygonCenterPoint(ArrayList<LatLng> polygonPointsList){
        LatLng centerLatLng = null;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(int i = 0 ; i < polygonPointsList.size() ; i++)
        {
            builder.include(polygonPointsList.get(i));
        }
        LatLngBounds bounds = builder.build();
        centerLatLng =  bounds.getCenter();

        return centerLatLng;
    }

}

