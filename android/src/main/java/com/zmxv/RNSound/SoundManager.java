package com.zmxv.RNSound;

/**
 * Created by admin on 2017/7/20.
 */

public class SoundManager {
    public interface Prepare{
        void onPrepared(Double duration);
        void onError(int what, int extra);
    }
    public interface Play{
        void onResult(boolean isOK);
    }
    public interface Time{
        void onresult(double position,boolean isPlaying);
    }
}
