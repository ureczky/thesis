/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.ureczky.utils.astro.geomagneticfield.wmm;

import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;

import java.util.GregorianCalendar;

/**
 * Estimates magnetic field at a given point on Earth, and in particular,
 * to compute the magnetic declination from true north.
 *
 * <p>This uses the World Magnetic Model produced by the United States National Geospatial-Intelligence Agency.
 * More details about the model can be found at
 * <a href="http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml">http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml</a>.
 * This class currently uses WMM-(BASE_YEAR) which is valid until (BASE_YEAR+5),
 * but should produce acceptable results for several years after that.
 * Future versions of Android may use a newer version of the model.
 */
public class GeomagneticField_WMM implements GeomagneticField {
    
    private long mTimeStamp;
    
    // The magnetic field at a given point, in nonoteslas in geodetic coordinates.
    private double mX;
    private double mY;
    private double mZ;

    // Geocentric coordinates -- set by computeGeocentricCoordinates.
    private double mGcLatitudeRad;
    private double mGcLongitudeRad;
    private double mGcRadiusKm;

    // Constants from WGS84 (the coordinate system used by GPS)
    static private final double EARTH_SEMI_MAJOR_AXIS_KM = 6378.137;
    static private final double EARTH_SEMI_MINOR_AXIS_KM = 6356.7523142;
    static private final double EARTH_REFERENCE_RADIUS_KM = 6371.2;

    // These coefficients and the formulae used below are from:
    // NOAA Technical Report: The US/UK World Magnetic Model for (BASE_YEAR)-(BASE_YEAR+5)
    private final int BASE_YEAR;
    private final float[][] G_COEFF;
    private final float[][] H_COEFF;
    private final float[][] DELTA_G;
    private final float[][] DELTA_H;
    private final long BASE_TIME;
    private final int MAX_N; // Maximum degree of the coefficients.

    // The ratio between the Gauss-normalized associated Legendre functions and the Schmidt quasi-normalized ones.
    // Compute these once staticly since they don't depend on input variables at all.
    private final double[][] SCHMIDT_QUASI_NORM_FACTORS;

    public GeomagneticField_WMM(WMM wmm, long timeStamp) {
        
        BASE_YEAR = wmm.getBaseYear();
        G_COEFF = wmm.getGCoeff();
        H_COEFF = wmm.getHCoeff();
        DELTA_G = wmm.getDeltaG();
        DELTA_H = wmm.getDeltaH();
        
        MAX_N = G_COEFF.length; // Maximum degree of the coefficients.
        assert G_COEFF.length == H_COEFF.length;
        assert H_COEFF.length == DELTA_G.length;
        assert DELTA_G.length == DELTA_H.length;
        
        SCHMIDT_QUASI_NORM_FACTORS = computeSchmidtQuasiNormFactors(MAX_N);
        BASE_TIME = new GregorianCalendar(BASE_YEAR, 1, 1).getTimeInMillis();
        
        mTimeStamp = timeStamp;
    }
    
