package hu.ureczky.utils.astro.geomagneticfield;

import android.util.Log;

import hu.ureczky.utils.TimeUtils;
import hu.ureczky.utils.astro.Astronomy;
import hu.ureczky.utils.astro.geomagneticfield.wmm.GeomagneticField_WMM;
import hu.ureczky.utils.astro.geomagneticfield.wmm.WMM;
import hu.ureczky.utils.astro.geomagneticfield.wmm.WMM_2005;
import hu.ureczky.utils.astro.geomagneticfield.wmm.WMM_2010;
import hu.ureczky.utils.astro.geomagneticfield.wmm.WMM_2015;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

public class GeomagneticFieldFactory {
        
    /**
     * Estimate the magnetic field at a given point and time.
     *
     * @param timeMillis Time at which to evaluate the declination, in milliseconds since 1970.01.01.
     *                   (approximate is fine -- the declination changes very slowly).
     */
    public static GeomagneticField create(long timeMillis) {
        
        int year = getYearFromTimeMillis(timeMillis);
                
        // Select the sufficient World Magnetic Model
        WMM wmm = (year >= 2015) ? new WMM_2015() : // for year >= 2015, use the model for 2015, which is valid until 2020
                  (year >= 2010) ? new WMM_2010() : // for year >= 2010, use the model for 2010, which is valid until 2015
                                   new WMM_2005();  // for year  < 2010, use the model for 2005, which is valid until 2010
                
        return new GeomagneticField_WMM(wmm, timeMillis);
    };
    
    private static int getYearFromTimeMillis(long timeMillis)
    {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(timeMillis);
        return gc.get(GregorianCalendar.YEAR);
    }
    
}
