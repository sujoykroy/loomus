package bk2suz.loomus;

import android.graphics.Bitmap;

/**
 * Created by sujoy on 14/9/15.
 */
public abstract class PlayerListener {
    public void onPlay() {};
    public void onPause() {};
    public void onSeek() {};
    public void onProgress(float head) {};
    public void onError(String message) {};
    public void onGraphLoad(Bitmap graphBitmap) {};
}
