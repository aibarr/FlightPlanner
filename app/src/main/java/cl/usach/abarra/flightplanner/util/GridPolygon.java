package cl.usach.abarra.flightplanner.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        if (this.center == null) this.calculateCenter();
        return center;
    }

    private void calculateArea(){
        this.area= 0.0;
        LatLng[] vertices = this.vertices.toArray(new LatLng[this.vertices.size()]);

        for ( int i = 0; i < vertices.length-1; i++) {
            this.area = this.area + ( (vertices[i].longitude * vertices[i+1].latitude)-( vertices[i+1].longitude * vertices[i].latitude)); //(xi * yi+1) - (xi+1 * yi)
        }

        this.area = 0.5 * this.area;
    }

    private void calculateCenter(){
        if (this.area == null || this.area == 0.0){
            this.calculateArea();
        }

        LatLng[] vertices = this.vertices.toArray(new LatLng[this.vertices.size()]);

        Double Cx = 0.0;
        Double Cy = 0.0;

        for (int i = 0; i < vertices.length - 1; i++ ){
            Cx = Cx + ((vertices[i].longitude + vertices[i+1].longitude) * ((vertices[i].longitude * vertices[i+1].latitude) - (vertices[i+1].longitude * vertices[i].latitude)));
            Cy = Cy + ((vertices[i].latitude + vertices[i+1].latitude) * ((vertices[i].longitude * vertices[i+1].latitude) - (vertices[i+1].longitude * vertices[i].latitude)));
        }
        double aux = (1/(6*this.area));
        Cx = aux * Cx;
        Cy = aux * Cy;
        this.center = new LatLng(Cy, Cx);
    }

    public void calculateGrid(Double orientation){
        System.out.println("Calculando Grilla");



        //TODO:Cambiar por setting por defecto
        Double parallelGap = 0.5;

        if (this.center == null) this.center = getPolygonCenterPoint((ArrayList<LatLng>) this.vertices);

        Double x0 = this.center.longitude;

        //Encontrar puntos ancho y largo del poligono
        Double x1, x2, y1, y2;

        List<Double> Xs = new ArrayList<Double>();
        List<Double> Ys = new ArrayList<Double>();

        //encajonemos el poligono
        for (LatLng vertice : this.vertices){
            Xs.add(vertice.longitude);
            Ys.add(vertice.latitude);
        }
        x1 = Collections.min(Xs);
        x2 = Collections.max(Xs);
        y1 = Collections.min(Ys);
        y2 = Collections.max(Ys);
        System.out.println("La caja es:\nX1:"+ x1 +" Y1:" + y1+ "\nX2:" + x2+ " Y2:" + y2);

        //Ecuación de la recta
        //TODO: Calcular pendiente según orientacion
        if (orientation==(Math.PI*1/2)||orientation==(Math.PI*1/2)){
            //SI la recta es vertical
            List<LatLng> ptsArriba = new ArrayList<LatLng>();
            List<LatLng> ptsAbajo = new ArrayList<LatLng>();


        } else if (orientation==0||orientation==Math.PI){
            //SI la recta es horizontal
            List<LatLng> ptsIzq = new ArrayList<LatLng>();
            List<LatLng> ptsDer = new ArrayList<LatLng>();

        }else {
            List<LatLng> ptsIzq = new ArrayList<LatLng>();
            List<LatLng> ptsDer = new ArrayList<LatLng>();
            //si la recta es diagonal
            Double m = Math.tan(Math.PI);  //deg or rad?
            System.out.println("M= " + m);
            //calcular la separación en Y

            Double cscOrientation = csc(orientation);

            Double yGap = cscOrientation * parallelGap;
            System.out.println("GAP is:"+yGap);

            if (yGap < 0) yGap = yGap * (-1);


            System.out.println("La separación es: "+ yGap);


            Double tempXder, tempYder, tempXizq, tempYizq;

            tempXizq = x1;
            tempYizq = y2 - yGap;
            Double B = tempYizq - m * tempXizq;
            tempYder = y2;

            System.out.println("B: "+B);
            System.out.println("tyi= " + tempYizq + " y1= " + y1);

            System.out.println("primera mitad");

            //Calcular cada recta desde una punta del cuadrilatero
            while (y1.compareTo(tempYizq)<0){

                //Genero el punto
                ptsIzq.add(new LatLng(tempYizq, tempXizq));

                //calculamos X derecho
                tempXder = (tempYder - B) / m;
                if (tempXder >= x2) {
                    tempXder = x2;
                    tempYder = tempYder - yGap;
                }

                //añado puntos a la derecha
                ptsDer.add(new LatLng(tempYder, tempXder));

                //Calculo la ecuacion nueva
                B = B - yGap;
                tempYizq = tempYizq - yGap;
                System.out.println("B1: "+ B + " X1: " + tempXizq +" Y1: " + tempYizq);

            }

            System.out.printf("Segunda Mitad");
            do {

                if (tempYizq < y1) tempYizq = y1;
                tempXizq = (tempYizq - B) / m;

                //genero el punto izquierdo
                ptsIzq.add(new LatLng(tempYizq, tempXizq));

                //calculo el punto derecho

                tempXder = (tempYder - B) / m;
                if (tempXder >= x2) {
                    tempXder = x2;
                    tempYder -= yGap;
                }

                ptsDer.add(new LatLng(tempYder, tempXder));

                System.out.println("B2: "+ B + " X2: " + tempXizq +" Y2: " + tempYizq);

                B = B - yGap;

            } while (tempXizq.compareTo(x2) < 0 && tempYder.compareTo(y1)>0);



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

