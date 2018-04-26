package cl.usach.abarra.flightplanner.util;

import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cl.usach.abarra.flightplanner.model.FlightLine;
import cl.usach.abarra.flightplanner.model.FlightPolygon;
import cl.usach.abarra.flightplanner.model.Waypoint;

/**
 * Created by Alfredo Barra on 26-05-2017. Pre-grade project.
 */

public class PlanArchiver {

    private List<Waypoint> waypoints;
    private List<FlightLine> fLines;
    private List<FlightPolygon> fPolygons;

    public PlanArchiver() {
        this.waypoints = new ArrayList<Waypoint>();
        this.fPolygons = new ArrayList<FlightPolygon>();
        this.fLines = new ArrayList<FlightLine>();
    }



    public PlanArchiver(List<Waypoint> waypoints, List<FlightLine> lines, List<FlightPolygon> polygons) {
        this.waypoints = waypoints;
        this.fLines = lines;
        this.fPolygons = polygons;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }


    public boolean loadPlan (String filePath){
        File file= new File(filePath);
        File fileDirectory = new File(file.getAbsolutePath());
        this.waypoints = new ArrayList<Waypoint>();
        this.fLines = new ArrayList<FlightLine>();
        this.fPolygons = new ArrayList<FlightPolygon>();
        if (!fileDirectory.exists()){
            System.out.println("Directorio no existe o no fue encontrado");
            return false;
        }
        if (!file.exists()){
            System.out.println("Archivo no existe o no fue encontrado");
            return false;
        }else {
            try {//TODO: TERMINAR LECTURA PLAN
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                int points = 0;
                int polygons = 0;
                Log.d("LOAD FILE: ", file.getAbsolutePath());
                line = bufferedReader.readLine();
                if (line == "BEGIN") {
                    line = bufferedReader.readLine();
                    while (line != "END"){
                        switch (line){
                            case "BeginWp":
                                line = bufferedReader.readLine();
                                while (line != "EndWp"){

                                }
                                break;
                            case "BeginPolys":
                                line = bufferedReader.readLine();
                                while (line != "EndPolys"){

                                }
                                break;
                            case "BeginLines":
                                line = bufferedReader.readLine();
                                while (line != "EndLines"){

                                }
                                break;

                            default:
                                break;
                        }
                        line = bufferedReader.readLine();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    public boolean savePlan (List<Waypoint> waypoints, ArrayList<ArrayList<LatLng>> polygons, String filePath){
        String fileName =  ".fplan";
        File writeFile = new File(filePath+fileName);
        return true;
    }


    public String savePlan (String filePath){
        Calendar c = Calendar.getInstance();
        String fileName = String.format("%1$td%1$tm%1$tY %1$tH%1$tM%1$tL", c)+".fplan";
        File fileDirectory = new File(filePath);
        String fileBody = "fplan_file;"+ this.waypoints.size()+";"+this.fLines.size()+ ""+this.fPolygons.size()+  ";\n";
        if (!fileDirectory.exists()){
            fileDirectory.mkdirs();
        }

        fileBody+="BEGIN\n";

        //escribo waypoints
        fileBody += "BeginWp\n";
        for (Waypoint waypoint: this.waypoints){
            fileBody = fileBody + waypoint.getPosition().latitude + "\t"+ waypoint.getPosition().longitude + "\t"+ waypoint.getHeight()+"\t"+waypoint.getSpeed()+ "\t"+waypoint.getType() +"\n";
        }
        fileBody += "EndWp\n";

        //escribo lineas
        fileBody += "BeginLines\n";
        for(FlightLine flightLine : fLines){
            fileBody += "StartLine\n";
            List<LatLng> auxV = new ArrayList<>(flightLine.getVertices());
            //escribo los vertices
            for(LatLng point: auxV){
                fileBody += point.latitude + ";" + point.longitude + "\n";
            }
            fileBody += "EndLine\n";
        }
        fileBody+= "EndLines\n";

        //escribo poligonos
        fileBody += "BeginPoly\n";
        for(FlightPolygon fPoly : fPolygons){
            fileBody += "StartPoly\n";
            List<LatLng> auxV = new ArrayList<>(fPoly.getVertices());
            for(LatLng point : auxV){
                fileBody += point.latitude + ";" + point.longitude + "\n";
            }
            fileBody += "EndPoly\n";
        }
        fileBody += "EndPolys\n";


        fileBody += "END";
        fileDirectory.setWritable(true, false);
        File writeFile = new File(filePath+fileName);
        System.out.println(writeFile);
        try {
            FileOutputStream outputStream = new FileOutputStream(writeFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(fileBody);
            outputStreamWriter.flush();
            outputStream.getFD().sync();
            outputStreamWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

}
