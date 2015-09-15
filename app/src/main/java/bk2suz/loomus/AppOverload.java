package bk2suz.loomus;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.util.Date;

/**
 * Created by sujoy on 14/9/15.
 */
public class AppOverload extends Application {
    private static final String TempAudioFolderName = "tempAudio";
    private static final String PermaAudioFolderName = "permaAudio";
    private static Context sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    public static File getTempAudioFile() {
        return new File(sApplication.getDir(TempAudioFolderName, MODE_PRIVATE), String.valueOf(new Date().getTime()));
    }

    public static File getPermaAudioFile() {
        return new File(sApplication.getDir(PermaAudioFolderName, MODE_PRIVATE), String.valueOf(new Date().getTime()));
    }

    public static File getPermaAudioFile(String fileName) {
        return new File(sApplication.getDir(PermaAudioFolderName, MODE_PRIVATE), fileName);
    }

    public static File getPermaDir() {
        return sApplication.getDir(PermaAudioFolderName, MODE_PRIVATE);
    }

    public static Context getContext() {
        return sApplication;
    }
}
