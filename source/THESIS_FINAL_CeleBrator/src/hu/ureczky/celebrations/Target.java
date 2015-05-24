package hu.ureczky.celebrations;

public enum Target {
    
    SUN(R.string.sun, R.drawable.sun),
    MOON(R.string.moon, R.drawable.moon);
    
    public int mStringResID;
    public int mIconID;
    
    Target(int stringResID, int iconID) {
        mStringResID = stringResID;
        mIconID = iconID;
    }
    
}
