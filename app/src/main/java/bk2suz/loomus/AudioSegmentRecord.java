package bk2suz.loomus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sujoy on 15/9/15.
 */
public class AudioSegmentRecord {
    private static final String DbName = "audioSegment";
    private static final int DbVersion = 5;

    private static final String TableName = "segment";

    private static final String FieldRowId = "rowid";
    public static final String FieldFileName = "filename";
    private static final String FieldName = "name";
    private static final String FieldStartFrom = "startFrom";
    private static final String FieldEndTo = "endTo";
    private static final String FieldVolume = "volume";
    private static final String FieldTempo = "tempo";

    private static final String SqlCreateSchema = "" +
            "CREATE TABLE IF NOT EXISTS " + TableName + " (" +
            FieldFileName + " TEXT UNIQUE NOT NULL," +
            FieldName + " TEXT NOT NULL," +
            FieldStartFrom + " INT NOT NULL," +
            FieldEndTo + " INT NOT NULL," +
            FieldVolume + " REAL NOT NULL," +
            FieldTempo + " REAL NOT NULL" +
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
            db.execSQL(SqlDropSchema);
            onCreate(db);
        }
    }

    private long mRowId = -1;
    private String mFileName;
    private String mName;
    private long mStartFromInByte;
    private long mEndToInByte;
    private long mLengthInByte;
    private float mVolume;
    private float mTempo;

    private static String[] Columns = { FieldRowId, FieldName, FieldFileName, FieldStartFrom,
                                        FieldEndTo, FieldVolume, FieldTempo };

    public AudioSegmentRecord(Cursor cursor) {
        mRowId = cursor.getLong(0);
        mName = cursor.getString(1);
        mFileName = cursor.getString(2);
        mStartFromInByte = cursor.getLong(3);
        mEndToInByte = cursor.getLong(4);
        mVolume = cursor.getFloat(5);
        mTempo = cursor.getFloat(6);
        if(mTempo>1) mTempo = 1;

        File file = AppOverload.getPermaAudioFile(mFileName);
        mLengthInByte = file.length();
    }

    public AudioSegmentRecord(File file, float tempo) {
        mFileName = file.getName();
        mName = file.getName();
        mStartFromInByte = 0;
        mEndToInByte = file.length();
        mLengthInByte = file.length();
        mVolume = 1;
        mTempo = tempo;
        add();
    }

    public boolean equals(AudioSegmentRecord record) {
        return record != null && mRowId == record.mRowId;
    }

    public long getLengthInByte() {
        return mLengthInByte;
    }

    public long getStartFromInByte() {
        return mStartFromInByte;
    }

    public float getStartFromInPercent() {
        return mStartFromInByte/(float) mLengthInByte;
    }

    public void setStartFromInByte(long value) {
        if(value<0) value = 0;
        mStartFromInByte = value;
        if(mStartFromInByte%2 == 1) mStartFromInByte += 1;
    }

    public long getEndToInByte() {
        return mEndToInByte;
    }

    public float getEndToInPercent() {
        return mEndToInByte/(float) mLengthInByte;
    }

    public void setEndToInByte(long value) {
        if(value>mLengthInByte) value = mLengthInByte;
        mEndToInByte = value;
        if(mEndToInByte%2 == 1) mEndToInByte += 1;
    }

    public float getDurationInSeconds() {
        return getTemoCorrectedDurationInByte() * 0.5F / (float) Recorder.getSampleRateInHz();
    }

    public long getDurationInByte() {
        return mEndToInByte-mStartFromInByte;
    }
    public long getTemoCorrectedDurationInByte() {
        return (long) ((mEndToInByte-mStartFromInByte)/mTempo);
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

    public float getVolume() {
        return mVolume;
    }

    public void setVolume(float volume) {
        mVolume = volume;
        if(mVolume<0) mVolume = 0F;
    }

    public float getTempo() {
        return mTempo;
    }

    public void setTempo(float tempo) {
        mTempo = tempo;
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
                values.put(FieldVolume, mVolume);
                values.put(FieldTempo, mTempo);
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
                values.put(FieldVolume, mVolume);
                values.put(FieldTempo, mTempo);
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

    public void saveWaveGraph() {
        float width = 1000F;
        float height = 400F;

        Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        File audioFile = getAudioFile();
        File graphFile = getWaveGraphFile();

        float pixelToAmplitude = (float) Math.floor((Short.MAX_VALUE-Short.MIN_VALUE)/height);
        if(pixelToAmplitude==0F) pixelToAmplitude = 1F;//safety
        float amplitudeToPixel = 1F/pixelToAmplitude;

        float pixelForZeroAmplitude = (Short.MAX_VALUE-Short.MIN_VALUE)*0.5F*amplitudeToPixel;

        int pixelToByteCount = (int) Math.floor(audioFile.length()/width);
        if(pixelToByteCount%2 == 1) pixelToByteCount += 1;
        if(pixelToByteCount == 0) pixelToByteCount = 2;//safetey

        int byteByfferSize = pixelToByteCount;
        int maxByteBufferSize = 1024;
        if(byteByfferSize>maxByteBufferSize) {
            int mult = (int) Math.ceil(pixelToByteCount/(float) maxByteBufferSize);
            byteByfferSize = (int) (pixelToByteCount/mult);
            if(byteByfferSize%2 == 1) byteByfferSize += 1;//2's multiple
            pixelToByteCount = byteByfferSize*mult;
        }

        InputStream audioStream = null;
        try {
            audioStream = new BufferedInputStream(new FileInputStream(audioFile));
        } catch (Exception e) {
            return;
        }

        int cumulativeByteReadCount = 0;
        int byteReadCount;
        byte[] bytes = new byte[byteByfferSize];

        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#ccca23"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        float x = 0;
        int count = 0;
        StringBuilder bl = new StringBuilder();
        while(true) {
            try {
                byteReadCount = audioStream.read(bytes, 0, byteByfferSize);
            } catch (IOException e) {
                break;
            }
            if(byteReadCount<=0) break;

            if (cumulativeByteReadCount == 0) {
                count ++;
                short[] shorts = new short[byteReadCount / 2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                canvas.drawLine(x, pixelForZeroAmplitude, x, pixelForZeroAmplitude + shorts[0]*amplitudeToPixel, paint);
                x += 1F;
            }
            cumulativeByteReadCount += byteReadCount;
            if(cumulativeByteReadCount>=pixelToByteCount) cumulativeByteReadCount %= pixelToByteCount;
        }
        try {
            audioStream.close();
        } catch (IOException e) {}

        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(graphFile));
        } catch (FileNotFoundException e) {}

        if(os != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            try {
                os.flush();
                os.close();
            } catch (IOException e) {}
        }
    }

    public Bitmap getWaveGraphBitmap() {
        if (getWaveGraphFile().isFile()) {
            saveWaveGraph();
        }

        float width = 400F;
        float height = 100F;

        String fileAbsPath = getWaveGraphFile().getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileAbsPath, options);
        if(options.outWidth < 0) return null;

        float scaleX = options.outWidth/width;
        float scaleY = options.outHeight/height;
        float scale =  Math.max(scaleX, scaleX);

        options.inJustDecodeBounds = false;
        options.inDither = true;
        options.inSampleSize = (int) scale;

        Bitmap origBitmap = BitmapFactory.decodeFile(fileAbsPath, options);
        if(origBitmap == null) return null;

        Bitmap bitmap = Bitmap.createScaledBitmap(origBitmap, (int)width, (int)height, true);
        return bitmap;
    }

    private static Runnable getOnRecordListLoadRunnable(final ArrayList<AudioSegmentRecord> recordList,
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
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                if(db != null) {
                    String orderBy = String.format("%s DESC", FieldRowId);
                    Cursor cursor = db.query(TableName, Columns, null, null, null, null, orderBy);
                    while(cursor.moveToNext()) {
                        AudioSegmentRecord record = new AudioSegmentRecord(cursor);
                        recordList.add(record);
                    }
                    db.close();
                }
                if(recordList.size() == 0) {
                    File[] files = AppOverload.getPermaDir().listFiles();
                    if(files != null) {
                        for(File file: files) {
                            AudioSegmentRecord record= new AudioSegmentRecord(file, 1);
                            recordList.add(record);
                        }
                    }
                }
                sHandler.post(getOnRecordListLoadRunnable(recordList, listener));
            }
        };
        sDbExecutor.execute(task);
    }
}
