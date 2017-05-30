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

/**
 * Created by Alfredo Barra on 26-05-2017. Pre-grade project.
 */

public class PlanArchiver {

    private List<LatLng> route;
    private Float[] heights;
    private Float[] speeds;
    private ArrayList<ArrayList<LatLng>> polygons;

    public PlanArchiver(List<LatLng> route, Float[] heights, Float[] speeds, ArrayList<ArrayList<LatLng>> polygons) {
        this.route = route;
        this.heights = heights;
        this.speeds = speeds;
        this.polygons = polygons;
    }

    public List<LatLng> getRoute() {
        return route;
    }

    public Float[] getHeights() {
        return heights;
    }

    public Float[] getSpeeds() {
        return speeds;
    }

    public ArrayList<ArrayList<LatLng>> getPolygons() {
        return polygons;
    }

    public PlanArchiver() {

    }

    public boolean loadPlan (String filePath){
        File file= new File(filePath);
        File fileDirectory = new File(file.getAbsolutePath());
        this.route = new ArrayList<LatLng>();
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
                        this.route.add(new LatLng(Double.parseDouble(parsedLine[0]),Double.parseDouble(parsedLine[1])));
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


    public boolean savePlan (List<LatLng> route, Float[] heights, Float[] speeds, ArrayList<ArrayList<LatLng>> polygons, String filePath){
        String fileName =  ".fplan";
        File writeFile = new File(filePath+fileName);
        return true;
    }


    public String savePlan (String filePath){

        Calendar c = Calendar.getInstance();
        String fileName = c.get(Calendar.DATE)+c.get(Calendar.MONTH)+ c.get(Calendar.YEAR)+c.get(Calendar.HOUR_OF_DAY)+ c.get(Calendar.MINUTE)+".fplan";
        File fileDirectory = new File(filePath);
        String fileBody = "fplan_file;"+ this.route.size()+";"+this.polygons.size()+";\n";
        //escribo waypoints
        for (LatLng point: this.route){
            fileBody = fileBody + point.latitude + "\t"+ point.longitude + "\tspeed\theight\tstyle\n";
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
