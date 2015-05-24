package hu.ureczky.utils.astro.distancecalculator;

import android.util.Log;

import hu.ureczky.utils.astro.Astronomy;

/**
 * Calculates geodetic distance between two points specified by latitude/longitude using 
 * Vincenty inverse formula for ellipsoids
 * <br/>
 * <br/><b>Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2012</b>
 * <br/> <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">{@link http://www.movable-type.co.uk/scripts/latlong-vincenty.html}</a>
 * <br/><b>From:</b>
 * Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the 
 * Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    
 * <br/> <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf">{@link http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf}</a>
 */
public class DC_Ellipsoid extends DistanceCalculator {
     
    private static final String TAG = "DC_Ellipsoid";
    
    @Override
    public double getDistance() {
        
        // WGS-84 ellipsoid params:
        final double a = Astronomy.EARTH_A;
        final double b = Astronomy.EARTH_B;
        final double f = Astronomy.EARTH_F;
        
        // Calculate constants from parameters:
        final double L     = mLon2 - mLon1;
        final double U1    = Math.atan((1-f) * Math.tan(mLat1));
        final double U2    = Math.atan((1-f) * Math.tan(mLat2));
        final double sinU1 = Math.sin(U1);
        final double cosU1 = Math.cos(U1);
        final double sinU2 = Math.sin(U2);
        final double cosU2 = Math.cos(U2);
        
        // Iterative variables
        double lambda = L;
        double lambdaP;
        int    iterLimit = 100;
        double sinSigma;
        double cosSigma;
        double cosSqAlpha;
        double cos2SigmaM;
        double sigma;
        double sinAlpha;
        double C;
        double sinLambda;
        double cosLambda;
        
        //Iterations
        do {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            sinSigma  = Math.sqrt(
                            (cosU2*sinLambda) * (cosU2*sinLambda)
                          + (cosU1*sinU2-sinU1*cosU2*cosLambda) * (cosU1*sinU2-sinU1*cosU2*cosLambda)
                        );
            if (sinSigma==0) {
                Log.i(TAG, "co-incident points");
                return 0;
            }
            cosSigma   = sinU1*sinU2 + cosU1*cosU2*cosLambda;
            sigma      = Math.atan2(sinSigma, cosSigma);
            sinAlpha   = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha*sinAlpha;
            cos2SigmaM = cosSigma - 2*sinU1*sinU2/cosSqAlpha;
                         if (Double.isNaN(cos2SigmaM)) cos2SigmaM = 0.0;  // equatorial line: cosSqAlpha=0 (§6)
            C          = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
            lambdaP    = lambda;
            lambda     = L + (1-C) * f * sinAlpha *
                         (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
        } while (Math.abs(lambda-lambdaP) > 1e-12 && --iterLimit>0);

        if (iterLimit==0) {
            return tryMyHack();
        }

        //Bearings:
//      double fwdAz = Math.atan2(cosU2*sinLambda,  cosU1*sinU2-sinU1*cosU2*cosLambda);
//      double revAz = Math.atan2(cosU1*sinLambda, -sinU1*cosU2+cosU1*sinU2*cosLambda);
//      double initialBearing = Math.toDegrees(fwdAz);
//      double finalBearing   = Math.toDegrees(revAz);
        
        //Calculate result
        double uSq = cosSqAlpha * (a*a - b*b) / (b*b);
        double A   = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
        double B   = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
        double deltaSigma = B*sinSigma*(
            cos2SigmaM+B/4*(
                cosSigma*(-1+2*cos2SigmaM*cos2SigmaM) -
                B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+4*cos2SigmaM*cos2SigmaM)
            )
        );
        double dist_m = b*A*(sigma-deltaSigma);
        
        return dist_m;
    }
    
    private double tryMyHack() {
        double avgLat = (mLat1 + mLat2) / 2;
        
        double lon2_1 = normalizeLongitude(mLon2 - mLon1); // lon2-lon1, but the smallest angle. (e.g. -170 deg instead of 190 deg) 
        double avgLon = normalizeLongitude(mLon1 + lon2_1 / 2); // average of lon1 and lon2 (which is in the smaller angle, not the bigger)
        double DEG = Math.PI / 180.0;
        boolean isAntipodal = (Math.abs(lon2_1) > 179 * DEG) && (Math.abs(avgLat) < 1 * DEG);
        
        if(isAntipodal) {
            
            double lat1 = mLat1;
            double lon1 = mLon1;
            double lat2 = mLat2;
            double lon2 = mLon2;
            
            // Intermediate point
            double lat3 = avgLat;
            double lon3 = avgLon;
            
            // 1 -> 3
            mLat1 = lat1; mLon1 = lon1; mLat2 = lat3; mLon2 = lon3;
            double d1 = getDistance();
            
            // 3 -> 1
            mLat1 = lat3; mLon1 = lon3; mLat2 = lat2; mLon2 = lon2;
            double d2 = getDistance();
            Log.i(TAG, "");
            double dist_m = d1 + d2; 
            
            return dist_m;
        }
        
        Log.e(TAG, "Formula failed to converge");
        return Double.NaN;
    }
    
}
