package bk2suz.loomus;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import java.util.ArrayList;

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
    }

    private void save() {
        mLockScreen.setVisibility(View.VISIBLE);
        mPgrProcessing.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        if(mPlayerListAdapter != null) mPlayerListAdapter.clear();
        super.onDestroy();
    }

    private void populatePlayers(ArrayList<AudioSegmentRecord> recordList) {
        mPlayerListAdapter = new PlayerListAdapter(this, recordList);
        mPatternListAdapter = new PatternListAdapter(this);
        for(int i=1; i<mPlayerListAdapter.getCount(); i++) {
            mPatternListAdapter.addNew(mPlayerListAdapter.getItem(i));
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
