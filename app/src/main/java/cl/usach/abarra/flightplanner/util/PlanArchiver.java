package cl.usach.abarra.flightplanner.util;

import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    private List<Polygon> polygons;

    public PlanArchiver(List<LatLng> route, Float[] heights, Float[] speeds, List<Polygon> polygons) {
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

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public PlanArchiver() {

    }

    public boolean loadPlan (){
        Bundle bundle = new Bundle();
        return true;
    }


    public boolean savePlan (List<LatLng> route, Float[] heights, Float[] speeds, List<Polygon> polygons, String filePath){
        String fileName =  ".fplan";
        File writeFile = new File(filePath+fileName);
        return true;
    }


    public boolean savePlan (String filePath){

        Calendar c = Calendar.getInstance();
        String fileName = c.get(Calendar.DATE)+c.get(Calendar.MONTH)+ c.get(Calendar.YEAR)+c.get(Calendar.HOUR_OF_DAY)+ c.get(Calendar.MINUTE)+".fplan";
        File fileDirectory = new File(filePath);
        String fileBody = "fplan_file\n";
        //escribo waypoints
        for (LatLng point: this.route){
            fileBody = fileBody + point.latitude + "\t"+ point.longitude + "\tspeed\theight\tstyle\n";
        }
        if (!fileDirectory.exists()){
            fileDirectory.mkdirs();
        }
        fileBody = fileBody + "polygons\n";
        for (Polygon polygon: polygons){
            fileBody = fileBody + polygon.getPoints().toString()+"\n";
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
        return true;
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
