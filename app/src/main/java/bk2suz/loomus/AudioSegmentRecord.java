package bk2suz.loomus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sujoy on 15/9/15.
 */
public class AudioSegmentRecord {
    private static final String DbName = "audioSegment";
    private static final int DbVersion = 2;

    private static final String TableName = "segment";

    private static final String FieldRowId = "rowid";
    private static final String FieldFileName = "filename";
    private static final String FieldName = "name";
    private static final String FieldStartFrom = "startFrom";
    private static final String FieldEndTo = "endTo";

    private static final String SqlCreateSchema = "" +
            "CREATE TABLE IF NOT EXISTS " + TableName + " (" +
            FieldFileName + " TEXT UNIQUE NOT NULL," +
            FieldName + " TEXT NOT NULL," +
            FieldStartFrom + " INT NOT NULL," +
            FieldEndTo + " INT NOT NULL" +
            ")";
    private static final String SqlDropSchema = "DROP TABLE IF EXISTS " + TableName;

    private static ExecutorService sExecutorSerivice = Executors.newFixedThreadPool(1);
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private static class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Context context) {
            super(context, DbName, null, DbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SqlCreateSchema);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SqlDropSchema);
            db.execSQL(SqlCreateSchema);
        }
    }

    private long mRowId;
    private String mFileName;
    private String mName;
    private long mStartFromInByte;
    private long mEndToInByte;
    private long mLengthInByte;

    public AudioSegmentRecord(long rowId, String name, String fileName, long startFrom, long endTo) {
        mRowId = rowId;
        mName = name;
        mFileName = fileName;
        mStartFromInByte = startFrom;
        mEndToInByte = endTo;

        File file = AppOverload.getPermaAudioFile(mFileName);
        mLengthInByte = file.length();

    }

    public AudioSegmentRecord(File file) {
        mFileName = file.getName();
        mName = file.getName();
        mStartFromInByte = 0;
        mEndToInByte = file.length();
        mLengthInByte = file.length();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(AppOverload.getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if(db == null) return;
                ContentValues values = new ContentValues();
                values.put(FieldFileName, mFileName);
                values.put(FieldName, mName);
                values.put(FieldStartFrom, mStartFromInByte);
                values.put(FieldEndTo, mEndToInByte);
                mRowId = db.insert(TableName, null, values);
                db.close();
            }
        };
        sExecutorSerivice.execute(task);
    }

    public void save() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(AppOverload.getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if(db == null) return;
                ContentValues values = new ContentValues();
                values.put(FieldName, mName);
                values.put(FieldStartFrom, mStartFromInByte);
                values.put(FieldEndTo, mEndToInByte);
                db.update(TableName, values,
                        String.format("%s = ?", FieldRowId),
                        new String[] {String.valueOf(mRowId)}
                );
                db.close();
            }
        };
        sExecutorSerivice.execute(task);
    }

    public void delete() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(AppOverload.getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if(db == null) return;
                int rowCount = db.delete(TableName, String.format("%s = ?", FieldRowId),
                        new String[]{String.valueOf(mRowId)}
                );
                db.close();
                if(rowCount>0) {
                    getFile().delete();
                }
            }
        };
        sExecutorSerivice.execute(task);
    }

    public long getLengthInByte() {
        return mLengthInByte;
    }

    public long getStartFromInByte() {
        return mStartFromInByte;
    }

    public void setStartFromInByte(long value) {
        mStartFromInByte = value;
    }

    public long getEndToInByte() {
        return mEndToInByte;
    }

    public void setEndToInByte(long value) {
        mEndToInByte = value;
    }

    public String getName() {
        return mFileName;
    }

    public void setName(String name) {
        mName = name;
    }

    public File getFile() {
        return AppOverload.getPermaAudioFile(mFileName);
    }

    private static Runnable getOnRecordListLoadRunnable(
            final ArrayList<AudioSegmentRecord> recordList,
            final OnLoadListener<ArrayList<AudioSegmentRecord>> listener) {
        return new Runnable() {
            @Override
            public void run() {
                listener.onLoad(recordList);
            }
        };
    }

    public static void getRecords(final OnLoadListener<ArrayList<AudioSegmentRecord>> listener) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ArrayList<AudioSegmentRecord> recordList = new ArrayList<AudioSegmentRecord>();

                DbHelper dbHelper = new DbHelper(AppOverload.getContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                if(db != null) {
                    String[] columns = {FieldRowId, FieldName, FieldFileName, FieldStartFrom, FieldEndTo};
                    Cursor cursor = db.query(TableName, columns, null, null, null, null, null);
                    while(cursor.moveToNext()) {
                        AudioSegmentRecord record = new AudioSegmentRecord(
                                cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                                cursor.getLong(3), cursor.getLong(4)
                        );
                        recordList.add(record);
                    }
                    db.close();
                }
                if(recordList.size()==0) {
                    File[] files = AppOverload.getPermaDir().listFiles();
                    if(files != null) {
                        for(int i=files.length-1; i>=0; i--) {
                            File file = files[i];
                            ContentValues values = new ContentValues();
                            values.put(FieldFileName, file.getName());
                            values.put(FieldName, file.getName());
                            values.put(FieldStartFrom, 0);
                            values.put(FieldEndTo, file.length());

                            db = dbHelper.getWritableDatabase();
                            if(db != null) {
                                long rowId = db.insert(TableName, null, values);
                                db.close();
                                if(rowId<0) continue;
                                AudioSegmentRecord record = new AudioSegmentRecord(
                                        rowId, file.getName(), file.getName(), 0, file.length()
                                );
                                recordList.add(record);
                            }
                        }
                    }
                }
                sHandler.post(getOnRecordListLoadRunnable(recordList, listener));
            }
        };
        sExecutorSerivice.execute(task);
    }
}