    /**
     * @param gdLatitudeDeg  Latitude  in WGS84 geodetic coordinates -- positive is east.
     * @param gdLongitudeDeg Longitude in WGS84 geodetic coordinates -- positive is north.
     * @param altitudeMeters Altitude  in WGS84 geodetic coordinates, in meters.
     */
    public void setParameters(
            float gdLatitudeDeg,
            float gdLongitudeDeg,
            float altitudeMeters
    ) {
        // We don't handle the north and south poles correctly --
        // Pretend that we're not quite at them to avoid crashing.
        gdLatitudeDeg = Math.min(90.0f - 1e-5f, Math.max(-90.0f + 1e-5f, gdLatitudeDeg));
        computeGeocentricCoordinates(gdLatitudeDeg, gdLongitudeDeg, altitudeMeters);
        
        // Note: LegendreTable computes associated Legendre functions for
        // cos(theta).  We want the associated Legendre functions for
        // sin(latitude), which is the same as cos(PI/2 - latitude), except the
        // derivate will be negated.
        LegendreTable legendre = new LegendreTable(MAX_N - 1, (Math.PI / 2.0 - mGcLatitudeRad));

        // Compute a table of (EARTH_REFERENCE_RADIUS_KM / radius)^n for i in
        // 0..MAX_N-2 (this is much faster than calling Math.pow MAX_N+1 times).
        double[] relativeRadiusPower = new double[MAX_N + 2];
        relativeRadiusPower[0] = 1.0;
        relativeRadiusPower[1] = EARTH_REFERENCE_RADIUS_KM / mGcRadiusKm;
        for (int i = 2; i < relativeRadiusPower.length; ++i) {
            relativeRadiusPower[i] = relativeRadiusPower[i - 1] * relativeRadiusPower[1];
        }

        // Compute tables of sin(lon * m) and cos(lon * m) for m = 0..MAX_N --
        // this is much faster than calling Math.sin and Math.com MAX_N+1 times.
        double[] sinMLon = new double[MAX_N];
        double[] cosMLon = new double[MAX_N];
        sinMLon[0] = 0.0;
        cosMLon[0] = 1.0;
        sinMLon[1] = Math.sin(mGcLongitudeRad);
        cosMLon[1] = Math.cos(mGcLongitudeRad);

        for (int m = 2; m < MAX_N; ++m) {
            // Standard expansions for sin((m-x)*theta + x*theta) and
            // cos((m-x)*theta + x*theta).
            int x = m >> 1;
            sinMLon[m] = sinMLon[m-x] * cosMLon[x] + cosMLon[m-x] * sinMLon[x];
            cosMLon[m] = cosMLon[m-x] * cosMLon[x] - sinMLon[m-x] * sinMLon[x];
        }

        double inverseCosLatitude = 1.0 / Math.cos(mGcLatitudeRad);
        double yearsSinceBase = (mTimeStamp - BASE_TIME) / (365f * 24f * 60f * 60f * 1000f);

        // We now compute the magnetic field strength given the geocentric
        // location. The magnetic field is the derivative of the potential
        // function defined by the model. See NOAA Technical Report: The US/UK
        // World Magnetic Model for 2010-2015 for the derivation.
        double gcX = 0.0;  // Geocentric northwards component.
        double gcY = 0.0;  // Geocentric eastwards component.
        double gcZ = 0.0;  // Geocentric downwards component.

        for (int n = 1; n < MAX_N; n++) {
            for (int m = 0; m <= n; m++) {
                // Adjust the coefficients for the current date.
                double g = G_COEFF[n][m] + yearsSinceBase * DELTA_G[n][m];
                double h = H_COEFF[n][m] + yearsSinceBase * DELTA_H[n][m];

                // Negative derivative with respect to latitude, divided by
                // radius.  This looks like the negation of the version in the
                // NOAA Techincal report because that report used
                // P_n^m(sin(theta)) and we use P_n^m(cos(90 - theta)), so the
                // derivative with respect to theta is negated.
                gcX += relativeRadiusPower[n+2]
                    * (g * cosMLon[m] + h * sinMLon[m])
                    * legendre.mPDeriv[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m];

                // Negative derivative with respect to longitude, divided by
                // radius.
                gcY += relativeRadiusPower[n+2] * m
                    * (g * sinMLon[m] - h * cosMLon[m])
                    * legendre.mP[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m]
                    * inverseCosLatitude;

                // Negative derivative with respect to radius.
                gcZ -= (n + 1) * relativeRadiusPower[n+2]
                    * (g * cosMLon[m] + h * sinMLon[m])
                    * legendre.mP[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m];
            }
        }

        // Convert back to geodetic coordinates.  This is basically just a
        // rotation around the Y-axis by the difference in latitudes between the
        // geocentric frame and the geodetic frame.
        double latDiffRad = Math.toRadians(gdLatitudeDeg) - mGcLatitudeRad;
        mX = (+ gcX * Math.cos(latDiffRad)
              + gcZ * Math.sin(latDiffRad));
        mY = gcY;
        mZ = (- gcX * Math.sin(latDiffRad)
              + gcZ * Math.cos(latDiffRad));
    }

    /** @return The X (northward) component of the magnetic field in nanoteslas. */
    @Override
    public float getX() {
        return (float) mX;
    }

    /** @return The Y (eastward) component of the magnetic field in nanoteslas. */
    @Override
    public float getY() {
        return (float) mY;
    }

    /** @return The Z (downward) component of the magnetic field in nanoteslas. */
    @Override
    public float getZ() {
        return (float) mZ;
    }

    /**
     * @return The declination of the horizontal component of the magnetic
     *         field from true north, in degrees (i.e. positive means the
     *         magnetic field is rotated east that much from true north).
     */
    @Override
    public float getDeclination() {
        return (float) Math.toDegrees(Math.atan2(mY, mX));
    }

    /**
     * @return The inclination of the magnetic field in degrees -- positive
     *         means the magnetic field is rotated downwards.
     */
    @Override
    public float getInclination() {
        return (float) Math.toDegrees(Math.atan2(mZ, getHorizontalStrength()));
    }

    /** @return  Horizontal component of the field strength in nanoteslas. */
    @Override
    public float getHorizontalStrength() {
        return (float) Math.sqrt(mX * mX + mY * mY);
    }

    /** @return  Total field strength in nanoteslas. */
    @Override
    public float getFieldStrength() {
        return (float) Math.sqrt(mX * mX + mY * mY + mZ * mZ);
    }

