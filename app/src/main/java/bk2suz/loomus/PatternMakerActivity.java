package bk2suz.loomus;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.Spinner;
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sujoy on 30/10/15.
 */
public class PatternMakerActivity extends AppCompatActivity {
    private ListView mPatterListView;
    private View mBtnCancel;
    private View mBtnSave;
    private View mBtnAdd;
    private View mLockScreen;
    private ProgressBar mPgrProcessing;

    private PatternListAdapter mPatternListAdapter;
    private PlayerListAdapter mPlayerListAdapter;

    private boolean mProcessing = false;
    private ExecutorService mExecutor;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_maker_activity);

        mPatterListView = (ListView) findViewById(R.id.trackListView);
        AudioSegmentRecord.getRecords(new OnLoadListener<ArrayList<AudioSegmentRecord>>() {
            @Override
            public void onLoad(ArrayList<AudioSegmentRecord> recordList) {
                populatePlayers(recordList);
            }
        });

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
        if(mPatternListAdapter.getCount() ==0) return;
        mLockScreen.setVisibility(View.VISIBLE);
        mPgrProcessing.setVisibility(View.VISIBLE);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                stitchTogether();
            }
        });
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

        int maxBufferSize = 1024, bufferSize;
        byte[] readBytes = new byte[maxBufferSize];

        for(Pattern pattern: mPatternListAdapter.mPatternList) {
            AudioSegmentRecord segmentRecord = pattern.mPlayer.getAudioSegmentRecord();
            if (segmentRecord == null) {
                long totalReadCount = 0;
                long totalByteCount = (long) Math.floor(sampleRate * pattern.mMult);
                totalByteCount *= 2;
                Arrays.fill(readBytes, (byte) 0);
                while(true) {
                    if(maxBufferSize>(totalByteCount-totalReadCount)) {
                        bufferSize = (int) (totalByteCount-totalReadCount);
                    } else {
                        bufferSize = maxBufferSize;
                    }
                    try {
                        outputStream.write(readBytes, 0, bufferSize);
                        outputStream.flush();
                    } catch (IOException e) {
                        break;
                    }
                    totalReadCount += bufferSize;
                    if(totalReadCount>=totalByteCount) break;
                }
            } else {
                SkipFixedInputStream inputStream = null;
                try {
                    inputStream = new SkipFixedInputStream(new FileInputStream(segmentRecord.getAudioFile()));
                } catch (FileNotFoundException e) {
                    continue;
                }
                inputStream.mark((int) segmentRecord.getLengthInByte() + 1);

                long startFromInByte = segmentRecord.getStartFromInByte();
                long endToInByte = segmentRecord.getEndToInByte();
                long durationInByte = endToInByte - startFromInByte;

                float volume = Player.sVolume * segmentRecord.getVolume();
                float tempo = segmentRecord.getTempo();

                long totalReadCount = 0;
                long totalByteCount = (long) Math.floor(durationInByte * pattern.mMult);
                if(totalByteCount%2==1) totalByteCount += 1;

                long readPosition = startFromInByte;
                try {
                    inputStream.reset();
                    inputStream.skipSured(startFromInByte);
                } catch (IOException e) {}
                while(true) {
                    if(maxBufferSize > (endToInByte-readPosition)) {
                        bufferSize = (int) (endToInByte-readPosition);
                    } else {
                        bufferSize = maxBufferSize;
                    }
                    if(bufferSize>(totalByteCount-totalReadCount)) {
                        bufferSize = (int) (totalByteCount-totalReadCount);
                    }
                    int readCount;
                    try {
                        readCount = inputStream.read(readBytes, 0, bufferSize);
                    } catch (IOException e) {
                        break;
                    }
                    if(readCount<=0) break;

                    short[] readShorts = new short[readCount/2];
                    ByteBuffer.wrap(readBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(readShorts);

                    int writeMaxShortCount = readShorts.length;
                    if(tempo<1) writeMaxShortCount = (int) Math.ceil(readShorts.length/tempo);
                    short[] writeShorts = new short[writeMaxShortCount];
                    int writeCount = 0;

                    for (float i = 0; i <readShorts.length; i += tempo) {
                        int pos = (int) Math.floor(i);
                        writeShorts[writeCount] = (short) (volume*readShorts[pos]);
                        writeCount++;
                    }

                    byte[] writeBytes = new byte[writeCount*2];
                    ByteBuffer.wrap(writeBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(writeShorts);

                    try {
                        outputStream.write(writeBytes, 0, writeBytes.length);
                        outputStream.flush();
                    } catch (IOException e) {
                        break;
                    }

                    readPosition += readCount;
                    if (readPosition>=endToInByte) {
                        try {
                            inputStream.reset();
                            inputStream.skipSured(startFromInByte);
                            readPosition = startFromInByte;
                        } catch (IOException e) {}
                    }
                    totalReadCount += readCount;
                    if(totalReadCount>=totalByteCount) break;
                }
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
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
        if(mPlayerListAdapter != null) mPlayerListAdapter.clear();
        mPlayerListAdapter = null;
        if(mExecutor != null) mExecutor.shutdownNow();
        mExecutor = null;
        super.onDestroy();
    }

    private void populatePlayers(ArrayList<AudioSegmentRecord> recordList) {
        mPlayerListAdapter = new PlayerListAdapter(this, recordList);
        mPatternListAdapter = new PatternListAdapter(this);
        for(int i=1; i<mPlayerListAdapter.getCount(); i++) {
            mPatternListAdapter.addNew(mPlayerListAdapter.getItem(i));
            break;
        }
        if(mPlayerListAdapter.getCount()==0) {
            addNewRow();
        }
        mPatterListView.setAdapter(mPatternListAdapter);
    }

    private void addNewRow() {
        if(mPatternListAdapter == null) return;
        mPatternListAdapter.addNew();
    }

    private static class Pattern {
        private Player mPlayer = null;
        private float mMult = 1;

        public Pattern(Player player) {
            mPlayer = player;
        }
    }

    private static class PlayerSpinnerItemView extends FrameLayout {
        private Player mPlayer;
        private TextView mTxtName;
        private AudioSegmentView mAudioSegmentView;
        private PlayerListener mPlayerListener;

        public PlayerSpinnerItemView(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View rootView = inflater.inflate(R.layout.player_spinner_item_view, this);
            mTxtName = (TextView) rootView.findViewById(R.id.txtName);
            mAudioSegmentView = (AudioSegmentView) rootView.findViewById(R.id.audioSegment);

            mPlayerListener = new PlayerListener() {
                @Override
                public void onPlay() {}

                @Override
                public void onPause() {}

                @Override
                public void onSeek() {}

                @Override
                public void onProgress(float head) {}

                @Override
                public void onError(String message) {}

                @Override
                public void onGraphLoad(Bitmap graphBitmap) {
                    mAudioSegmentView.setBackgroundBitmap(graphBitmap);
                }
            };
        }

        public void setPlayer(Player player) {
            if(mPlayer != null) mPlayer.removePlayerListener(mPlayerListener);
            mPlayer = player;
            if(mPlayer.getAudioSegmentRecord() == null) {
                mTxtName.setText(R.string.silence);
                mAudioSegmentView.setBackgroundBitmap(null);
            } else {
                mPlayer.addPlayerListener(mPlayerListener);
                mTxtName.setText(mPlayer.getName());
                mAudioSegmentView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
                mPlayer.loadGraphBitmap();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if(mPlayer != null) mPlayer.removePlayerListener(mPlayerListener);
            mPlayer = null;
        }
    }

    private static class PlayerListAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Player> mPlayerList;

        public PlayerListAdapter(Context context, ArrayList<AudioSegmentRecord> audioSegmentList) {
            mContext = context;
            mPlayerList = new ArrayList<>();
            try {
                mPlayerList.add(new Player(null, false));
            } catch (Exception e) {}
            for(AudioSegmentRecord record: audioSegmentList) {
                Player player = null;
                try {
                    player = new Player(record, false);
                } catch (Exception e) {
                    continue;
                }
                mPlayerList.add(player);
            }
        }

        public void clear() {
            for(Player player: mPlayerList) {
                player.cleanIt();
            }
            mPlayerList.clear();
        }

        public int getItemPosition(Player player) {
            return mPlayerList.indexOf(player);
        }

        @Override
        public int getCount() {
            return mPlayerList.size();
        }

        @Override
        public Player getItem(int position) {
            return mPlayerList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PlayerSpinnerItemView itemView;
            if(convertView == null) {
                itemView = new PlayerSpinnerItemView(mContext);
            } else {
                itemView = (PlayerSpinnerItemView) convertView;
            }
            itemView.setPlayer(getItem(position));
            return itemView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            return view;
        }
    }

    private class PatternItemView extends FrameLayout {
        private Pattern mPattern;
        private Spinner mSpnPlayers;
        private EditText mEdtMult;
        private View mBtnDelete;

        public PatternItemView(Context context) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View rootView = inflater.inflate(R.layout.pattern_item_view, this);
            mBtnDelete = rootView.findViewById(R.id.imvDelete);
            mSpnPlayers = (Spinner) rootView.findViewById(R.id.spnPlayers);
            mEdtMult = (EditText) rootView.findViewById(R.id.edtMult);
            mSpnPlayers.setAdapter(mPlayerListAdapter);

            mSpnPlayers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (mPattern == null) return;
                    mPattern.mPlayer = mPlayerListAdapter.getItem(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
            mEdtMult.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (mPattern == null) return;
                    try {
                        mPattern.mMult = Float.parseFloat(s.toString());
                    } catch (NumberFormatException e) {
                    }
                }
            });
            mBtnDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPatternListAdapter.remove(mPattern);
                }
            });
        }

        public void setPattern(Pattern pattern) {
            mPattern = pattern;
            mSpnPlayers.setSelection(mPlayerListAdapter.getItemPosition(mPattern.mPlayer));
            mEdtMult.setText(String.valueOf(mPattern.mMult));
        }
    }

    private class PatternListAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Pattern> mPatternList;

        public PatternListAdapter(Context context) {
            mContext = context;
            mPatternList = new ArrayList<>();
        }

        public void addNew() {
            mPatternList.add(new Pattern(mPlayerListAdapter.getItem(0)));
            notifyDataSetChanged();
        }

        public void addNew(Player player) {
            mPatternList.add(new Pattern(player));
            notifyDataSetChanged();
        }

        public void remove(Pattern pattern) {
            mPatternList.remove(pattern);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mPatternList.size();
        }

        @Override
        public Pattern getItem(int position) {
            return mPatternList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PatternItemView patternItemView;
            if(convertView == null) {
                patternItemView = new PatternItemView(mContext);
            } else {
                patternItemView = (PatternItemView) convertView;
            }
            patternItemView.setPattern(getItem(position));
            return patternItemView;
        }
    }
}
