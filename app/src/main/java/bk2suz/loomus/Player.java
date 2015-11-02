package bk2suz.loomus;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sujoy on 14/9/15.
 */
public class Player implements Runnable {
    public static float sVolume = 1;
    public static final int TrackBufferMultiplier = 2;
    private static ExecutorService sGraphExecutor = Executors.newFixedThreadPool(1);

    private boolean mIsPlaying;
    private boolean mIsEnabled;
    private boolean mIsDeletable = false;

    private AudioTrack mAudioTrack;
    private int mTrackBufferSize;
    private long mPlayPeriodInMilli;

    private SkipFixedInputStream mInputStream = null;
    private long mCurrentPositionInByte;

    private ArrayList<PlayerListener> mPlayerListeners;
    private ArrayList<OnRegionChangeListener> mOnRegionChangeListeners;
    private Handler mHandler;

    private ScheduledExecutorService mAudioWriterExecutor;
    private Runnable mOnProgressRunnable;

    private long mDurationInByte;
    private long mStartFromInByte;
    private long mEndToInByte;

    private float mVolume = 1;
    private float mTempo = 1f;

    private AudioSegmentRecord mSegmentRecord;

    public Player(AudioSegmentRecord segmentRecord) throws Exception {
        this(segmentRecord, true);
    }

    public Player(AudioSegmentRecord segmentRecord, boolean withTrack) throws Exception {
        Recorder.configure();

        mSegmentRecord = segmentRecord;
        if(mSegmentRecord != null) {
            if(withTrack) {
                try {
                    mInputStream = new SkipFixedInputStream(new FileInputStream(mSegmentRecord.getAudioFile()));
                } catch (Exception e) {
                    throw new Exception("No input file");
                }
                mInputStream.mark((int) mSegmentRecord.getLengthInByte() + 1);
            }
            buildRegion();
            mVolume = mSegmentRecord.getVolume();
            mTempo = mSegmentRecord.getTempo();
        }
        if(withTrack) createTrack(mTempo);

        mPlayerListeners = new ArrayList<PlayerListener>();
        mOnRegionChangeListeners = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
        if(withTrack) {
            mAudioWriterExecutor = Executors.newScheduledThreadPool(1);
        }

        mIsPlaying = false;
        mCurrentPositionInByte = 0;

        mOnProgressRunnable = new Runnable() {
            @Override
            public void run() {
                float head = mCurrentPositionInByte/(float) mDurationInByte;
                for(PlayerListener listener: mPlayerListeners) {
                    listener.onProgress(head);
                }
            }
        };

        mIsEnabled = false;
    }

