package bk2suz.loomus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by sujoy on 14/9/15.
 */
public class PlayerView extends FrameLayout {
    private static final String Zero = "0.0";

    private Player mPlayer = null;
    private TextView mTxtName;
    private CheckBox mChkEnabled;
    private TextView mTxtDuration;
    private View mDeleteButton;
    private AudioSegmentView mAudioSegmentView;
    private RegionSliderView mRegionSliderView;

    private ViewPlayerListener mPlayerListener;
    private Player mPlayHead;
    private PlayerListAdapter mPlayerListAdapter;

    public PlayerView(Context context, PlayerListAdapter playerListAdapter) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.player_view, this);

        mPlayerListAdapter = playerListAdapter;
        mPlayHead = mPlayerListAdapter.getPlayHead();

        mTxtName = (TextView) rootView.findViewById(R.id.txtName);
        mTxtName.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                renamePlayer();
            }
        });

        mTxtDuration = (TextView) rootView.findViewById(R.id.txtDuration);
        mDeleteButton = rootView.findViewById(R.id.imbDeleteSegment);
        mDeleteButton.setVisibility(GONE);
        mAudioSegmentView = (AudioSegmentView) rootView.findViewById(R.id.audioSegment);
        mAudioSegmentView.setOnSegmentChangeListener(new AudioSegmentView.OnSegmentChangeListener() {
            @Override
            public void onRegionChange(float left, float right, boolean ongoing) {
                if (mPlayer == null) return;
                mPlayer.setRegion(left, right, !ongoing);
                mRegionSliderView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
                showDuration();

                if (!ongoing) {
                    if (mPlayHead.getDurationInByte() < mPlayer.getDurationInByte()) {
                        mPlayHead.setDurationInByte(mPlayer.getDurationInByte());
                    }
                }
            }

            @Override
            public void onHeadChange(float head) {
                if (mPlayer == null) return;
                mPlayer.setHead(head);
            }
        });
        mRegionSliderView = (RegionSliderView) rootView.findViewById(R.id.regionSlider);
        mRegionSliderView.setOnRegionChangeListener(new RegionSliderView.OnRegionChangeListener() {
            @Override
            public void onRegionChange(float left, float right, boolean ongoing) {
                if (mPlayer == null) return;
                mPlayer.setRegion(left, right, !ongoing);
                mAudioSegmentView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
                showDuration();

                if (!ongoing) {
                    if (mPlayHead.getDurationInByte() < mPlayer.getDurationInByte()) {
                        mPlayHead.setDurationInByte(mPlayer.getDurationInByte());
                    }
                }
            }
        });

        mChkEnabled = (CheckBox) rootView.findViewById(R.id.chkOn);
        mChkEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mPlayer == null) return;
                if(isChecked) {
                    mPlayer.setIsEnabled(true);
                    mPlayer.seek(mPlayHead.getCurrentPositionInByte());
                    if(mPlayHead.checkIsPlaying()) mPlayer.play();
                } else {
                    mPlayer.pause();
                    mPlayer.setIsEnabled(false);
                }
            }
        });
        mPlayerListener = new ViewPlayerListener();

        mDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePlayer();
            }
        });
    }

    public void showHideDeleteButton() {
        mDeleteButton.setVisibility(mPlayer.checkIsDeletable() ? VISIBLE : GONE);
    }

    private void deletePlayer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.sure_to_delete_audio);
        builder.setTitle(R.string.delete_audio);
        builder.setPositiveButton(R.string.yes_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPlayerListAdapter.delete(mPlayer);
            }
        });
        builder.setNegativeButton(R.string.no_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void renamePlayer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final EditText editText  = new EditText(getContext());
        editText.setText(mPlayer.getName());
        builder.setMessage(R.string.change_audio_name);
        builder.setTitle(R.string.rename_audio);
        builder.setView(editText);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPlayer.setName(editText.getText().toString());
                showName();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }

    public void setPlayer(Player player) {
        if(mPlayer != null) mPlayer.removePlayerListener(mPlayerListener);
        mPlayer = player;
        mPlayer.addPlayerListener(mPlayerListener);

        showName();
        showDuration();
        mAudioSegmentView.setHead(mPlayer.getHead());
        mRegionSliderView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
        mAudioSegmentView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());

        mChkEnabled.setChecked(mPlayer.checkIsEnabled());

        mPlayer.loadGraphBitmap();
    }

    private void showName() {
        mTxtName.setText(mPlayer.getName());
    }

    private void showDuration() {
        mTxtDuration.setText(String.format("%.2f",
                        mPlayer.getDurationInByte() * 0.5F / (float) Recorder.getSampleRateInHz())
        );
    }

    private class ViewPlayerListener extends PlayerListener {

        @Override
        public void onError(String message) {
            Log.d("LOGA", message);
        }

        @Override
        public void onPlay() {}

        @Override
        public void onPause() {}

        @Override
        public void onSeek() {}

        @Override
        public void onProgress(float head) {
            mAudioSegmentView.setHead(head);
        }

        @Override
        public void onGraphLoad(Bitmap graphBitmap) {
            mAudioSegmentView.setBackgroundBitmap(graphBitmap);
        }
    }
}
