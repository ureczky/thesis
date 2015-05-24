package hu.ureczky.utils.astro.geomagneticfield.wmm;

// These coefficients and the formulae used below are from:
// NOAA Technical Report: The US/UK World Magnetic Model for 2005-2010
public class WMM_2005 implements WMM {
    
    private static final int BASE_YEAR = 2005;
    
    private static final float[][] G_COEFF = new float[][] {
        {      0.0f },
        { -29556.8f, -1671.7f },
        {  -2340.6f,  3046.9f, 1657.0f },
        {   1335.4f, -2305.1f, 1246.7f,  674.0f },
        {    919.8f,   798.1f,  211.3f, -379.4f,  100.0f },
        {   -227.4f,   354.6f,  208.7f, -136.5f, -168.3f, -14.1f },
        {     73.2f,    69.7f,   76.7f, -151.2f,  -14.9f,  14.6f, -86.3f },
        {     80.1f,   -74.5f,   -1.4f,   38.5f,   12.4f,   9.5f,   5.7f,   1.8f },
        {     24.9f,     7.7f,   -11.6f,  -6.9f,  -18.2f,  10.0f,   9.2f, -11.6f, -5.2f },
        {      5.6f,     9.9f,    3.5f,   -7.0f,    5.1f, -10.8f,  -1.3f,   8.8f, -6.7f, -9.1f },
        {     -2.3f,    -6.3f,    1.6f,   -2.6f,    0.0f,   3.1f,   0.4f,   2.1f,  3.9f, -0.1f, -2.3f },
        {      2.8f,    -1.6f,   -1.7f,    1.7f,   -0.1f,   0.1f,  -0.7f,   0.7f,  1.8f,  0.0f,  1.1f,  4.1f },
        {     -2.4f,    -0.4f,    0.2f,    0.8f,   -0.3f,   1.1f,  -0.5f,   0.4f, -0.3f, -0.3f, -0.1f, -0.3f, -0.1f }
    };
    
    private static final float[][] H_COEFF = new float[][] {
        {      0.0f },
        {      0.0f,  5079.8f },
        {      0.0f, -2594.7f, -516.7f },
        {      0.0f,  -199.9f,  269.3f, -524.2f },
        {      0.0f,   281.5f, -226.0f,  145.8f, -304.7f },
        {      0.0f,    42.4f,  179.8f, -123.0f,  -19.5f, 103.6f },
        {      0.0f,   -20.3f,   54.7f,   63.6f,  -63.4f,  -0.1f,  50.4f },
        {      0.0f,   -61.5f,  -22.4f,    7.2f,   25.4f,  11.0f, -26.4f,  -5.1f },
        {      0.0f,    11.2f,  -21.0f,    9.6f,  -19.8f,  16.1f,   7.7f, -12.9f, -0.2f },
        {      0.0f,   -20.1f,   12.9f,   12.6f,   -6.7f,  -8.1f,   8.0f,   2.9f, -7.9f,  6.0f },
        {      0.0f,     2.4f,    0.2f,    4.4f,    4.8f,  -6.5f,  -1.1f,  -3.4f, -0.8f, -2.3f, -7.9f },
        {      0.0f,     0.3f,    1.2f,   -0.8f,   -2.5f,   0.9f,  -0.6f,  -2.7f, -0.9f, -1.3f, -2.0f, -1.2f },
        {      0.0f,    -0.4f,    0.3f,    2.4f,   -2.6f,   0.6f,   0.3f,   0.0f,  0.0f,  0.3f, -0.9f, -0.4f,  0.8f }
    };

    private static final float[][] DELTA_G = new float[][] {
        {      0.0f },
        {      8.0f,    10.6f },
        {    -15.1f,    -7.8f,   -0.8f },
        {      0.4f,    -2.6f,   -1.2f,   -6.5f },
        {     -2.5f,     2.8f,   -7.0f,    6.2f,   -3.8f },
        {     -2.8f,     0.7f,   -3.2f,   -1.1f,    0.1f,  -0.8f },
        {     -0.7f,     0.4f,   -0.3f,    2.3f,   -2.1f,  -0.6f,   1.4f },
        {      0.2f,    -0.1f,   -0.3f,    1.1f,    0.6f,   0.5f,  -0.4f,   0.6f },
        {      0.1f,     0.3f,   -0.4f,    0.3f,   -0.3f,   0.2f,   0.4f,  -0.7f,  0.4f },
        {      0.0f,     0.0f,    0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f },
        {      0.0f,     0.0f,    0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f },
        {      0.0f,     0.0f,    0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f,  0.0f },
        {      0.0f,     0.0f,    0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f }
    };

    private static final float[][] DELTA_H = new float[][] {
        {      0.0f },
        {      0.0f,   -20.9f },
        {      0.0f,   -23.2f,  -14.6f },
        {      0.0f,    5.0f,    -7.0f,   -0.6f },
        {      0.0f,    2.2f,     1.6f,    5.8f,    0.1f },
        {      0.0f,    0.0f,     1.7f,    2.1f,    4.8f,  -1.1f },
        {      0.0f,   -0.6f,    -1.9f,   -0.4f,   -0.5f,  -0.3f,   0.7f },
        {      0.0f,    0.6f,     0.4f,    0.2f,    0.3f,  -0.8f,  -0.2f,   0.1f },
        {      0.0f,   -0.2f,     0.1f,    0.3f,    0.4f,   0.1f,  -0.2f,   0.4f,  0.4f },
        {      0.0f,    0.0f,     0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f },
        {      0.0f,    0.0f,     0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f },
        {      0.0f,    0.0f,     0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f,  0.0f },
        {      0.0f,    0.0f,     0.0f,    0.0f,    0.0f,   0.0f,   0.0f,   0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f }
    };

    ///////////////////////////////
    // GETTERS for the constants //
    ///////////////////////////////
    
    @Override public int getBaseYear() { return BASE_YEAR; }
    @Override public float[][] getGCoeff() { return G_COEFF; }
    @Override public float[][] getHCoeff() { return H_COEFF; }
    @Override public float[][] getDeltaG() { return DELTA_G; }
    @Override public float[][] getDeltaH() { return DELTA_H; }
}
