package cl.usach.abarra.flightplanner.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alfredo Barra on 30-08-2017. Pre-grade project.
 */


public class GridPolygon {
    private List<LatLng> vertices;
    List<LatLng> finalRoute;
    Double altitude;
    private List<LatLng> grid;
    private Double area;
    private LatLng center;

    private static Double rad2deg = (180 / Math.PI);
    private static Double deg2rad = (1.0 / rad2deg);

    private static PointLatLngAlt StartPointLatLngAlt = PointLatLngAlt.zero;

    public GridPolygon() {
        this.grid = new ArrayList<LatLng>();
    }

    public GridPolygon(List<LatLng> vertices) {
        this.vertices = vertices;
        this.calculateArea();
        this.calculateCenter();
        this.grid = new ArrayList<LatLng>();
    }

    public void setVertices(List<LatLng> vertices) {
        this.vertices = vertices;
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
        Double aux = (1/(6*this.area));
        Cx = aux * Cx;
        Cy = aux * Cy;
        this.center = new LatLng(Cy, Cx);
    }

    public void calculateGridOLD(Double orientation){
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
        if (orientation==(Math.PI*1/2)||orientation==(Math.PI*3/2)){
            //SI la recta es vertical
            List<LatLng> ptsArriba = new ArrayList<LatLng>();
            List<LatLng> ptsAbajo = new ArrayList<LatLng>();

            Double tempX = x1;

            while (tempX < x2){
                ptsArriba.add(new LatLng(y1, tempX));
                ptsAbajo.add(new LatLng(y2, tempX));

                tempX += parallelGap;
            }


        } else if (orientation==0||orientation==Math.PI){
            //SI la recta es horizontal
            List<LatLng> ptsIzq = new ArrayList<LatLng>();
            List<LatLng> ptsDer = new ArrayList<LatLng>();

            Double tempY = y2;

            while (tempY > y1){
                ptsIzq.add(new LatLng(tempY,x1));
                ptsDer.add(new LatLng(tempY,x2));

                tempY -= parallelGap;
            }

        }else{
            //si la recta es diagonal
            List<LatLng> ptsIzq = new ArrayList<LatLng>();
            List<LatLng> ptsDer = new ArrayList<LatLng>();
            Double m = Math.atan(orientation);  //deg
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
    private Double csc(Double theta)
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

    public void calculateGridMP(Double height, Double distance, Double spacing, Double angle, float minLaneSeparation, float leadin, StartPosition startpos, PointLatLngAlt homeLocation){
        //Estoy en Mission Planner
        System.out.println("mission Planner");

        if (spacing < 4 && spacing != 0)
            spacing = 4.0;

        if (distance < 0.1)
            distance = 0.1;

        List<PointLatLngAlt> polygon = new ArrayList<PointLatLngAlt>();

        if (this.vertices.size() == 0){
            this.vertices = new ArrayList<>();
            return;
        }else{
            for (LatLng vertice : vertices){
                polygon.add(new PointLatLngAlt(vertice));
            }
        }

        //Make a non round number in case of corner cases
        if (minLaneSeparation != 0){
            minLaneSeparation += 0.5F;
        }
        //Lane separation in meters
        Double minLaneSeparationInMeters = minLaneSeparation * distance;

        List<PointLatLngAlt> ans = new ArrayList<PointLatLngAlt>();

        //utm zone distance calcs will be done in
        int utmzone = polygon.get(0).getUTMZone();

        //utm position list
        List<UTMPos> UTMPositions = UTMPos.toList(PointLatLngAlt.ToUTM(utmzone, polygon), utmzone);


        // close the loop if its not already
        if (UTMPositions.get(0) != UTMPositions.get(UTMPositions.size() - 1))
            UTMPositions.add(UTMPositions.get(0));
        //Get mins max coverage area
        Rect area = getPolyMinMax(UTMPositions);

        //Used to determine the size of the outer grid area
        Double diagdist = area.diagDistance();

        //somewhere to store out generated lines
        List<LatLngLine> grid = new ArrayList<LatLngLine>();
        //number of lines we need
        int lines = 0;

        //get start point middle

        Double x = area.midWidth();
        Double y = area.midHeight();

        addtomap(new UTMPos(x, y, utmzone),"Base");//no sirve de nada

        // get left extent
        Double xb1 = x;
        Double yb1 = y;

        Double[] changePos;
        // to the left
        changePos = newpos( xb1,  yb1, angle - 90, diagdist / 2 + distance);
        xb1 = Double.valueOf(changePos[0]);
        yb1 = Double.valueOf(changePos[1]);
        // backwards
        changePos = newpos( xb1,  yb1, angle + 180, diagdist / 2 + distance);
        xb1 = Double.valueOf(changePos[0]);
        yb1 = Double.valueOf(changePos[1]);

        UTMPos left = new UTMPos(xb1, yb1, utmzone);

        addtomap(left, "left");

        // get right extent
        Double xb2 = x;
        Double yb2 = y;
        // to the right
        changePos = newpos( xb2,  yb2, angle + 90, diagdist / 2 + distance);
        xb2 = Double.valueOf(changePos[0]);
        yb2 = Double.valueOf(changePos[1]);
        // backwards
        changePos = newpos( xb2,  yb2, angle + 180, diagdist / 2 + distance);
        xb2 = Double.valueOf(changePos[0]);
        yb2 = Double.valueOf(changePos[1]);

        UTMPos right = new UTMPos(xb2, yb2, utmzone);

        addtomap(right,"right");

        // set start point to left hand side
        x = xb1;
        y = yb1;

        // draw the outergrid, this is a grid that cover the entire area of the rectangle plus more.
        while (lines < ((diagdist + distance * 2) / distance))
        {
            // copy the start point to generate the end point
            Double nx = Double.valueOf(x);
            Double ny = Double.valueOf(y);
            changePos = newpos( nx,  ny, angle, diagdist + distance*2);

            nx = Double.valueOf(changePos[0]);
            ny = Double.valueOf(changePos[1]);


            //TODO:Revisar la copia de memoria. Esta quedando la cagá con el movimiento de memoria y me modifica valores que no quiero modificar
            LatLngLine line = new LatLngLine();
            line.p1 = new UTMPos(x, y, utmzone);
            line.p2 = new UTMPos(nx, ny, utmzone);
            line.basepnt = new UTMPos(x, y, utmzone);
            grid.add(line);

            // addtomap(line);

            changePos = newpos( x,  y, angle + 90, distance);

            x = Double.valueOf(changePos[0]);
            y = Double.valueOf(changePos[1]);
            lines++;
        }

        // find intersections with our polygon

        // store lines that dont have any intersections
        List<LatLngLine> remove = new ArrayList<LatLngLine>();

        int gridno = grid.size();

        // cycle through our grid
        for (int a = 0; a < gridno; a++)
        {
            Double closestdistance = Double.MAX_VALUE;
            Double farestdistance = Double.MIN_VALUE;

            UTMPos closestpoint = new UTMPos(UTMPos.zero);
            UTMPos farestpoint = new UTMPos(UTMPos.zero);

            // somewhere to store our intersections
            List<UTMPos> matchs = new ArrayList<UTMPos>();

            int b = -1;
            int crosses = 0;
            UTMPos newUTMPos = new UTMPos(UTMPos.zero);
            for (UTMPos pnt : UTMPositions)
            {
                b++;
                if (b == 0)
                {
                    continue;
                }
                newUTMPos = FindLineIntersection(UTMPositions.get(b - 1), UTMPositions.get(b), grid.get(a).p1, grid.get(a).p2);
                if (!newUTMPos.isZero())
                {
                    crosses++;
                    matchs.add(newUTMPos);
                    if (closestdistance > grid.get(a).p1.GetDistance(newUTMPos))
                    {
                        closestpoint.y = newUTMPos.y;
                        closestpoint.x = newUTMPos.x;
                        closestpoint.zone = newUTMPos.zone;
                        closestdistance = grid.get(a).p1.GetDistance(newUTMPos);
                    }
                    if (farestdistance < grid.get(a).p1.GetDistance(newUTMPos))
                    {
                        farestpoint.y = newUTMPos.y;
                        farestpoint.x = newUTMPos.x;
                        farestpoint.zone = newUTMPos.zone;
                        farestdistance = grid.get(a).p1.GetDistance(newUTMPos);
                    }
                }
            }
            if (crosses == 0) // outside our polygon
            {
                if (!PointInPolygon(grid.get(a).p1, UTMPositions) && !PointInPolygon(grid.get(a).p2, UTMPositions))
                    remove.add(grid.get(a));
            }
            else if (crosses == 1) // bad - shouldnt happen
            {

            }
            else if (crosses == 2) // simple start and finish
            {
                LatLngLine line = grid.get(a);
                line.p1 = closestpoint;
                line.p2 = farestpoint;
                grid.set(a, line);
            }
            else // multiple intersections
            {
                LatLngLine line = grid.get(a);
                remove.add(line);

                while (matchs.size() > 1)
                {
                    LatLngLine newline = new LatLngLine();

                    closestpoint = findClosestPoint(closestpoint, matchs);
                    newline.p1 = closestpoint;
                    matchs.remove(closestpoint);

                    closestpoint = findClosestPoint(closestpoint, matchs);
                    newline.p2 = closestpoint;
                    matchs.remove(closestpoint);

                    newline.basepnt = line.basepnt;

                    grid.add(newline);
                }
            }
        }

        // cleanup and keep only lines that pass though our polygon
        for (LatLngLine line : remove)
        {
            grid.remove(line);
        }

        // debug
        for (LatLngLine line : grid)
        {
            addtomap(line);
        }

        if (grid.size() == 0)
            return;

        // pick start positon based on initial point rectangle
        UTMPos startposutm;

        switch (startpos)
        {
            default:
            case Home:
                startposutm = new UTMPos(homeLocation);
                break;
            case BottomLeft:
                startposutm = new UTMPos(area.left, area.bottom, utmzone);
                break;
            case BottomRight:
                startposutm = new UTMPos(area.right, area.bottom, utmzone);
                break;
            case TopLeft:
                startposutm = new UTMPos(area.left, area.top, utmzone);
                break;
            case TopRight:
                startposutm = new UTMPos(area.right, area.top, utmzone);
                break;
            case Point:
                startposutm = new UTMPos(StartPointLatLngAlt);
                break;
        }

        // find the closes polygon point based from our startpos selection
        startposutm = findClosestPoint(startposutm, UTMPositions);

        // find closest line point to startpos
        LatLngLine closest = findClosestLine(startposutm, grid, 0.0 /*Lane separation does not apply to starting point*/, angle);

        UTMPos lastpnt;

        // get the closes point from the line we picked
        if (closest.p1.GetDistance(startposutm) < closest.p2.GetDistance(startposutm))
        {
            lastpnt = closest.p1;
        }
        else
        {
            lastpnt = closest.p2;
        }

        // S =  start
        // E = end
        // ME = middle end
        // SM = start middle

        while (grid.size() > 0)
        {
            // for each line, check which end of the line is the next closest
            if (closest.p1.GetDistance(lastpnt) < closest.p2.GetDistance(lastpnt))
            {
                UTMPos newstart = newpos(closest.p1, angle,(double)(-leadin));
                newstart.tag = "S";

                addtomap(newstart, "S");
                ans.add(newstart.toLLA());

                closest.p1.tag = "SM";
                addtomap(closest.p1, "SM");
                ans.add(closest.p1.toLLA());

                if (spacing > 0)
                {
                    for (int d = (int)(spacing - ((closest.basepnt.GetDistance(closest.p1)) % spacing));
                         d < (closest.p1.GetDistance(closest.p2));
                         d += spacing.intValue())
                    {
                        Double ax = closest.p1.x;
                        Double ay = closest.p1.y;


                        changePos = newpos( ax,  ay, angle, (double)d);

                        ax = Double.valueOf(changePos[0]);
                        ay = Double.valueOf(changePos[1]);

                        UTMPos UTMPos1 = new UTMPos(ax, ay, utmzone) ;
                        UTMPos1.tag = "M";
                        addtomap(UTMPos1, "M");
                        ans.add(UTMPos1.toLLA());
                    }
                }

                closest.p2.tag = "ME";
                addtomap(closest.p2, "ME");
                ans.add(closest.p2.toLLA());

                UTMPos newend = newpos(closest.p2, angle, 0.0);
                newend.tag = "E";
                addtomap(newend, "E");
                ans.add(newend.toLLA());

                lastpnt = closest.p2;

                grid.remove(closest);
                if (grid.size() == 0)
                    break;

                closest = findClosestLine(newend, grid, minLaneSeparationInMeters, angle);
            }
            else
            {
                UTMPos newstart = newpos(closest.p2, angle, (double)leadin);
                newstart.tag = "S";
                addtomap(newstart, "S");
                ans.add(newstart.toLLA());

                closest.p2.tag = "SM";
                addtomap(closest.p2, "SM");
                ans.add(closest.p2.toLLA());

                if (spacing > 0)
                {
                    for (int d = (int)((closest.basepnt.GetDistance(closest.p2)) % spacing);
                         d < (closest.p1.GetDistance(closest.p2));
                         d += spacing.intValue())
                    {
                        Double ax = closest.p2.x;
                        Double ay = closest.p2.y;


                        changePos = newpos( ax,  ay, angle,(double) -d);

                        ax = Double.valueOf(changePos[0]);
                        ay = Double.valueOf(changePos[1]);

                        UTMPos UTMPos2 = new UTMPos(ax, ay, utmzone) ;
                        UTMPos2.tag = "M";
                        addtomap(UTMPos2, "M");
                        ans.add(UTMPos2.toLLA());
                    }
                }

                closest.p1.tag = "ME";
                addtomap(closest.p1, "ME");
                ans.add(closest.p1.toLLA());

                UTMPos newend = newpos(closest.p1, angle, 0.0);
                newend.tag = "E";
                addtomap(newend, "E");
                ans.add(newend.toLLA());

                lastpnt = closest.p1;

                grid.remove(closest);
                if (grid.size() == 0)
                    break;
                closest = findClosestLine(newend, grid, minLaneSeparationInMeters, angle);
            }
        }

        // set the altitude on all points
        for (PointLatLngAlt plla : ans){
            plla.alt = height;
        };

        this.grid = new ArrayList<LatLng>();

        for (PointLatLngAlt point : ans){
            this.grid.add(new LatLng(point.lat, point.lng));
        }

        System.out.println("Calculado");

    }

    // polar to rectangular
    private static Double[] newpos(Double x, Double y, Double bearing, Double distance)
    {
        Double degN = 90 - bearing;
        if (degN < 0)
            degN += 360;
        Double nx = x + distance * Math.cos(degN * deg2rad);
        Double ny = y + distance * Math.sin(degN * deg2rad);

        Double[] ans = {nx, ny};
        return ans;
    }

    // polar to rectangular
    private static UTMPos newpos(UTMPos input, Double bearing, Double distance)
    {
        Double degN = 90 - bearing;
        if (degN < 0)
            degN += 360;
        Double x = input.x + distance * Math.cos(degN * deg2rad);
        Double y = input.y + distance * Math.sin(degN * deg2rad);

        return new UTMPos(x, y, input.zone);
    }

    private static Rect getPolyMinMax(List<UTMPos> UTMPos)
    {
        if (UTMPos.size() == 0)
            return new Rect();

        Double minx, miny, maxx, maxy;

        minx = maxx = UTMPos.get(0).x;
        miny = maxy = UTMPos.get(0).y;

        for (UTMPos pnt : UTMPos)
        {
            minx = Math.min(minx, pnt.x);
            maxx = Math.max(maxx, pnt.x);

            miny = Math.min(miny, pnt.y);
            maxy = Math.max(maxy, pnt.y);
        }

        System.out.println("Rectangulo listo");

        return new Rect(minx, maxy, maxx - minx,miny - maxy);
    }

    private static void addtomap(LatLngLine pos)
    {

    }

    private static void addtomap(UTMPos pos, String tag)
    {

    }

    /// <summary>
    /// from http://stackoverflow.com/questions/1119451/how-to-tell-if-a-line-intersects-a-polygon-in-c
    /// </summary>
    /// <param name="start1"></param>
    /// <param name="end1"></param>
    /// <param name="start2"></param>
    /// <param name="end2"></param>
    /// <returns></returns>
    private static UTMPos FindLineIntersection(UTMPos start1, UTMPos end1, UTMPos start2, UTMPos end2)
    {
        Double denom = ((end1.x - start1.x) * (end2.y - start2.y)) - ((end1.y - start1.y) * (end2.x - start2.x));
        //  AB & CD are parallel
        if (denom == 0.0d)
            return new UTMPos(UTMPos.zero);
        Double numer = ((start1.y - start2.y) * (end2.x - start2.x)) - ((start1.x - start2.x) * (end2.y - start2.y));
        Double r = numer / denom;
        Double numer2 = ((start1.y - start2.y) * (end1.x - start1.x)) - ((start1.x - start2.x) * (end1.y - start1.y));
        Double s = numer2 / denom;
        if ((r < 0.0d || r > 1.0d) || (s < 0.0d || s > 1.0d))
            return new UTMPos(UTMPos.zero);
        // Find intersection point
        UTMPos result = new UTMPos();
        result.x = start1.x + (r * (end1.x - start1.x));
        result.y = start1.y + (r * (end1.y - start1.y));
        result.zone = start1.zone;
        return result;
    }

    /// <summary>
    /// from http://stackoverflow.com/questions/1119451/how-to-tell-if-a-line-intersects-a-polygon-in-c
    /// </summary>
    /// <param name="start1"></param>
    /// <param name="end1"></param>
    /// <param name="start2"></param>
    /// <param name="end2"></param>
    /// <returns></returns>
    public static UTMPos FindLineIntersectionExtension(UTMPos start1, UTMPos end1, UTMPos start2, UTMPos end2)
    {
        Double denom = ((end1.x - start1.x) * (end2.y - start2.y)) - ((end1.y - start1.y) * (end2.x - start2.x));
        //  AB & CD are parallel
        if (denom == 0)
            return new UTMPos(UTMPos.zero);
        Double numer = ((start1.y - start2.y) * (end2.x - start2.x)) -
                ((start1.x - start2.x) * (end2.y - start2.y));
        Double r = numer / denom;
        Double numer2 = ((start1.y - start2.y) * (end1.x - start1.x)) -
                ((start1.x - start2.x) * (end1.y - start1.y));
        Double s = numer2 / denom;
        if ((r < 0 || r > 1) || (s < 0 || s > 1))
        {
            // line intersection is outside our lines.
        }
        // Find intersection point
        UTMPos result = new UTMPos();
        result.x = start1.x + (r * (end1.x - start1.x));
        result.y = start1.y + (r * (end1.y - start1.y));
        result.zone = start1.zone;
        return result;
    }

    private static UTMPos findClosestPoint(UTMPos start, List<UTMPos> list)
    {
        UTMPos answer = new UTMPos(UTMPos.zero);
        Double currentbest = Double.MAX_VALUE;

        for (UTMPos pnt : list)
        {
            Double dist1 = start.GetDistance(pnt);

            if (dist1.compareTo(currentbest) < 0)
            {
                answer = new UTMPos(pnt);
                currentbest = dist1;
            }
        }

        return answer;
    }

    // Add an angle while normalizing output in the range 0...360
    private static Double AddAngle(Double angle, Double degrees)
    {
        angle += degrees;

        angle = angle % 360.0;

        while (angle < 0.0)
        {
            angle += 360.0;
        }
        return angle;
    }

    private static LatLngLine findClosestLine(UTMPos start, List<LatLngLine> list, Double minDistance, Double angle)
    {
        // By now, just add 5.000 km to our lines so they are long enough to allow intersection
        Double METERS_TO_EXTEND = 5000000.0;


        Double perperndicularOrientation = AddAngle(angle, 90.0);

        // Calculation of a perpendicular line to the grid lines containing the "start" point
        /*
         *  --------------------------------------|------------------------------------------
         *  --------------------------------------|------------------------------------------
         *  -------------------------------------start---------------------------------------
         *  --------------------------------------|------------------------------------------
         *  --------------------------------------|------------------------------------------
         *  --------------------------------------|------------------------------------------
         *  --------------------------------------|------------------------------------------
         *  --------------------------------------|------------------------------------------
         */
        UTMPos start_perpendicular_line = newpos(start, perperndicularOrientation, -METERS_TO_EXTEND);
        UTMPos stop_perpendicular_line = newpos(start, perperndicularOrientation, METERS_TO_EXTEND);

        // Store one intersection point per grid line
        Map<UTMPos, LatLngLine> intersectedPoints = new HashMap<UTMPos, LatLngLine>();
        // lets order distances from every intersected point per line with the "start" point
        Map<Double, UTMPos> ordered_min_to_max = new HashMap<Double, UTMPos>();

        for (LatLngLine line : list)
        {
            // Extend line at both ends so it intersecs for sure with our perpendicular line
            UTMPos extended_line_start = newpos(line.p1, angle, -METERS_TO_EXTEND);
            UTMPos extended_line_stop = newpos(line.p2, angle, METERS_TO_EXTEND);
            // Calculate intersection point
            UTMPos p = FindLineIntersection(extended_line_start, extended_line_stop, start_perpendicular_line, stop_perpendicular_line);

            // Store it
            intersectedPoints.put(p, line);

            // Calculate distances between interesected point and "start" (i.e. line and start)
            Double distance_p = start.GetDistance(p);
            if (!ordered_min_to_max.containsKey(distance_p))
                ordered_min_to_max.put(distance_p, p);
        }

        // Acquire keys and sort them.
        List<Double> ordered_keys = new ArrayList<>();
        ordered_keys.addAll(ordered_min_to_max.keySet());
        Collections.sort(ordered_keys);

        // Lets select a line that is the closest to "start" point but "mindistance" away at least.
        // If we have only one line, return that line whatever the minDistance says
        Double key = Double.MAX_VALUE;
        int i = 0;
        while (key == Double.MAX_VALUE && i < ordered_keys.size())
        {
            if (ordered_keys.get(i) >= minDistance)
                key = ordered_keys.get(i);
            i++;
        }

        // If no line is selected (because all of them are closer than minDistance, then get the farest one
        if (key == Double.MAX_VALUE)
            key = ordered_keys.get(ordered_keys.size()-1);

        // return line
        return intersectedPoints.get(ordered_min_to_max.get(key));

    }

    private static boolean PointInPolygon(UTMPos p, List<UTMPos> poly)
    {
        UTMPos p1, p2;
        boolean inside = false;

        if (poly.size() < 3)
        {
            return inside;
        }
        UTMPos oldPoint = new UTMPos(poly.get(poly.size() - 1));

        for (int i = 0; i < poly.size(); i++)
        {

            UTMPos newPoint = new UTMPos(poly.get(i));

            if (newPoint.y > oldPoint.y)
            {
                p1 = oldPoint;
                p2 = newPoint;
            }
            else
            {
                p1 = newPoint;
                p2 = oldPoint;
            }

            if ((newPoint.y < p.y) == (p.y <= oldPoint.y)
                    && ((Double)p.x - (Double)p1.x) * (Double)(p2.y - p1.y)
                    < ((Double)p2.x - (Double)p1.x) * (Double)(p.y - p1.y))
            {
                inside = !inside;
            }
            oldPoint = newPoint;
        }
        return inside;
    }

    public enum StartPosition
    {
        Home ,
        BottomLeft ,
        TopLeft ,
        BottomRight ,
        TopRight ,
        Point
    }

    class LatLngLine{
        // start of line
        UTMPos p1;
        // end of line
        UTMPos p2;
        // used as a base for grid along line (initial setout)
        UTMPos basepnt;

    }

}