    /**
     * @param gdLatitudeDeg  Latitude  in WGS84 geodetic coordinates.
     * @param gdLongitudeDeg Longitude in WGS84 geodetic coordinates.
     * @param altitudeMeters Altitude above sea level in WGS84 geodetic coordinates.
     * @return Geocentric latitude (i.e. angle between closest point on the equator and this point, at the center of the earth.
     */
    private void computeGeocentricCoordinates(double gdLatitudeDeg, double gdLongitudeDeg, double altitudeMeters) {
        double altitudeKm = altitudeMeters / 1000.0f;
        double a2 = EARTH_SEMI_MAJOR_AXIS_KM * EARTH_SEMI_MAJOR_AXIS_KM;
        double b2 = EARTH_SEMI_MINOR_AXIS_KM * EARTH_SEMI_MINOR_AXIS_KM;
        double gdLatRad = Math.toRadians(gdLatitudeDeg);
        double clat = Math.cos(gdLatRad);
        double slat = Math.sin(gdLatRad);
        double tlat = slat / clat;
        double latRad = Math.sqrt(a2 * clat * clat + b2 * slat * slat);

        mGcLatitudeRad = Math.atan(tlat * (latRad * altitudeKm + b2) / (latRad * altitudeKm + a2));
        mGcLongitudeRad = Math.toRadians(gdLongitudeDeg);

        double radSq = altitudeKm * altitudeKm
            + 2 * altitudeKm * Math.sqrt(a2 * clat * clat + b2 * slat * slat)
            + (a2 * a2 * clat * clat + b2 * b2 * slat * slat)
            / (a2 * clat * clat + b2 * slat * slat);
        mGcRadiusKm = Math.sqrt(radSq);
    }


    /** Utility class to compute a table of Gauss-normalized associated Legendre functions P_n^m(cos(theta)) */
    static private class LegendreTable {
        // These are the Gauss-normalized associated Legendre functions --
        // that is, they are normal Legendre functions multiplied by
        // (n-m)!/(2n-1)!! (where (2n-1)!! = 1*3*5*...*2n-1)
        public final double[][] mP;

        // Derivative of mP, with respect to theta.
        public final double[][] mPDeriv;

        /**
         * @param maxN     The maximum n- and m-values to support
         * @param thetaRad Returned functions will be Gauss-normalized
         *                 P_n^m(cos(thetaRad)), with thetaRad in radians.
         */
        public LegendreTable(int maxN, double thetaRad) {
            // Compute the table of Gauss-normalized associated Legendre
            // functions using standard recursion relations. Also compute the
            // table of derivatives using the derivative of the recursion relations.
            double cos = Math.cos(thetaRad);
            double sin = Math.sin(thetaRad);

            mP = new double[maxN + 1][];
            mPDeriv = new double[maxN + 1][];
            mP[0] = new double[] { 1.0 };
            mPDeriv[0] = new double[] { 0.0 };
            for (int n = 1; n <= maxN; n++) {
                mP[n] = new double[n + 1];
                mPDeriv[n] = new double[n + 1];
                for (int m = 0; m <= n; m++) {
                    if (n == m) {
                        mP[n][m] = sin * mP[n - 1][m - 1];
                        mPDeriv[n][m] = cos * mP[n - 1][m - 1] + sin * mPDeriv[n - 1][m - 1];
                    } else if (n == 1 || m == n - 1) {
                        mP[n][m] = cos * mP[n - 1][m];
                        mPDeriv[n][m] = -sin * mP[n - 1][m] + cos * mPDeriv[n - 1][m];
                    } else {
                        assert n > 1 && m < n - 1;
                        double k = ((n - 1) * (n - 1) - m * m) / (double) ((2 * n - 1) * (2 * n - 3));
                        mP[n][m] = cos * mP[n - 1][m] - k * mP[n - 2][m];
                        mPDeriv[n][m] = -sin * mP[n - 1][m] + cos * mPDeriv[n - 1][m] - k * mPDeriv[n - 2][m];
                    }
                }
            }
        }
    }

    /**
     * Compute the ration between the Gauss-normalized associated Legendre
     * functions and the Schmidt quasi-normalized version. This is equivalent to
     * sqrt((m==0?1:2)*(n-m)!/(n+m!))*(2n-1)!!/(n-m)!
     */
    private static double[][] computeSchmidtQuasiNormFactors(int maxN) {
        double[][] schmidtQuasiNorm = new double[maxN + 1][];
        schmidtQuasiNorm[0] = new double[] { 1.0f };
        for (int n = 1; n <= maxN; n++) {
            schmidtQuasiNorm[n] = new double[n + 1];
            schmidtQuasiNorm[n][0] = schmidtQuasiNorm[n - 1][0] * (2 * n - 1) / (double) n;
            for (int m = 1; m <= n; m++) {
                schmidtQuasiNorm[n][m] = schmidtQuasiNorm[n][m - 1]
                    * Math.sqrt((n - m + 1) * (m == 1 ? 2 : 1) / (double) (n + m));
            }
        }
        return schmidtQuasiNorm;
    }

}
