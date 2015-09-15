package bk2suz.loomus;

import java.io.File;

/**
 * Created by sujoy on 14/9/15.
 */
public abstract class RecorderListener {
    public abstract void onRecord();
    public abstract void onPause();
    public abstract void onSave(File file);
    public abstract void onCancel();
    public abstract void onError(String message);
    public abstract void onProgress(float timeInSeconds);
}
