package bk2suz.loomus;

import android.app.ActionBar;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private static final String Dash = "-";
    private static final String Zero = "0.0";
    private static final int PATTERN_MAKER_REQUEST_CODE = 1;
    private static final int SEQUENCE_MAKER_REQUEST_CODE = 2;

    private TextView mTxtRecorderElapsedTime;

    private View mRecorderRecordButton;
    private View mRecorderPauseButton;
    private View mRecorderSaveButton;
    private View mRecorderCancelButton;
    private Spinner mSpinnerMaxTimeOptions;
    private LevelView mRecordLevelView;

    private SliderView mVolumeSliderView;

    private View mPlayerListPlayButton;
    private View mPlayerListPauseButton;
    private View mPlayerListResetButton;
    private View mPlayerListDeleteToggleButton;
    private View mPatternMakerButton;
    private View mSequenceMakerButton;
    private View mMergeButton;
    private View mExportButton;
    private View mProcessingView;

    private Recorder mRecorder = null;
    private RecorderListener mRecorderListener;

    private PlayerListAdapter mPlayerListAdapter;
    private PlayerEditorView mPlayerEditorView;

    private ExecutorService mExecutor;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        setVolumeControlStream(Player.streamType);

        ArrayList<String> maxTimeList = new ArrayList<String>();
        for(float t=0; t<10; t+=0.5) {
            if(t == 0) {
               maxTimeList.add(Dash);
            } else {
                maxTimeList.add(String.valueOf(t));
            }
        }
        ArrayAdapter<String> maxTimeAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                maxTimeList
        );
        mSpinnerMaxTimeOptions = (Spinner) findViewById(R.id.spnMaxTimeOptions);
        mSpinnerMaxTimeOptions.setAdapter(maxTimeAdapter);
        mRecordLevelView = (LevelView) findViewById(R.id.lvlRecordLevel);

        mRecorderListener = new ActivityRecorderListener();
        mTxtRecorderElapsedTime = (TextView) findViewById(R.id.txtElapsedTime);

        mRecorderRecordButton = findViewById(R.id.imbRecord);
        mRecorderPauseButton = findViewById(R.id.imbPause);
        mRecorderSaveButton = findViewById(R.id.imbSave);
        mRecorderCancelButton = findViewById(R.id.imbCancel);

        mRecorderPauseButton.setVisibility(View.GONE);
        mRecorderSaveButton.setVisibility(View.GONE);
        mRecorderCancelButton.setVisibility(View.GONE);

        mRecorderRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String optionName = (String) mSpinnerMaxTimeOptions.getSelectedItem();
                float maxTime = 0;
                if (!optionName.equals(Dash)) maxTime = Float.parseFloat(optionName);
                try {
                    if (mRecorder != null) mRecorder.cleanIt();
                    mRecorder = new Recorder(maxTime, mRecorderListener);
                } catch (Exception e) {
                    mRecorder = null;
                }
                if (mRecorder != null) mRecorder.record();
            }
        });

        mRecorderPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder == null) return;
                mRecorder.pause();
            }
        });

        mRecorderSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder == null) return;
                mRecorder.save();
            }
        });

        mRecorderCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRecorder == null) return;
                mRecorder.cancel();
            }
        });

        mVolumeSliderView = (SliderView) findViewById(R.id.volumeSlider);
        mVolumeSliderView.setValue(Player.sVolume);
        mVolumeSliderView.setOnChangeListener(new SliderView.OnChangeListener() {
            @Override
            public void onChange(float value, boolean ongoing) {
                Player.sVolume = value;
            }
        });

        mPlayerListPlayButton = findViewById(R.id.imbPlay);
        mPlayerListPauseButton = findViewById(R.id.imbPausePlaying);
        mPlayerListResetButton = findViewById(R.id.imbResetPlaying);
        mPlayerListDeleteToggleButton = findViewById(R.id.imbDeleteToggle);
        mPatternMakerButton = findViewById(R.id.imbPatternMaker);
        mSequenceMakerButton = findViewById(R.id.imbSequenceMaker);
        mMergeButton = findViewById(R.id.imbMerge);
        mExportButton = findViewById(R.id.imbExport);
        mProcessingView = findViewById(R.id.pgrProcessing);
        mProcessingView.setVisibility(View.GONE);

        mPlayerListPlayButton.setVisibility(View.GONE);
        mPlayerListPauseButton.setVisibility(View.GONE);
        mPlayerListResetButton.setVisibility(View.GONE);
        mPlayerListDeleteToggleButton.setVisibility(View.GONE);

        mPlayerListPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerListAdapter.play();
                mPlayerListPlayButton.setVisibility(View.GONE);
                mPlayerListPauseButton.setVisibility(View.VISIBLE);
            }
        });

        mPlayerListPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerListAdapter.pause();
                mPlayerListPlayButton.setVisibility(View.VISIBLE);
                mPlayerListPauseButton.setVisibility(View.GONE);
            }
        });

        mPlayerListResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerListAdapter.reset();
            }
        });

        mPlayerListDeleteToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerListAdapter.toggleDelete();
            }
        });

        mPatternMakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PatternMakerActivity.class);
                startActivityForResult(intent, PATTERN_MAKER_REQUEST_CODE);
            }
        });

        mSequenceMakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SequencerActivity.class);
                startActivityForResult(intent, SEQUENCE_MAKER_REQUEST_CODE);
            }
        });

        mMergeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPlayerListAdapter.getCount() ==0) return;

                mProcessingView.setVisibility(View.VISIBLE);
                mExportButton.setVisibility(View.GONE);
                mMergeButton.setVisibility(View.GONE);
                mergeTogetherAsync(false);
            }
        });

        mExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPlayerListAdapter.getCount() ==0) return;

                mProcessingView.setVisibility(View.VISIBLE);
                mExportButton.setVisibility(View.GONE);
                mMergeButton.setVisibility(View.GONE);
                mergeTogetherAsync(true);
            }
        });

        mPlayerEditorView = (PlayerEditorView) findViewById(R.id.playerEditor);
        mPlayerEditorView.setOnCloseListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerEditorView.setPlayer(null);
                mPlayerEditorView.setVisibility(View.GONE);
                mPlayerListAdapter.notifyDataSetChanged();
            }
        });

        AudioSegmentRecord.getRecords(new OnLoadListener<ArrayList<AudioSegmentRecord>>() {
            @Override
            public void onLoad(ArrayList<AudioSegmentRecord> recordList) {
                loadAudioSegments(recordList);
            }
        });

        mExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(Looper.getMainLooper());

    }

    private Runnable getFinishRunnable(final File file, final boolean save) {
        return new Runnable() {
            @Override
            public void run() {
                mProcessingView.setVisibility(View.GONE);
                mExportButton.setVisibility(View.VISIBLE);
                mMergeButton.setVisibility(View.VISIBLE);
                if(file == null) return;
                if(save) {
                    showExportDoneMessage(file);
                } else {
                    mPlayerListAdapter.addNew(file, 1);
                }
            }
        };
    }

    private void showExportDoneMessage(File file) {
        Toast.makeText(this, getResources().getString(R.string.exported_to, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
    }

    private void mergeTogetherAsync(final boolean save) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mergeTogether(save);
            }
        });
    }

    private void mergeTogether(boolean save) {
        File outputFile = null;
        if(save) {
            outputFile = AppOverload.getTempAudioFile();
        } else {
            outputFile = AppOverload.getPermaAudioFile();
        }
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            mHandler.post(getFinishRunnable(null, save));
            return;
        }

        ArrayList<AudioSegmentReader> readerList = new ArrayList<>();
        for(int i=0; i<mPlayerListAdapter.getCount(); i++) {
            Player player = mPlayerListAdapter.getItem(i);
            if(!player.checkIsEnabled()) continue;
            readerList.add(new AudioSegmentReader(player.getAudioSegmentRecord()));
        }

        if(readerList.size() == 0) {
            mHandler.post(getFinishRunnable(null, save));
            return;
        }

        long totalDurationinByte = mPlayerListAdapter.getMaxDurationInByte();

        int maxBufferSize = 1024;
        int byteBufferSize, shortBufferSize;
        byte[] writeBytes = new byte[maxBufferSize];
        short[] writeShorts = new short[maxBufferSize/2];

        int p = 0;
        while(p<totalDurationinByte) {
            if(maxBufferSize>(totalDurationinByte-p)) {
                byteBufferSize = (int) (totalDurationinByte-p);
            } else {
                byteBufferSize = maxBufferSize;
            }
            if(byteBufferSize<=0) byteBufferSize = 2;//safety
            shortBufferSize = byteBufferSize/2;
            Arrays.fill(writeShorts, (short) 0);

            for(AudioSegmentReader reader: readerList) {
                short[] readShorts = reader.getShorts(shortBufferSize);
                for(int i=0; i<readShorts.length; i++) {
                    writeShorts[i] += readShorts[i]*reader.mVolume/readerList.size();
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
        try {
            outputStream.close();
        } catch (IOException e) {
            mHandler.post(getFinishRunnable(null, save));
            return;
        }
        if(save) {
            File wavFile = AppOverload.getNewExportAudioFile(".wav");
            boolean result = Wave.save(1, Recorder.getSampleRateInHz(), 16, outputFile, wavFile);
            outputFile.delete();

            if (!result) {
                mHandler.post(getFinishRunnable(null, save));
                return;
            }
            mHandler.post(getFinishRunnable(wavFile, save));

        } else {
            mHandler.post(getFinishRunnable(outputFile, save));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if((requestCode == PATTERN_MAKER_REQUEST_CODE ||
                requestCode == SEQUENCE_MAKER_REQUEST_CODE) && resultCode == RESULT_OK) {
            String fileName =  data.getStringExtra(AudioSegmentRecord.FieldFileName);
            if(fileName != null && !fileName.isEmpty()) {
                mPlayerListAdapter.addNew(new File(fileName), 1);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadAudioSegments(ArrayList<AudioSegmentRecord> recordList) {
        mPlayerListAdapter = new PlayerListAdapter(this, recordList, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Player player = ((PlayerItemView) v).getPlayer();
                if(player != null) {
                    mPlayerEditorView.setPlayer(player);
                    mPlayerEditorView.setVisibility(View.VISIBLE);
                }
            }
        });
        ListView playerListView = (ListView) findViewById(R.id.playerListView);
        playerListView.setAdapter(mPlayerListAdapter);

        mPlayerListPlayButton.setVisibility(View.VISIBLE);
        mPlayerListResetButton.setVisibility(View.VISIBLE);
        mPlayerListDeleteToggleButton.setVisibility(View.VISIBLE);
    }


    private class ActivityRecorderListener extends RecorderListener {
        @Override
        public void onCancel() {
            mRecorderRecordButton.setVisibility(View.VISIBLE);
            mRecorderPauseButton.setVisibility(View.GONE);
            mRecorderSaveButton.setVisibility(View.GONE);
            mRecorderCancelButton.setVisibility(View.GONE);
            mTxtRecorderElapsedTime.setText(Zero);
            mRecordLevelView.setValue(0);
            mRecorder.cleanIt();
            mRecorder = null;
            AppOverload.clearTempAudioDir();
        }

        @Override
        public void onRecord() {
            mRecorderRecordButton.setVisibility(View.GONE);
            mRecorderPauseButton.setVisibility(View.VISIBLE);
            mRecorderSaveButton.setVisibility(View.GONE);
            mRecorderCancelButton.setVisibility(View.GONE);
        }

        @Override
        public void onPause() {
            mRecorderRecordButton.setVisibility(View.VISIBLE);
            mRecorderPauseButton.setVisibility(View.GONE);
            mRecorderSaveButton.setVisibility(View.VISIBLE);
            mRecorderCancelButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onSave(File file) {
            mPlayerListAdapter.addNew(file, 1);
            mRecorderRecordButton.setVisibility(View.VISIBLE);
            mRecorderPauseButton.setVisibility(View.GONE);
            mRecorderSaveButton.setVisibility(View.GONE);
            mRecorderCancelButton.setVisibility(View.GONE);
            mTxtRecorderElapsedTime.setText(Zero);
            mRecordLevelView.setValue(0);
            mRecorder.cleanIt();
            mRecorder = null;
            AppOverload.clearTempAudioDir();
        }

        @Override
        public void onError(String message) {}

        @Override
        public void onProgress(float timeInSeconds, float amplitude) {
            mTxtRecorderElapsedTime.setText(String.format("%.2f", timeInSeconds));
            mRecordLevelView.setValue(amplitude);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) mRecorder.cleanIt();
        if(mPlayerListAdapter != null) mPlayerListAdapter.clear();
        if(mExecutor != null) mExecutor.shutdownNow();
        mExecutor = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
