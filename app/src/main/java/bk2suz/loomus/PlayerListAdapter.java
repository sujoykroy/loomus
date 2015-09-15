package bk2suz.loomus;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

    public PlayerListAdapter(Context context, ArrayList<AudioSegmentRecord> recordList) {
        try {
            mPlayHead = new Player(null);
        } catch (Exception e) {
            return;
        }
        mPlayHead.addPlayerListener(new PlayHeadPlayerListener());

        mPlayers = new ArrayList<Player>();
        for(AudioSegmentRecord record: recordList) {
            Player player = null;
            try {
                player = new Player(record);
            } catch (Exception e) {
                continue;
            }
            if(mPlayHead.getDurationInByte()<player.getDurationInByte()) {
                mPlayHead.setDurationInByte(player.getDurationInByte());
            }
            mPlayers.add(player);
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
        mPlayers.add(player);
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
        PlayerView playerView;
        if(convertView == null) {
            playerView = new PlayerView(mContext, this);

        } else {
            playerView = (PlayerView) convertView;
        }
        playerView.setPlayer(getItem(position));
        playerView.showHideDeleteButton();
        return playerView;
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
    }
}
