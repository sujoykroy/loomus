package bk2suz.loomus;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sujoy on 30/10/15.
 */
public class SequencerActivity extends AppCompatActivity {
    private ListView mSequenceItemListView;
    private EditText mEdtSequenceDurationInSeconds;
    private EditText mEdtSequenceDurationInIndices;

    private float mSequenceDurationInSeconds = 1;
    private float mSequenceDurationInIndices = 8;
    private long mSequenceDurationInBytes;
    private float mSecondsPerIndex;
    private float mBytesPerIndex;

    private SeekBar mSkbTimeline;

    private View mBtnPlay;
    private View mBtnPause;
    private View mBtnCancel;
    private View mBtnSave;
    private View mBtnAdd;
    private View mLockScreen;
    private ProgressBar mPgrProcessing;

    private SequencerItemListAdapter mSequencerItemListAdapter;
    private AudioSegmentListAdapter mAudioSegmentListAdapter;

    private boolean mProcessing = false;
    private ExecutorService mExecutor;
    private Handler mHandler;

    private SequencerItem mSequenceTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sequencer);

        mSequenceTracker = new SequencerItem(null);

        mSequenceItemListView = (ListView) findViewById(R.id.trackListView);
        AudioSegmentRecord.getRecords(new OnLoadListener<ArrayList<AudioSegmentRecord>>() {
            @Override
            public void onLoad(ArrayList<AudioSegmentRecord> recordList) {
                populateAudioSegments(recordList);
            }
        });

        mSkbTimeline = (SeekBar) findViewById(R.id.skbTimeline);
        mSkbTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) return;
                long bytePos = (long) (progress*.01*mSequenceDurationInBytes);
                if(bytePos%2==1) bytePos -= 1;
                if(bytePos<0) bytePos = 0;
                mSequenceTracker.seek(bytePos);
                for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
                    sequenceItem.seek(bytePos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mSequenceTracker.addSequenceListener(new SequencerItemListener() {
            @Override
            public void onEvent(SequencerItem sequenceItem) {
                int progress = (int) (100 * sequenceItem.mCurrentBytePos / (float) sequenceItem.mDurationByteCount);
                mSkbTimeline.setProgress(progress);
            }
        });

        mEdtSequenceDurationInSeconds = (EditText) findViewById(R.id.edtSeqDurSecs);
        mEdtSequenceDurationInSeconds.setText(String.valueOf(mSequenceDurationInSeconds));
        mEdtSequenceDurationInSeconds.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mSequenceDurationInSeconds = Float.parseFloat(s.toString());
                } catch (NumberFormatException e) {
                    mEdtSequenceDurationInSeconds.setText(String.valueOf(mSequenceDurationInSeconds));
                    return;
                }
                reformat();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mEdtSequenceDurationInIndices = (EditText) findViewById(R.id.edtSeqDurIndices);
        mEdtSequenceDurationInIndices.setText(String.valueOf(mSequenceDurationInIndices));
        mEdtSequenceDurationInIndices.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mSequenceDurationInIndices = Float.parseFloat(s.toString());
                } catch (NumberFormatException e) {
                    mEdtSequenceDurationInIndices.setText(String.valueOf(mSequenceDurationInIndices));
                    return;
                }
                reformat();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mBtnPlay = findViewById(R.id.imbPlay);
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
                    sequenceItem.seek(mSequenceTracker.mCurrentBytePos);
                }

                mSequenceTracker.play();
                for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
                    sequenceItem.play();
                }
                mBtnPlay.setVisibility(View.GONE);
                mBtnPause.setVisibility(View.VISIBLE);
            }
        });

        mBtnPause = findViewById(R.id.imbPause);
        mBtnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSequenceTracker.pause();
                for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
                    sequenceItem.pause();
                }
                mBtnPlay.setVisibility(View.VISIBLE);
                mBtnPause.setVisibility(View.GONE);
            }
        });
        mBtnPause.setVisibility(View.GONE);

        mBtnCancel = findViewById(R.id.imbCancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnSave = findViewById(R.id.imbSave);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        mBtnAdd = findViewById(R.id.imbAdd);
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewRow();
            }
        });

        mLockScreen = findViewById(R.id.imvLock);
        mPgrProcessing = (ProgressBar) findViewById(R.id.pgrProcessing);

        mLockScreen.setVisibility(View.GONE);
        mPgrProcessing.setVisibility(View.GONE);

        mLockScreen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void save() {
        if(mSequencerItemListAdapter.getCount() ==0) return;
        mLockScreen.setVisibility(View.VISIBLE);
        mPgrProcessing.setVisibility(View.VISIBLE);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                stitchTogether();
            }
        });
    }

    private void reformat() {
        long byteCount = (long) (2 * mSequenceDurationInSeconds*Recorder.getSampleRateInHz());
        if(byteCount%2==1) byteCount -= 1;
        if(byteCount<2) byteCount = 2;
        mSequenceDurationInBytes = byteCount;
        mSecondsPerIndex = mSequenceDurationInSeconds/mSequenceDurationInIndices;
        mBytesPerIndex = mSequenceDurationInBytes/mSequenceDurationInIndices;

        mSequenceTracker.mDurationIndex = mSequenceDurationInIndices;
        mSequenceTracker.mDurationByteCount = mSequenceDurationInBytes;
        mSequenceTracker.mStartIndex = 0;
        mSequenceTracker.mEndIndex = mSequenceDurationInIndices;
        mSequenceTracker.mStartBytePos = 0;
        mSequenceTracker.mEndBytePos = mSequenceDurationInBytes;
        for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
            sequenceItem.reformat();
        }
        mSequencerItemListAdapter.notifyDataSetChanged();
    }

    private Runnable getFinishRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };
    }

    private void stitchTogether() {
        float sampleRate = Recorder.getSampleRateInHz();

        File outputFile = AppOverload.getPermaAudioFile();
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            setResult(RESULT_CANCELED);
            mHandler.post(getFinishRunnable());
            return;
        }

        int itemCount = 0;
        for(int i=0; i<mSequencerItemListAdapter.getCount(); i++) {
            SequencerItem sequencerItem = mSequencerItemListAdapter.getItem(i);
            if(sequencerItem.mAudioSegmentRecord == null) continue;
            sequencerItem.mCurrentBytePos = 0;
            sequencerItem.resetStream();
            itemCount++;
        }

        if(itemCount == 0) {
            mHandler.post(getFinishRunnable());
            return;
        }

        int maxBufferSize = 1024;
        int byteBufferSize, shortBufferSize;

        int p = 0;
        while(p<mSequenceDurationInBytes) {
            if(maxBufferSize>(mSequenceDurationInBytes-p)) {
                byteBufferSize = (int) (mSequenceDurationInBytes-p);
            } else {
                byteBufferSize = maxBufferSize;
            }
            if(byteBufferSize<=0) byteBufferSize = 2;//safety
            shortBufferSize = byteBufferSize/2;

            byte[] readBytes = new byte[byteBufferSize];
            short[] readShorts = new short[byteBufferSize/2];

            byte[] writeBytes = new byte[byteBufferSize];
            short[] writeShorts = new short[byteBufferSize/2];
            Arrays.fill(writeShorts, (short) 0);

            for(SequencerItem si: mSequencerItemListAdapter.mSequencerItemList) {
                if(si.mAudioSegmentRecord == null) continue;
                int siWriteCount = 0;
                int zeroCount = 0;
                Arrays.fill(readBytes, (byte) 0);
                while(siWriteCount<byteBufferSize && zeroCount<2) {
                    if (si.mCurrentBytePos < si.mStartBytePos && si.mCurrentBytePos + byteBufferSize > si.mStartBytePos) {
                        siWriteCount += si.mStartBytePos - si.mCurrentBytePos;
                        si.mCurrentBytePos = si.mStartBytePos;
                    }
                    if (si.mCurrentBytePos >= si.mStartBytePos && si.mCurrentBytePos < si.mEndBytePos) {
                        if(si.mStreamCurrentBytePos>=si.mAudioSegmentRecord.getEndToInByte()) {
                            si.resetStream();
                        }

                        int streamBufferSize = (int) ((byteBufferSize-siWriteCount)*si.mTempo);
                        streamBufferSize = (int) Math.min(si.mAudioSegmentRecord.getDurationInByte(), streamBufferSize);
                        streamBufferSize = (int) Math.min(si.mAudioSegmentRecord.getEndToInByte() - si.mStreamCurrentBytePos, streamBufferSize);

                        if(streamBufferSize%2==1) streamBufferSize -= 1;
                        if(streamBufferSize<2) streamBufferSize = 2;

                        byte[] streamByteBuffer = new byte[streamBufferSize];
                        int streamReadCount = 0;
                        try {
                            streamReadCount = si.mInputStream.read(streamByteBuffer, 0, streamBufferSize);
                        } catch (IOException e) {
                            break;
                        }
                        si.mStreamCurrentBytePos += streamReadCount;
                        for(float i=0; i<streamBufferSize; i+= si.mTempo) {
                            if(siWriteCount>=byteBufferSize) break;
                            int pos = (int) Math.floor(i);
                            readBytes[siWriteCount] = streamByteBuffer[pos];
                            siWriteCount++;
                            si.mCurrentBytePos++;
                        }
                        if(siWriteCount%2 == 1 && siWriteCount<byteBufferSize) {
                            siWriteCount += 1;
                            si.mCurrentBytePos++;
                        }
                        zeroCount = streamReadCount == 0 ? zeroCount + 1:  0;
                    } else {
                        si.mCurrentBytePos += byteBufferSize - siWriteCount;
                        siWriteCount = byteBufferSize;
                    }
                }
                ByteBuffer.wrap(readBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(readShorts);
                for(int i=0; i<readShorts.length; i++) {
                    writeShorts[i] += readShorts[i]*si.mVolume;
                }
            }

            ByteBuffer.wrap(writeBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(writeShorts);
            try {
                outputStream.write(writeBytes, 0, writeBytes.length);
                outputStream.flush();
            } catch (IOException e) {
                break;
            }
            p += byteBufferSize;
        }

        for(SequencerItem si: mSequencerItemListAdapter.mSequencerItemList) {
            if(si.mAudioSegmentRecord == null) continue;
            si.mCurrentBytePos = 0;
            si.resetStream();
        }

        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            setResult(RESULT_CANCELED);
            mHandler.post(getFinishRunnable());
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(AudioSegmentRecord.FieldFileName, outputFile.getAbsolutePath());
        setResult(RESULT_OK, intent);
        mHandler.post(getFinishRunnable());
    }

    @Override
    protected void onDestroy() {
        if(mSequencerItemListAdapter != null) {
            for(SequencerItem sequenceItem: mSequencerItemListAdapter.mSequencerItemList) {
                sequenceItem.clear();
            }
            mSequenceTracker.clear();
        }
        mSequencerItemListAdapter = null;
        mSequenceTracker = null;
        mAudioSegmentListAdapter = null;
        if(mExecutor != null) mExecutor.shutdownNow();
        mExecutor = null;
        super.onDestroy();
    }

    private void populateAudioSegments(ArrayList<AudioSegmentRecord> recordList) {
        mAudioSegmentListAdapter = new AudioSegmentListAdapter(this, recordList);
        mSequencerItemListAdapter = new SequencerItemListAdapter(this);
        reformat();
        addNewRow();
        mSequenceItemListView.setAdapter(mSequencerItemListAdapter);
    }

    private void addNewRow() {
        if(mSequencerItemListAdapter == null) return;
        mSequencerItemListAdapter.addNew();
    }


    private abstract class SequencerItemListener {
        public abstract void onEvent(SequencerItem sequenceItem);
    }

    private class SequencerItem implements Runnable {
        public AudioSegmentRecord mAudioSegmentRecord;
        private float mDurationIndex;
        private long mDurationByteCount;
        private float mStartIndex;
        private float mEndIndex;
        private long mStartBytePos;
        private long mEndBytePos;
        private long mCurrentBytePos;

        private AudioTrack mAudioTrack;
        private int mTrackBufferSize;
        private long mPlayPeriodInMilli;

        private ScheduledExecutorService mAudioWriterExecutor;

        private SkipFixedInputStream mInputStream = null;
        private long mStreamCurrentBytePos;

        private boolean mIsPlaying = false;
        private SequencerItemListener mSequenceListener;
        private Runnable mOnProgressRunnable;

        private ArrayList<SequencerItemListener> mSequenceListenerList = new ArrayList<>();
        private float mVolume = 1;
        private float mTempo = 1;

        public SequencerItem(AudioSegmentRecord record) {
            setAudioSegmentRecord(record);
            mAudioWriterExecutor = Executors.newScheduledThreadPool(1);
            mOnProgressRunnable = new Runnable() {
                @Override
                public void run() {
                    fireEvent();
                }
            };
        }

        private void fireEvent() {
            for(SequencerItemListener listener: mSequenceListenerList) {
                listener.onEvent(this);
            }
        }

        public void addSequenceListener(SequencerItemListener listener) {
            mSequenceListenerList.add(listener);
        }

        public void removeSequenceListener(SequencerItemListener listener) {
            mSequenceListenerList.remove(listener);
        }

        public void setAudioSegmentRecord(AudioSegmentRecord record) {
            mAudioSegmentRecord = record;
            mCurrentBytePos = 0;
            this.reformat();
            mTempo = mAudioSegmentRecord == null ? 1 : mAudioSegmentRecord.getTempo();
            mStartIndex = 0;
            mEndIndex = 0;
            try {
                createTrack(mTempo);
            } catch (Exception e) {}
            if(mAudioSegmentRecord == null) {
                if(mAudioTrack != null) mAudioTrack.release();
                mAudioTrack = null;
            }
            try {
                mInputStream = new SkipFixedInputStream(new FileInputStream(mAudioSegmentRecord.getAudioFile()));
            } catch (Exception e) {
                return;
            }
            mVolume = mAudioSegmentRecord.getVolume();
            mInputStream.mark((int) mAudioSegmentRecord.getLengthInByte() + 1);
            try {
                mInputStream.reset();
                mInputStream.skipSured(mAudioSegmentRecord.getStartFromInByte());
            } catch (IOException e) {}
            mStreamCurrentBytePos = mAudioSegmentRecord.getStartFromInByte();

            this.reformat();
        }

        public void setStartIndex(float value) {
            mStartIndex = value;
            this.reformat();
        }

        public void setEndIndex(float value) {
            mEndIndex = value;
            this.reformat();
        }

        public void reformat() {
            if(mAudioSegmentRecord == null) return;
            mDurationIndex = mAudioSegmentRecord.getDurationInSeconds()/mSecondsPerIndex;

            long byteCount = (long) (mStartIndex*mBytesPerIndex);
            if(byteCount%2==1) byteCount -= 1;
            if(byteCount<0) byteCount = 0;
            mStartBytePos = byteCount;

            if(mEndIndex >0) {
                byteCount = (long) (mEndIndex * mBytesPerIndex);
            } else {
                byteCount = mStartBytePos + (long) (mAudioSegmentRecord.getDurationInByte()/mTempo);
            }
            if (byteCount % 2 == 1) byteCount -= 1;
            if (byteCount < 2) byteCount = 2;
            mEndBytePos = byteCount;
            if(mEndBytePos>mSequenceDurationInBytes) mEndBytePos = mSequenceDurationInBytes;
            mDurationByteCount = mEndBytePos - mStartBytePos;
        }

        private void createTrack(float tempo) throws Exception {
            float sampleRate = Recorder.getSampleRateInHz() * tempo;
            int minBufferSize = AudioTrack.getMinBufferSize(
                    (int) sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            int trackBufferSize = minBufferSize * 4;
            if (trackBufferSize%2 == 1) trackBufferSize += 1;
            AudioTrack audioTrack = null;
            try {
                audioTrack = new AudioTrack(
                        Player.streamType, (int) sampleRate,
                        AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                        trackBufferSize,
                        AudioTrack.MODE_STREAM);
            } catch (IllegalArgumentException e) {
                throw new Exception("Bad "  +e.getMessage());
            }

            if(mAudioTrack != null) mAudioTrack.release();
            mAudioTrack = audioTrack;
            mTrackBufferSize = trackBufferSize;
            mPlayPeriodInMilli = (int) Math.floor(mTrackBufferSize*.5*1000F/sampleRate);
        }

        private void clear() {
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
            mSequenceListenerList.clear();
        }

        public void play() {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    if(!mIsPlaying) {
                        mIsPlaying = true;
                        if(mAudioTrack != null) mAudioTrack.play();
                        schedulePlaying();
                    }
                }
            };
            mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
        }

        public void pause() {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    mIsPlaying = false;
                    if(mAudioTrack != null) mAudioTrack.pause();
                }
            };
            mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
        }

        public void seek(final long byteCount) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    mCurrentBytePos = byteCount;
                    if(mCurrentBytePos<mStartBytePos || mCurrentBytePos>=mEndBytePos) {
                        resetStream();
                        return;
                    }
                    long shiftStream = (long) ((mCurrentBytePos-mStartBytePos)*mTempo);
                    if(shiftStream%2==1) shiftStream -= 1;
                    if(shiftStream<0) shiftStream = 0;
                    shiftStream %= mAudioSegmentRecord.getDurationInByte();

                    mStreamCurrentBytePos = mAudioSegmentRecord.getStartFromInByte() + shiftStream;
                    try {
                        mInputStream.reset();
                        mInputStream.skipSured(mStreamCurrentBytePos);
                    } catch (IOException e) {}
                }
            };
            mAudioWriterExecutor.schedule(task, 0, TimeUnit.SECONDS);
        }

        private void schedulePlaying() {
            mAudioWriterExecutor.schedule(this, 0, TimeUnit.SECONDS);
        }

        private void resetStream() {
            try {
                mInputStream.reset();
                mInputStream.skipSured(mAudioSegmentRecord.getStartFromInByte());
            } catch (IOException e) {}
            mStreamCurrentBytePos = mAudioSegmentRecord.getStartFromInByte();
        }

        @Override
        public void run() {
            if(!mIsPlaying) return;

            long startTime = new Date().getTime();

            if (mAudioSegmentRecord != null) {
                byte[] bytes = new byte[mTrackBufferSize];
                Arrays.fill(bytes, (byte) 0);
                int totalReadCount = 0;
                int readCount;
                int zeroCount = 0;
                while(totalReadCount<mTrackBufferSize && zeroCount<2) {
                    if(mCurrentBytePos < mStartBytePos && mCurrentBytePos+mTrackBufferSize>mStartBytePos) {
                        totalReadCount +=  mStartBytePos-mCurrentBytePos;
                        mCurrentBytePos =  mStartBytePos;
                    }
                    if (mCurrentBytePos >= mStartBytePos && mCurrentBytePos<mEndBytePos) {
                        if(mStreamCurrentBytePos>=mAudioSegmentRecord.getEndToInByte()) {
                            resetStream();
                        }
                        int bufferSize = (int) Math.min(mAudioSegmentRecord.getDurationInByte(), (mTrackBufferSize-totalReadCount));
                        bufferSize = (int) Math.min(bufferSize, (mEndBytePos - mCurrentBytePos)*mTempo);
                        if(bufferSize%2==1) bufferSize -= 2;
                        if(bufferSize<2) bufferSize = 2;
                        try {
                            readCount = mInputStream.read(bytes, totalReadCount, mTrackBufferSize-totalReadCount);
                        } catch (IOException e) {
                            break;
                        }
                        if(readCount == 0) {
                            zeroCount++;
                        } else {
                            zeroCount = 0;
                            totalReadCount += readCount;
                            mStreamCurrentBytePos += readCount;
                            mCurrentBytePos += readCount/mTempo;
                            if(mCurrentBytePos%2 == 1) mCurrentBytePos -= 1;
                            if(mCurrentBytePos <2 ) mCurrentBytePos = 2;
                            if(mCurrentBytePos>=mEndBytePos) {
                                resetStream();
                            }
                        }
                    } else {
                        mCurrentBytePos += mTrackBufferSize-totalReadCount;
                        totalReadCount = mTrackBufferSize;
                    }
                    if(mCurrentBytePos>=mSequenceDurationInBytes) {
                        mCurrentBytePos = 0;
                    }
                }
                short[] shorts = new short[mTrackBufferSize / 2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

                short[] interleaved = new short[mTrackBufferSize];
                for (int i = 0; i < shorts.length; i++) {
                    shorts[i] = (short) (Player.sVolume * mVolume * shorts[i]);
                    interleaved[i * 2] = shorts[i];
                    interleaved[i * 2 + 1] = shorts[i];
                }
                mAudioTrack.write(interleaved, 0, interleaved.length);
                mAudioTrack.flush();
            } else {
                if(mCurrentBytePos>=mEndBytePos) mCurrentBytePos = mStartBytePos;
                mCurrentBytePos += mTrackBufferSize;
            }
            mHandler.post(mOnProgressRunnable);

            long elapsedTime = new Date().getTime()-startTime;
            mAudioWriterExecutor.schedule(this, mPlayPeriodInMilli - elapsedTime, TimeUnit.MILLISECONDS);
        }
    }

    private class AudioSegmentItemView extends FrameLayout {
        private AudioSegmentRecord mAudioSegmentRecord;
        private TextView mTxtName;
        private AudioSegmentView mAudioSegmentView;
        private PlayerListener mPlayerListener;

        public AudioSegmentItemView(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View rootView = inflater.inflate(R.layout.player_spinner_item_view, this);
            mTxtName = (TextView) rootView.findViewById(R.id.txtName);
            mAudioSegmentView = (AudioSegmentView) rootView.findViewById(R.id.audioSegment);
        }

        public void setAudioSegmentRecord(AudioSegmentRecord record) {
            mAudioSegmentRecord = record;
            mAudioSegmentView.setBackgroundBitmap(null);
            if(mAudioSegmentRecord == null) {
                mTxtName.setText(R.string.silence);
            } else {
                mTxtName.setText(mAudioSegmentRecord.getName());
                mAudioSegmentView.setRegion(
                        mAudioSegmentRecord.getStartFromInPercent(),
                        mAudioSegmentRecord.getEndToInPercent()
                );
                mExecutor.execute(getBitmapLoader(mAudioSegmentRecord));
            }
        }

        private Runnable getBitmapLoader(final AudioSegmentRecord record) {
            return new Runnable() {
                @Override
                public void run() {
                    if(!record.equals(mAudioSegmentRecord)) return;
                    mHandler.post(getBitmapShower(record, record.getWaveGraphBitmap()));
                }
            };
        }

        private Runnable getBitmapShower(final AudioSegmentRecord record, final Bitmap bitmap) {
            return new Runnable() {
                @Override
                public void run() {
                    if(!record.equals(mAudioSegmentRecord)) return;
                    mAudioSegmentView.setBackgroundBitmap(bitmap);
                }
            };
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mAudioSegmentRecord = null;
        }
    }

    private class AudioSegmentListAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<AudioSegmentRecord> mAudioSegmentList;

        public AudioSegmentListAdapter(Context context, ArrayList<AudioSegmentRecord> audioSegmentList) {
            mContext = context;
            mAudioSegmentList = new ArrayList<>();
            mAudioSegmentList.add(null);
            mAudioSegmentList.addAll(audioSegmentList);
        }

        public int getItemPosition(AudioSegmentRecord record) {
            return record == null ? 0 : mAudioSegmentList.indexOf(record);
        }

        @Override
        public int getCount() {
            return mAudioSegmentList.size();
        }

        @Override
        public AudioSegmentRecord getItem(int position) {
            return mAudioSegmentList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AudioSegmentItemView itemView;
            if(convertView == null) {
                itemView = new AudioSegmentItemView(mContext);
            } else {
                itemView = (AudioSegmentItemView) convertView;
            }
            itemView.setAudioSegmentRecord(getItem(position));
            return itemView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            return view;
        }
    }

    private class SequencerItemView extends FrameLayout {
        private SequencerItem mSequencerItem;
        private Spinner mSpnPlayers;
        private TextView mTxtDurationIndex;
        private EditText mEdtStartIndex;
        private EditText mEdtEndIndex;
        private View mBtnDelete;
        private SequencerItemListener mSequencerItemListener;

        private SeekBar  mSeekBar;

        public SequencerItemView(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View rootView = inflater.inflate(R.layout.sequencer_item_view, this);
            mBtnDelete = rootView.findViewById(R.id.imvDelete);
            mSpnPlayers = (Spinner) rootView.findViewById(R.id.spnPlayers);
            mTxtDurationIndex = (TextView) rootView.findViewById(R.id.txtDurationIndex);
            mEdtStartIndex = (EditText) rootView.findViewById(R.id.edtStartIndex);
            mEdtEndIndex = (EditText) rootView.findViewById(R.id.edtEndIndex);

            mSeekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
            mSeekBar.setVisibility(GONE);

            mSpnPlayers.setAdapter(mAudioSegmentListAdapter);

            mSpnPlayers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (mSequencerItem == null) return;
                    mSequencerItem.setAudioSegmentRecord(mAudioSegmentListAdapter.getItem(position));
                    showIndices();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            mEdtStartIndex = (EditText) rootView.findViewById(R.id.edtStartIndex);
            mEdtStartIndex.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mSequencerItem == null) return;
                    try {
                        mSequencerItem.setStartIndex(Float.parseFloat(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            mEdtEndIndex = (EditText) rootView.findViewById(R.id.edtEndIndex);
            mEdtEndIndex.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mSequencerItem == null) return;
                    try {
                        mSequencerItem.setEndIndex(Float.parseFloat(s.toString()));
                    } catch (NumberFormatException e) {
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            mBtnDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSequencerItemListAdapter.remove(mSequencerItem);
                }
            });
            mSequencerItemListener = new SequencerItemListener() {
                @Override
                public void onEvent(SequencerItem sequenceItem) {
                    boolean running = (sequenceItem.mStartBytePos<=sequenceItem.mCurrentBytePos &&
                            sequenceItem.mCurrentBytePos<=sequenceItem.mEndBytePos);
                    mTxtDurationIndex.setBackgroundColor(running ? Color.GREEN : Color.argb(0,0,0,0));
                    /*
                    float value = 100*sequenceItem.mCurrentBytePos/(float) mSequenceDurationInBytes;
                    mSeekBar.setProgress((int) value);
                    */
                }
            };
        }

        public void setSequencerItem(SequencerItem sequencerItem) {
            if(mSequencerItem != null) mSequencerItem.removeSequenceListener(mSequencerItemListener);
            mSequencerItem = sequencerItem;
            mSpnPlayers.setSelection(mAudioSegmentListAdapter.getItemPosition(mSequencerItem.mAudioSegmentRecord));
            showIndices();
            mTxtDurationIndex.setBackgroundColor(Color.argb(0, 0, 0, 0));
            mSequencerItem.addSequenceListener(mSequencerItemListener);
        }

        private void showIndices() {
            mTxtDurationIndex.setText(String.format("%.2f", mSequencerItem.mDurationIndex));
            mEdtStartIndex.setText(String.valueOf(mSequencerItem.mStartIndex));
            mEdtEndIndex.setText(String.valueOf(mSequencerItem.mEndIndex));
        }
    }

    private class SequencerItemListAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<SequencerItem> mSequencerItemList;

        public SequencerItemListAdapter(Context context) {
            mContext = context;
            mSequencerItemList = new ArrayList<>();
        }

        public void addNew() {
            mSequencerItemList.add(new SequencerItem(mAudioSegmentListAdapter.getItem(0)));
            notifyDataSetChanged();
        }

        public void addNew(AudioSegmentRecord audioSegmentRecord) {
            mSequencerItemList.add(new SequencerItem(audioSegmentRecord));
            notifyDataSetChanged();
        }

        public void remove(SequencerItem sequencerItem) {
            mSequencerItemList.remove(sequencerItem);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mSequencerItemList.size();
        }

        @Override
        public SequencerItem getItem(int position) {
            return mSequencerItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SequencerItemView sequencerItemView;
            if(convertView == null) {
                sequencerItemView = new SequencerItemView(mContext);
            } else {
                sequencerItemView = (SequencerItemView) convertView;
            }
            sequencerItemView.setSequencerItem(getItem(position));
            return sequencerItemView;
        }
    }
}
