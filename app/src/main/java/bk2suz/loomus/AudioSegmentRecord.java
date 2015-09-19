package bk2suz.loomus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

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

    private static ExecutorService sDbExecutor = Executors.newFixedThreadPool(1);
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
            //db.execSQL(SqlDropSchema);
            //db.execSQL(SqlCreateSchema);
        }
    }

    private long mRowId = -1;
    private String mFileName;
    private String mName;
    private long mStartFromInByte;
    private long mEndToInByte;
    private long mLengthInByte;

    private static String[] Columns = { FieldRowId, FieldName, FieldFileName, FieldStartFrom, FieldEndTo };

    public AudioSegmentRecord(Cursor cursor) {
        mRowId = cursor.getLong(0);
        mName = cursor.getString(1);
        mFileName = cursor.getString(2);
        mStartFromInByte = cursor.getLong(3);
        mEndToInByte = cursor.getLong(4);

        File file = AppOverload.getPermaAudioFile(mFileName);
        mLengthInByte = file.length();
    }

    public AudioSegmentRecord(File file) {
        mFileName = file.getName();
        mName = file.getName();
        mStartFromInByte = 0;
        mEndToInByte = file.length();
        mLengthInByte = file.length();

        add();
    }


    public long getLengthInByte() {
        return mLengthInByte;
    }

    public long getStartFromInByte() {
        return mStartFromInByte;
    }

    public void setStartFromInByte(long value) {
        if(value<0) value = 0;
        mStartFromInByte = value;
        if(mStartFromInByte%2 == 1) mStartFromInByte += 1;
    }

    public long getEndToInByte() {
        return mEndToInByte;
    }

    public void setEndToInByte(long value) {
        if(value>mLengthInByte) value = mLengthInByte;
        mEndToInByte = value;
        if(mEndToInByte%2 == 1) mEndToInByte += 1;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public File getAudioFile() {
        return AppOverload.getPermaAudioFile(mFileName);
    }

    public File getWaveGraphFile() {
        return AppOverload.getGraphFile(mFileName);
    }

    private void add() {
        if (mRowId != -1) return;
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
        sDbExecutor.execute(task);
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
        sDbExecutor.execute(task);
    }

    public void delete() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DbHelper dbHelper = new DbHelper(AppOverload.getContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if(db == null) return;
                int rowCount = db.delete(
                        TableName,
                        String.format("%s = ?", FieldRowId),
                        new String[]{String.valueOf(mRowId)}
                );
                db.close();
                if(rowCount>0) {
                    getAudioFile().delete();
                    getWaveGraphFile().delete();
                }
            }
        };
        sDbExecutor.execute(task);
    }

    private static Runnable getOnRecordListLoadRunnable(final ArrayList<AudioSegmentRecord> recordList, final OnLoadListener<ArrayList<AudioSegmentRecord>> listener) {
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
                    Cursor cursor = db.query(TableName, Columns, null, null, null, null, null);
                    while(cursor.moveToNext()) {
                        AudioSegmentRecord record = new AudioSegmentRecord(cursor);
                        recordList.add(record);
                    }
                    db.close();
                }
                sHandler.post(getOnRecordListLoadRunnable(recordList, listener));
            }
        };
        sDbExecutor.execute(task);
    }
}
