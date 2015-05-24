package hu.ureczky.celebrations;

import hu.ureczky.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

// Class to enable debug mode
public class DebugEnabler {
    
    // Limits: how many times should be clicked in a given time frame
    private static final int CNT_LIMIT = 10;
    private static final int TIME_LIMIT_SEC = 8;
    
    public boolean mDebugMode = false; //TODO eltarolni,beolvasani
    private List<Long> mClickTimes = new ArrayList<Long>();
    
    public DebugEnabler() {
    }
    
    // @return : debug mode just enabled
    public boolean click() {
        mClickTimes.add(System.currentTimeMillis());
        if(mClickTimes.size() > CNT_LIMIT) {
            mClickTimes.remove(0);
            long t0 = mClickTimes.get(0);
            long t1 = mClickTimes.get(mClickTimes.size()-1);
            long dt_sec = (t1 - t0) / TimeUtils.MILLISECONDS_PER_SECOND;
            if(dt_sec < TIME_LIMIT_SEC) {
                mClickTimes.clear();
                mDebugMode ^= true;
            }
        }
        return mDebugMode;
    }
    
}
