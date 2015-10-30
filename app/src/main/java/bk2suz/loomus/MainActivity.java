package bk2suz.loomus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final String Dash = "-";
    private static final String Zero = "0.0";
    private static final int PATTERN_MAKER_REQUEST_CODE = 1;

    private TextView mTxtRecorderElapsedTime;

    private View mRecorderRecordButton;
    private View mRecorderPauseButton;
    private View mRecorderSaveButton;
    private View mRecorderCancelButton;
    private Spinner mSpinnerMaxTimeOptions;

    private View mPlayerListPlayButton;
    private View mPlayerListPauseButton;
    private View mPlayerListResetButton;
    private View mPlayerListDeleteToggleButton;
    private View mPatternMakerButton;

    private Recorder mRecorder = null;
    private RecorderListener mRecorderListener;

    private PlayerListAdapter mPlayerListAdapter;
    private PlayerEditorView mPlayerEditorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);

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

        mPlayerListPlayButton = findViewById(R.id.imbPlay);
        mPlayerListPauseButton = findViewById(R.id.imbPausePlaying);
        mPlayerListResetButton = findViewById(R.id.imbResetPlaying);
        mPlayerListDeleteToggleButton = findViewById(R.id.imbDeleteToggle);
        mPatternMakerButton = findViewById(R.id.imbPatternMaker);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PATTERN_MAKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String fileName =  data.getStringExtra(AudioSegmentRecord.FieldFileName);
            if(fileName != null && !fileName.isEmpty()) {
                mPlayerListAdapter.addNew(new File(fileName));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadAudioSegments(ArrayList<AudioSegmentRecord> recordList) {
        mPlayerListAdapter = new PlayerListAdapter(this, recordList, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Player player = ((PlayerItemView) v).getPlayer();
                if(player != null) {
                    mPlayerEditorView.setPlayer(player);
                    mPlayerEditorView.setVisibility(View.VISIBLE);
                }
                return false;
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
            mRecorder.cleanIt();
            mRecorder = null;
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
            mPlayerListAdapter.addNew(file);
            mRecorderRecordButton.setVisibility(View.VISIBLE);
            mRecorderPauseButton.setVisibility(View.GONE);
            mRecorderSaveButton.setVisibility(View.GONE);
            mRecorderCancelButton.setVisibility(View.GONE);
            mTxtRecorderElapsedTime.setText(Zero);
            //Log.d("Loga", String.format("saved at: %s", file.getAbsolutePath()));
            mRecorder.cleanIt();
            mRecorder = null;
        }

        @Override
        public void onError(String message) {
            Log.d("LOGA", message);
        }

        @Override
        public void onProgress(float timeInSeconds) {
            mTxtRecorderElapsedTime.setText(String.format("%.2f", timeInSeconds));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) mRecorder.cleanIt();
        if(mPlayerListAdapter != null) mPlayerListAdapter.clear();
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
