package bk2suz.loomus;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Date;

/**
 * Created by sujoy on 14/9/15.
 */
public class AppOverload extends Application {
    private static final String ExternalFolderName = "Loomus";
    private static final String TempAudioFolderName = "tempAudio";
    private static final String PermaAudioFolderName = "permaAudio";
    private static final String GraphFolderName = "graph";
    private static Context sApplication;

    private static String sExternalSdPath = null;
    private static final String[] sPossibleExternalSdPaths = {
            "/mnt/extSdCard",
            "/mnt/sdcard/ext_sd",
            "/mnt/external",
            "/mnt/sdcard/external_sd"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    public static File getTempAudioFile() {
        return new File(sApplication.getDir(TempAudioFolderName, MODE_PRIVATE), String.valueOf(new Date().getTime()));
    }

    public static File getPermaDir()  {
        if(sExternalSdPath != null) return new File(sExternalSdPath);
        for(String path: sPossibleExternalSdPaths) {
            File folder=new File(new File(path), ExternalFolderName);
            if(folder.exists() || folder.mkdirs()) {
                sExternalSdPath = folder.getAbsolutePath();
                break;
            }
        }
        if(sExternalSdPath != null)  {
            return new File(sExternalSdPath);
        }
        return sApplication.getDir(PermaAudioFolderName, MODE_PRIVATE);
    }

    public static File getPermaAudioFile(String fileName) {
        return new File(getPermaDir(), fileName);
    }

    public static File getPermaAudioFile() {
        return getPermaAudioFile(String.valueOf(new Date().getTime()));
    }

    public static File getGraphFile(String fileName) {
        File graphFolder = new File(getPermaDir(), GraphFolderName);
        graphFolder.mkdirs();
        return new File(graphFolder, fileName);
    }

    public static File getExportAudioFile() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), String.valueOf(new Date().getTime() + ".wav"));
        return file;
    }

    public static File getExportAudioFile(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), filename);
        return file;
    }

    public static Context getContext() {
        return sApplication;
    }
}