    private void createTrack(float tempo) throws Exception {
        float sampleRate = Recorder.getSampleRateInHz() * tempo;
        int minBufferSize= AudioTrack.getMinBufferSize(
                (int) sampleRate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int trackBufferSize = minBufferSize * TrackBufferMultiplier;
        if (trackBufferSize%2 == 1) trackBufferSize += 1;
        AudioTrack audioTrack = null;
        try {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, (int) sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    trackBufferSize,
                    AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            throw new Exception("Bad "  +e.getMessage());
        }

        if(mAudioTrack != null) mAudioTrack.release();
        mAudioTrack = audioTrack;
        mTrackBufferSize = trackBufferSize;
        mPlayPeriodInMilli = (int) Math.floor(mTrackBufferSize*.25*1000F/sampleRate);
    }

    public AudioSegmentRecord getAudioSegmentRecord() {
        return mSegmentRecord;
    }

    public void setIsEnabled(boolean value) {
        mIsEnabled = value;
    }

    public boolean checkIsEnabled() {
        return mIsEnabled;
    }

    public void addPlayerListener(PlayerListener listener) {
        mPlayerListeners.remove(listener);
        mPlayerListeners.add(listener);
    }

    public void removePlayerListener(PlayerListener listener) {
        mPlayerListeners.remove(listener);
    }

    public void addOnRegionChangeListener(OnRegionChangeListener listener) {
        mOnRegionChangeListeners.remove(listener);
        mOnRegionChangeListeners.add(listener);
    }

    public void removeOnRegionListener(OnRegionChangeListener listener) {
        mOnRegionChangeListeners.remove(listener);
    }

    public void setDurationInByte(long durationInByte) {
        if(mSegmentRecord == null) {
            mDurationInByte = durationInByte;
        }
    }

    public long getDurationInByte() {
        return mDurationInByte;
    }

    public float getDurationInSeconds() {
        return getTemoCorrectedDurationInByte() * 0.5F / (float) Recorder.getSampleRateInHz();
    }

    public long getTemoCorrectedDurationInByte() {
        return (long) (mDurationInByte/mTempo);
    }

    private void schedulePlaying() {
        mAudioWriterExecutor.schedule(this, 0, TimeUnit.SECONDS);
    }

    private Runnable getOnPlayRunnable() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for(PlayerListener listener: mPlayerListeners) listener.onPlay();
            }
        };
        return task;
    }

    public void play() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if(!mIsEnabled) return;

                if(!mIsPlaying) {
                    mIsPlaying = true;
                    mAudioTrack.play();
                    schedulePlaying();
                }
                mHandler.post(getOnPlayRunnable());
            }
        };
        mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    private Runnable getOnPauseRunnable() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for(PlayerListener listener: mPlayerListeners) listener.onPause();
            }
        };
        return task;
    }

    public void pause() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mIsPlaying = false;
                mAudioTrack.pause();
                mHandler.post(getOnPauseRunnable());
            }
        };
        mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }


    private Runnable getOnSeekRunnable() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for(PlayerListener listener: mPlayerListeners) listener.onSeek();
            }
        };
        return task;
    }

    private Runnable getOnErrorRunnable(final String message) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for(PlayerListener listener: mPlayerListeners) listener.onError(message);
            }
        };
        return task;
    }

    public void seek(final long byteCount) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    mInputStream.reset();
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }
                mCurrentPositionInByte =  ((long)(byteCount*mTempo))%mDurationInByte;
                if(mCurrentPositionInByte%2 ==1) mCurrentPositionInByte+=1;
                try {
                    mInputStream.skipSured(mStartFromInByte + mCurrentPositionInByte);
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }
                mHandler.post(getOnSeekRunnable());
            }
        };
        mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if(!mIsPlaying) return;

        if(mSegmentRecord == null) {
            mCurrentPositionInByte += mTrackBufferSize;
            mCurrentPositionInByte %= mDurationInByte;
            mHandler.post(mOnProgressRunnable);
            return;
        }

        long startTime = new Date().getTime();
        byte[] bytes = new byte[mTrackBufferSize];
        int readCount= 0;
        try {
            readCount = mInputStream.read(bytes, 0, mTrackBufferSize);
        } catch (IOException e) {
            mHandler.post(getOnErrorRunnable(e.getMessage()));
            return;
        }
        if(readCount>0) {
            mCurrentPositionInByte += readCount;

            short[] shorts = new short[readCount/2];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            short[] interleaved = new short[readCount];
            for(int i=0; i<readCount/2; i++) {
                shorts[i] = (short) (sVolume*mVolume*shorts[i]);
                interleaved[i*2] = shorts[i];
                interleaved[i*2+1] = shorts[i];
            }
            mAudioTrack.write(interleaved, 0, readCount);
            mAudioTrack.flush();
            mHandler.post(mOnProgressRunnable);
        }
        if (mCurrentPositionInByte>=mDurationInByte) {
            try {
                mInputStream.reset();
                mCurrentPositionInByte = 0;
                mInputStream.skipSured(mStartFromInByte + mCurrentPositionInByte);
            } catch (IOException e) {
                return;
            }
        }
        long elapsedTime = new Date().getTime()-startTime;
        mAudioWriterExecutor.schedule(this, mPlayPeriodInMilli-elapsedTime, TimeUnit.MILLISECONDS);
    }

    public String getName() {
        return mSegmentRecord.getName();
    }

    public void setName(String name) {
        mSegmentRecord.setName(name);
        mSegmentRecord.save();
    }

    public void cleanIt() {
        if(mAudioTrack != null) mAudioTrack.release();
        mAudioTrack = null;
        if(mAudioWriterExecutor != null) mAudioWriterExecutor.shutdownNow();
        mAudioWriterExecutor = null;
        if(mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {}
        }
        mInputStream = null;
        mPlayerListeners.clear();
        mOnRegionChangeListeners.clear();
    }

    public boolean checkIsPlaying() {
        return mIsPlaying;
    }

    public long getCurrentPositionInByte() {
        return mCurrentPositionInByte;
    }

    public float getHead() {
        return mCurrentPositionInByte/(float) mSegmentRecord.getLengthInByte();
    }

    public void setHead(float head) {//fraction of total original audio segment..
        long byteCount = ((long) (head*mSegmentRecord.getLengthInByte())-mStartFromInByte);
        if (byteCount%2 == 1) byteCount += 1;
        seek(byteCount);
    }

    public float getRegionLeft() {
        return mSegmentRecord.getStartFromInByte()/(float) mSegmentRecord.getLengthInByte();
    }

    public float getStartFromInSeconds() {
        return mSegmentRecord.getStartFromInByte()* 0.5F / (float) Recorder.getSampleRateInHz();
    }

    public float getRegionRight() {
        return mSegmentRecord.getEndToInByte()/(float) mSegmentRecord.getLengthInByte();
    }

    public void setRegion(float left, float right, boolean save) {
        mSegmentRecord.setStartFromInByte((long) (left * mSegmentRecord.getLengthInByte()));
        mSegmentRecord.setEndToInByte((long) (right * mSegmentRecord.getLengthInByte()));
        if(save) mSegmentRecord.save();
        buildRegion();
        for(OnRegionChangeListener listener: mOnRegionChangeListeners) {
            listener.OnRegionChange(this);
        }
    }

    public float getVolume() {
        return mSegmentRecord.getVolume();
    }

    public void setVolume(float volume, boolean save) {
        mSegmentRecord.setVolume(volume);
        mVolume = mSegmentRecord.getVolume();
        if(save) mSegmentRecord.save();
    }

    public float getTempo() {
        return mSegmentRecord.getTempo();
    }

    public void setTempo(final float tempo, final boolean save) {
        if (tempo<=0) return;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if(!mIsEnabled) return;
                boolean success = true;
                try {
                    createTrack(tempo);
                } catch (Exception e) {
                    success = false;
                }
                if(success) {
                    mTempo = tempo;
                    mSegmentRecord.setTempo(tempo);
                    if (save) mSegmentRecord.save();
                }
                if(mIsPlaying) {
                    mAudioTrack.play();
                }
            }
        };
        mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    private void buildRegion() {
        mStartFromInByte = mSegmentRecord.getStartFromInByte();
        mEndToInByte = mSegmentRecord.getEndToInByte();
        mDurationInByte = mEndToInByte - mStartFromInByte;
    }

    public void toggleDeletable() {
        mIsDeletable = !mIsDeletable;
    }

    public boolean checkIsDeletable() {
        return mIsDeletable;
    }

    public void delete() {
        mSegmentRecord.delete();
        cleanIt();
    }

    public void loadGraphBitmap() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (!mSegmentRecord.getWaveGraphFile().isFile()) {
                    createAndSaveGraphImage();
                }
                if(mSegmentRecord.getWaveGraphFile().isFile()) {
                    loadBitmapFromFile();
                }
            }
        };
        sGraphExecutor.execute(task);
    }

    private void loadBitmapFromFile() {
        float width = 400F;
        float height = 100F;

        String fileAbsPath = mSegmentRecord.getWaveGraphFile().getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileAbsPath, options);
        if(options.outWidth < 0) return;

        float scaleX = options.outWidth/width;
        float scaleY = options.outHeight/height;
        float scale =  Math.max(scaleX, scaleX);

        options.inJustDecodeBounds = false;
        options.inDither = true;
        options.inSampleSize = (int) scale;

        Bitmap origBitmap = BitmapFactory.decodeFile(fileAbsPath, options);
        if(origBitmap == null) return;

        final Bitmap bitmap = Bitmap.createScaledBitmap(origBitmap, (int)width, (int)height, true);
        origBitmap = null;

        Runnable task = new Runnable() {
            @Override
            public void run() {
                for(PlayerListener listener: mPlayerListeners) listener.onGraphLoad(bitmap);
            }
        };
        mHandler.post(task);
    }

    public void createAndSaveGraphImage() {
        float width = 1000F;
        float height = 400F;

        Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        File audioFile = mSegmentRecord.getAudioFile();
        File graphFile = mSegmentRecord.getWaveGraphFile();

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
            } catch (IOException e) {
            }
        }
    }

    public static abstract class OnRegionChangeListener {
        public abstract void OnRegionChange(Player player);
    }
}
