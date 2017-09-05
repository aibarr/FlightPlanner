package cl.usach.abarra.flightplanner.util;

import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cl.usach.abarra.flightplanner.model.Waypoint;

/**
 * Created by Alfredo Barra on 26-05-2017. Pre-grade project.
 */

public class PlanArchiver {

    private List<Waypoint> waypoints;
    private ArrayList<ArrayList<LatLng>> polygons;

    public PlanArchiver() {

    }

    public PlanArchiver(List<Waypoint> waypoints, ArrayList<ArrayList<LatLng>> polygons) {
        this.waypoints = waypoints;
        this.polygons = polygons;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public ArrayList<ArrayList<LatLng>> getPolygons() {
        return polygons;
    }

    public void setPolygons(ArrayList<ArrayList<LatLng>> polygons) {
        this.polygons = polygons;
    }

    public boolean loadPlan (String filePath){
        File file= new File(filePath);
        File fileDirectory = new File(file.getAbsolutePath());
        this.waypoints = new ArrayList<Waypoint>();
        this.polygons = new ArrayList<ArrayList<LatLng>>();
        if (!fileDirectory.exists()){
            System.out.println("Directorio no existe o no fue encontrado");
            return false;
        }
        if (!file.exists()){
            System.out.println("Archivo no existe o no fue encontrado");
            return false;
        }else {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                int points = 0;
                int polygons = 0;
                Log.d("LOAD FILE: ", file.getAbsolutePath());
                line = bufferedReader.readLine();
                String[] parsedLine = line.split(";");
                if (parsedLine[0].equals("fplan_file")){
                    points = Integer.parseInt(parsedLine[1]);
                    polygons = Integer.parseInt(parsedLine[2]);
                    int i;
                    for (i = 0; i< points; i++){
                        line = bufferedReader.readLine();
                        parsedLine = line.split("\t");
                        Waypoint waypoint = new Waypoint();
                        waypoint.setPosition(new LatLng(Double.parseDouble(parsedLine[0]),Double.parseDouble(parsedLine[1])));
                        waypoint.setHeight(Double.parseDouble(parsedLine[2]));
                        waypoint.setSpeed(Integer.valueOf(parsedLine[3]));
                        waypoint.setType('s');
                        waypoints.add(waypoint);
                    }
                    for (i=points; i<polygons+points;i++){
                        ArrayList<LatLng> vertices = new ArrayList<LatLng>();
                        line = bufferedReader.readLine();
                        parsedLine = line.split(";");
                        for ( String point: parsedLine){
                            String[] coords=point.split(",");
                            vertices.add(new LatLng(Double.parseDouble(coords[0]),Double.parseDouble(coords[1])));
                        }
                        this.polygons.add(vertices);
                    }
                }else {
                    return false;
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
        String fileBody = "fplan_file;"+ this.waypoints.size()+";"+this.polygons.size()+";\n";
        //escribo waypoints
        for (Waypoint waypoint: this.waypoints){
            fileBody = fileBody + waypoint.getPosition().latitude + "\t"+ waypoint.getPosition().longitude + "\t"+ waypoint.getHeight()+"\t"+waypoint.getSpeed()+ "\t"+waypoint.getType() +"\n";
        }
        if (!fileDirectory.exists()){
            fileDirectory.mkdirs();
        }
        for (ArrayList<LatLng> polygon: polygons){
            for (LatLng vertice : polygon){
                fileBody = fileBody + vertice.latitude+","+vertice.longitude+";";
            }
            fileBody = fileBody + "\n";
        }
        fileBody=fileBody+"end";
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
