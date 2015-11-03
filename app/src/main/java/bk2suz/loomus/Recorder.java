package bk2suz.loomus;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sujoy on 14/9/15.
 */
public class Recorder implements Runnable {
    private static boolean sConfigured = false;
    private static int sSampleRateInHz;
    private static int sChannelConfig;
    private static int sEncoding;
    private static final int ReadSizeMultiplier = 1;
    private static final int AudioRecorderBufferMultiplier = ReadSizeMultiplier + 1;

    private boolean mIsRecording;
    private boolean mIsFinished;

    private AudioRecord mAudioRecorder;
    private int mReadSizeInBytes;

    private ScheduledExecutorService mAudioReaderExecutor;
    private long mReadPeriodInMilli;

    private File mAudioFile;
    private OutputStream mWriteStream;

    private float mMaxElapsedTime;
    private float mElapsedTime;

    private RecorderListener mRecorderListener;
    private Handler mHandler;
    private Runnable mOnProgressRunnable;

    private float mLastMaxValue;

    public Recorder(float maxElapsedTime, RecorderListener recorderListener) throws Exception {
        if(!sConfigured) configure();

        int minAudioRecorderBufferSize = AudioRecord.getMinBufferSize(
                sSampleRateInHz, sChannelConfig, sEncoding
        );
        int audioRecorderBufferSize = minAudioRecorderBufferSize * AudioRecorderBufferMultiplier;
        mReadSizeInBytes = minAudioRecorderBufferSize * ReadSizeMultiplier;
        try {
            mAudioRecorder = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    sSampleRateInHz, sChannelConfig, sEncoding,
                    audioRecorderBufferSize
            );
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if(mAudioRecorder == null || mAudioRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            throw new Exception("Bad");
        }

        mAudioFile = AppOverload.getTempAudioFile();
        try {
            mWriteStream = new BufferedOutputStream(new FileOutputStream(mAudioFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mIsRecording = false;
        mAudioReaderExecutor = Executors.newScheduledThreadPool(1);
        mReadPeriodInMilli = (long) Math.floor(1000*mReadSizeInBytes*.5 /(float) sSampleRateInHz);

        mMaxElapsedTime = maxElapsedTime;
        mElapsedTime = 0;

        mRecorderListener = recorderListener;
        mHandler = new Handler(Looper.getMainLooper());
        mOnProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) {
                    mRecorderListener.onProgress(mElapsedTime, mLastMaxValue/(float)Short.MAX_VALUE);
                }
            }
        };
    }


    private Runnable getOnErrorRunnable(final String errorMessage) {
        return new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) mRecorderListener.onError(errorMessage);
            }
        };
    }


    private Runnable getOnCancelRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) mRecorderListener.onCancel();
            }
        };
    }

    public void cancel() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.stop();
                mIsRecording = false;
                try {
                    mWriteStream.close();
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }
                mAudioReaderExecutor.shutdownNow();
                mAudioFile.delete();
                if(mRecorderListener != null) {
                    mHandler.post(getOnCancelRunnable());
                }
            }
        };
        mAudioReaderExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    private Runnable getOnSaveRunnable(final File file) {
        return new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) mRecorderListener.onSave(file);
            }
        };
    }

    public void save() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.stop();
                mIsRecording = false;
                try {
                    mWriteStream.close();
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }

                File outputFile = AppOverload.getPermaAudioFile();

                InputStream inputStream = null;
                try {
                    inputStream = new BufferedInputStream(new FileInputStream(mAudioFile));
                } catch (FileNotFoundException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }
                OutputStream outputStream = null;
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                } catch (FileNotFoundException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                    return;
                }
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int readCount;
                try {
                    while ((readCount = inputStream.read(buffer, 0, bufferSize)) > 0) {
                        outputStream.write(buffer, 0, readCount);
                    }
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                }
                try {
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                } catch (IOException e) {
                    mHandler.post(getOnErrorRunnable(e.getMessage()));
                }

                mAudioReaderExecutor.shutdownNow();
                mAudioFile.delete();

                mHandler.post(getOnSaveRunnable(outputFile));
            }
        };
        mAudioReaderExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }


    private Runnable getOnPauseRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) mRecorderListener.onPause();
            }
        };
    }

    public void pause() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.stop();
                mIsRecording = false;
                mHandler.post(getOnPauseRunnable());
            }
        };
        mAudioReaderExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    private Runnable getOnRecordRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (mRecorderListener != null) mRecorderListener.onRecord();
            }
        };
    }

    private void scheduleRecording() {
        mAudioReaderExecutor.schedule(this, 0, TimeUnit.SECONDS);
    }

    public void record() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mAudioRecorder.startRecording();
                mIsRecording = true;
                mHandler.post(getOnRecordRunnable());
                scheduleRecording();
            }
        };
        mAudioReaderExecutor.schedule(task, 0, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if(!mIsRecording) return;
        float maxValue = 0;
        long startTime = new Date().getTime();
        short[] shorts = new short[mReadSizeInBytes/2];
        int readCount = mAudioRecorder.read(shorts, 0, shorts.length);
        if(readCount>0) {
            mElapsedTime += readCount/(float) sSampleRateInHz;
            if(mMaxElapsedTime ==0 || mElapsedTime<=mMaxElapsedTime) {
                byte[] bytes = new byte[mReadSizeInBytes];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                try {
                    mWriteStream.write(bytes, 0, bytes.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for(int i=0; i<shorts.length; i++) {
                    if(maxValue<Math.abs(shorts[i])) maxValue = Math.abs(shorts[i]);
                }
            }
        }
        if(mMaxElapsedTime ==0 || mElapsedTime<mMaxElapsedTime) {
            mLastMaxValue = maxValue;
            mHandler.post(mOnProgressRunnable);
            long elapsedTime = new Date().getTime()-startTime;
            mAudioReaderExecutor.schedule(this, mReadPeriodInMilli - elapsedTime, TimeUnit.MILLISECONDS);
        } else {
            pause();
        }
    }

    public void cleanIt() {
        mRecorderListener = null;
        if(mAudioRecorder != null) mAudioRecorder.release();
    }


    public static void configure() {
        if(sConfigured) return;

        int [] sampleRates = {48000, 44100};
        int[] channelConfigs = {AudioFormat.CHANNEL_IN_MONO};
        int[] encodings = {AudioFormat.ENCODING_PCM_16BIT};

        for(int sampleRate: sampleRates) {
            for(int channelConfig: channelConfigs) {
                for(int encoding: encodings) {
                    int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding);
                    if(bufferSize >0) {
                        sChannelConfig = channelConfig;
                        sEncoding = encoding;
                        sSampleRateInHz = sampleRate;
                        sConfigured = true;
                        break;
                    }
                }
            }
        }
    }

    public static String getConfiguration() {
        int channel = 1;
        if(sChannelConfig == AudioFormat.CHANNEL_IN_STEREO) channel = 2;
        String encoding = "";
        if (sEncoding == AudioFormat.ENCODING_PCM_8BIT) encoding = "8 bit PCM";
        else if (sEncoding == AudioFormat.ENCODING_PCM_16BIT) encoding = "16 bit PCM";

        return String.format("Sample Rate=%d, Channel=%d, encoding=%s", sSampleRateInHz, channel, encoding);
    }

    public static int getSampleRateInHz() {
        return sSampleRateInHz;
    }
}
