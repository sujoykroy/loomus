package bk2suz.loomus;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by sujoy on 15/9/15.
 */
public class PlayerListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Player> mPlayers;

    private Player mPlayHead;
    private PlayerRegionChangeListener mPlayerRegionChangeListener;
    private View.OnLongClickListener mOnLongClickListener;

    public PlayerListAdapter(Context context, ArrayList<AudioSegmentRecord> recordList,
                             View.OnLongClickListener onLongClickListener) {
        try {
            mPlayHead = new Player(null);
        } catch (Exception e) {
            return;
        }
        mPlayHead.setIsEnabled(true);
        mPlayHead.addPlayerListener(new PlayHeadPlayerListener());

        mOnLongClickListener = onLongClickListener;
        mPlayerRegionChangeListener  = new PlayerRegionChangeListener();

        mPlayers = new ArrayList<Player>();
        for(AudioSegmentRecord record: recordList) {
            Player player = null;
            try {
                player = new Player(record);
            } catch (Exception e) {
                continue;
            }
            mPlayerRegionChangeListener.OnRegionChange(player);
            mPlayers.add(player);
            player.addOnRegionChangeListener(mPlayerRegionChangeListener);
        }
        mContext = context;
    }

    public void addNew(File file) {
        Player player = null;
        try {
            player = new Player(new AudioSegmentRecord(file));
        } catch (Exception e) {
            return;
        }
        if(mPlayHead.getDurationInByte()<player.getDurationInByte()) {
            mPlayHead.setDurationInByte(player.getDurationInByte());
        }
        mPlayerRegionChangeListener.OnRegionChange(player);
        mPlayers.add(player);
        player.addOnRegionChangeListener(mPlayerRegionChangeListener);
        notifyDataSetChanged();
    }

    public void play() {
        mPlayHead.play();
        for(Player player: mPlayers) player.play();
    }

    public void pause() {
        mPlayHead.pause();
        for(Player player: mPlayers) player.pause();
    }

    public void reset() {
        mPlayHead.seek(0);
        for(Player player: mPlayers) player.seek(0);
    }

    @Override
    public int getCount() {
        return mPlayers.size();
    }

    @Override
    public Player getItem(int position) {
        return mPlayers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlayerItemView playerItemView;
        if(convertView == null) {
            playerItemView = new PlayerItemView(mContext, this);
            playerItemView.setLongClickable(true);
            playerItemView.setOnLongClickListener(mOnLongClickListener);

        } else {
            playerItemView = (PlayerItemView) convertView;
        }
        playerItemView.setPlayer(getItem(position));
        playerItemView.showHideDeleteButton();
        return playerItemView;
    }

    public void clear() {
        for(Player player: mPlayers) {
            player.cleanIt();
        }
        mPlayHead.cleanIt();
    }

    public void toggleDelete() {
        for(Player player: mPlayers) {
            player.toggleDeletable();
        }
        notifyDataSetChanged();
    }

    public Player getPlayHead() {
        return mPlayHead;
    }

    public void delete(Player player) {
        mPlayers.remove(player);
        notifyDataSetChanged();
        player.delete();
    }

    private class PlayHeadPlayerListener extends PlayerListener {
        @Override
        public void onError(String message) {}

        @Override
        public void onPlay() {}

        @Override
        public void onPause() {}

        @Override
        public void onSeek() {}

        @Override
        public void onProgress(float head) {
            if(head>=1F) {
                reset();
            }
        }

        @Override
        public void onGraphLoad(Bitmap graphBitmap) {}
    }

    private class PlayerRegionChangeListener extends Player.OnRegionChangeListener {
        @Override
        public void OnRegionChange(Player player) {
            if (mPlayHead.getDurationInByte() < player.getDurationInByte()) {
                mPlayHead.setDurationInByte(player.getDurationInByte());
            }
        }
    }
}
