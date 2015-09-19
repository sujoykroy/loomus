package bk2suz.loomus;

import android.graphics.Bitmap;

/**
 * Created by sujoy on 14/9/15.
 */
public abstract class PlayerListener {
    public abstract void onPlay();
    public abstract void onPause();
    public abstract void onSeek();
    public abstract void onProgress(float head);
    public abstract void onError(String message);
    public abstract void onGraphLoad(Bitmap graphBitmap);
}
