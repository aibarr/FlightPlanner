package cl.usach.abarra.flightplanner;

import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import cl.usach.abarra.flightplanner.util.PointLatLngAlt;
import cl.usach.abarra.flightplanner.util.UTMPos;

import static org.junit.Assert.assertArrayEquals;

public class LatLngTest {
    List<Double[]> target = new ArrayList<>();
    List<PointLatLngAlt> from = new ArrayList<>();

    @Test
    public void LLAtoUTMisOK (){
        assertArrayEquals(PointLatLngAlt.ToUTM(19, from).toArray(), target.toArray());
    }


}
