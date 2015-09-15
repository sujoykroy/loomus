package bk2suz.loomus;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sujoy on 14/9/15.
 */
public class Player implements Runnable {
    private static float VolumeGain = 0.25F/8;
    public static final int TrackBufferMultiplier = 2;

    private boolean mIsPlaying;
    private boolean mIsEnabled;
    private boolean mIsDeletable = false;

    private AudioTrack mAudioTrack;
    private int mTrackBufferSize;
    private long mPlayPeriodInMilli;

    private File mFile;
    private InputStream mInputStream = null;
    private long mCurrentPositionInByte;

    private ArrayList<PlayerListener> mPlayerListeners;
    private Handler mHandler;

    private ScheduledExecutorService mAudioWriterExecutor;
    private Runnable mOnProgressRunnable;

    private long mDurationInByte;
    private long mStartFromInByte;
    private long mEndToInByte;

    private AudioSegmentRecord mSegmentRecord;

    public Player(AudioSegmentRecord segmentRecord) throws Exception {
        Recorder.configure();

        int minBufferSize= mAudioTrack.getMinBufferSize(
                Recorder.getSampleRateInHz(),
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        mTrackBufferSize = minBufferSize * TrackBufferMultiplier;
        mPlayPeriodInMilli = (int) Math.floor(100*mTrackBufferSize/(float) Recorder.getSampleRateInHz());
        //Log.d("LOGA", String.format("mPlayPeriodInMilli=%d", mPlayPeriodInMilli));
        try {
            mAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, Recorder.getSampleRateInHz(),
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    mTrackBufferSize,
                    AudioTrack.MODE_STREAM);
        } catch (IllegalArgumentException e) {
            throw new Exception("Bad "  +e.getMessage());
        }
        /*mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume()*VolumeGain, AudioTrack.getMaxVolume()*VolumeGain);*/
        mSegmentRecord = segmentRecord;
        if(mSegmentRecord != null) {
            try {
                mInputStream = new BufferedInputStream(new FileInputStream(mSegmentRecord.getFile()));
            } catch (Exception e) {
                throw new Exception("No input file");
            }
            mInputStream.mark((int)mSegmentRecord.getLengthInByte() + 1);
            buildRegion();
        }
        mPlayerListeners = new ArrayList<PlayerListener>();
        mHandler = new Handler(Looper.getMainLooper());
        mAudioWriterExecutor = Executors.newScheduledThreadPool(1);

        mIsPlaying = false;
        mCurrentPositionInByte = 0;

        mOnProgressRunnable = new Runnable() {
            @Override
            public void run() {
                float head = (mStartFromInByte + mCurrentPositionInByte)/
                                    (float) mSegmentRecord.getLengthInByte();
                for(PlayerListener listener: mPlayerListeners) {
                    listener.onProgress(head);
                }
            }
        };

        mIsEnabled = false;
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

    public void setDurationInByte(long durationInByte) {
        if(mSegmentRecord == null) {
            mDurationInByte = durationInByte;
        }
    }

    public long getDurationInByte() {
        return mDurationInByte;
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
        if(!mIsEnabled) return;
        Runnable task = new Runnable() {
            @Override
            public void run() {
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
                mCurrentPositionInByte =  byteCount%mDurationInByte;
                try {
                    mInputStream.skip(mStartFromInByte + mCurrentPositionInByte);
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

        byte[] bytes = new byte[mTrackBufferSize];
        int readCount= 0;
        try {
            readCount = mInputStream.read(bytes, 0, mTrackBufferSize);
        } catch (IOException e) {
            mHandler.post(getOnErrorRunnable(e.getMessage()));
            return;
        }
        if(readCount>0) {
            short[] shorts = new short[readCount/2];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            short[] interleaved = new short[readCount];
            for(int i=0; i<readCount/2; i++) {
                shorts[i] = (short) (VolumeGain*shorts[i]);
                interleaved[i*2] = shorts[i];
                interleaved[i*2+1] = shorts[i];
            }
            mAudioTrack.write(interleaved, 0, readCount);
            mAudioTrack.flush();
            mHandler.post(mOnProgressRunnable);
        }
        mCurrentPositionInByte += readCount;
        if (mCurrentPositionInByte>=mDurationInByte) {
            try {
                mInputStream.reset();
                mCurrentPositionInByte = 0;
                mInputStream.skip(mStartFromInByte + mCurrentPositionInByte);
            } catch (IOException e) {
                return;
            }
        }
        mAudioWriterExecutor.schedule(this, mPlayPeriodInMilli, TimeUnit.MILLISECONDS);
    }

    public String getName() {
        return mSegmentRecord.getName();
    }

    public void setName(String name) {
        mSegmentRecord.setName(name);
        mSegmentRecord.save();
    }

    public void cleanIt() {
        mAudioTrack.release();
        mAudioWriterExecutor.shutdownNow();
        mPlayerListeners.clear();
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
        seek(((long) (head*mSegmentRecord.getLengthInByte())-mStartFromInByte));
    }

    public float getRegionLeft() {
        return mSegmentRecord.getStartFromInByte()/(float) mSegmentRecord.getLengthInByte();
    }

    public float getRegionRight() {
        return mSegmentRecord.getEndToInByte()/(float) mSegmentRecord.getLengthInByte();
    }

    public void setRegion(float left, float right, boolean save) {
        mSegmentRecord.setStartFromInByte((long) (left * mSegmentRecord.getLengthInByte()));
        mSegmentRecord.setEndToInByte((long) (right * mSegmentRecord.getLengthInByte()));
        if(save) mSegmentRecord.save();
        buildRegion();
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
}
