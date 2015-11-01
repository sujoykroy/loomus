package bk2suz.loomus;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by sujoy on 29/10/15.
 */
public class PlayerEditorView extends FrameLayout {
    private Context mContext;

    private Player mPlayer = null;

    private View mLayoutHead;
    private View mBtnClose;
    private TextView mTxtDuration;
    private TextView mTxtName;
    private AudioSegmentView mAudioSegmentView;
    private RegionSliderView mRegionSliderView;
    private SliderView mVolumeSliderView;
    private SliderView mTempoSliderView;

    private EditorPlayerListener  mEditorPlayerListener;
    private OnRegionChangeListener mOnRegionChangeListener;

    public PlayerEditorView(Context context) {
        super(context);
        doInit(context);
    }

    public PlayerEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInit(context);
    }

    private void doInit(Context context) {
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.player_editor_view, this);


        mLayoutHead = rootView.findViewById(R.id.layHead);

        mBtnClose = rootView.findViewById(R.id.imvClose);
        mTxtDuration = (TextView) rootView.findViewById(R.id.txtDuration);
        mTxtName = (TextView) rootView.findViewById(R.id.txtName);
        mAudioSegmentView = (AudioSegmentView) rootView.findViewById(R.id.audioSegment);
        mRegionSliderView = (RegionSliderView) rootView.findViewById(R.id.regionSlider);
        mVolumeSliderView = (SliderView) rootView.findViewById(R.id.volumeSlider);
        mTempoSliderView = (SliderView) rootView.findViewById(R.id.tempoSlider);

        mAudioSegmentView.setOnSegmentChangeListener(new AudioSegmentListener());
        mRegionSliderView.setOnRegionChangeListener(new RegionSliderListener());
        mVolumeSliderView.setOnChangeListener(new VolumeSliderListener());
        mTempoSliderView.setOnChangeListener(new TempoSliderListener());

        mEditorPlayerListener = new EditorPlayerListener();
    }

    public void hideExtra() {
        mLayoutHead.setVisibility(GONE);
        mTempoSliderView.setVisibility(GONE);
    }

    public void setOnRegionChangeListener(OnRegionChangeListener listener) {
        mOnRegionChangeListener = listener;
    }

    public void setOnCloseListener(View.OnClickListener listener) {
        mBtnClose.setOnClickListener(listener);
    }

    public void setPlayer(Player player) {
        if(mPlayer != null) mPlayer.removePlayerListener(mEditorPlayerListener);
        mPlayer = player;
        if(player == null) return;

        mPlayer.addPlayerListener(mEditorPlayerListener);
        showDuration();
        String nameLabel = getResources().getString(R.string.editing_of, mPlayer.getName());
        mTxtName.setText(nameLabel);

        mVolumeSliderView.setValue(mPlayer.getVolume());
        mTempoSliderView.setValue(mPlayer.getTempo());
        mAudioSegmentView.setHead(mPlayer.getHead());
        mRegionSliderView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
        mAudioSegmentView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());

        mPlayer.loadGraphBitmap();
    }

    private void showDuration() {
        if(mPlayer == null) return;
        mTxtDuration.setText(String.format("%.2f", mPlayer.getDurationInSeconds()));
    }

    private class TempoSliderListener extends SliderView.OnChangeListener {
        @Override
        public void onChange(float value, boolean ongoing) {
            if (mPlayer == null) return;
            mPlayer.setTempo(value, !ongoing);
            showDuration();
        }
    }

    private class VolumeSliderListener extends SliderView.OnChangeListener {
        @Override
        public void onChange(float value, boolean ongoing) {
            if (mPlayer == null) return;
            mPlayer.setVolume(value, !ongoing);
        }
    }

    private class RegionSliderListener extends RegionSliderView.OnRegionChangeListener {
        @Override
        public void onRegionChange(float left, float right, boolean ongoing) {
            if (mPlayer == null) return;
            mPlayer.setRegion(left, right, !ongoing);
            mAudioSegmentView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
            showDuration();
            if(mOnRegionChangeListener != null) mOnRegionChangeListener.onRegionChange();
        }
    }

    private class AudioSegmentListener extends AudioSegmentView.OnSegmentChangeListener {
        @Override
        public void onRegionChange(float left, float right, boolean ongoing) {
            if (mPlayer == null) return;
            mPlayer.setRegion(left, right, !ongoing);
            mRegionSliderView.setRegion(mPlayer.getRegionLeft(), mPlayer.getRegionRight());
            showDuration();
            if(mOnRegionChangeListener != null) mOnRegionChangeListener.onRegionChange();
        }

        @Override
        public void onHeadChange(float head) {
            if (mPlayer == null) return;
            mPlayer.setHead(head);
        }
    }

    private class EditorPlayerListener extends PlayerListener {
        @Override
        public void onProgress(float head) {
            mAudioSegmentView.setHead(head);
        }

        @Override
        public void onGraphLoad(Bitmap graphBitmap) {
            mAudioSegmentView.setBackgroundBitmap(graphBitmap);
        }
    }

    public static abstract class OnRegionChangeListener {
        public abstract void onRegionChange();
    }
}
