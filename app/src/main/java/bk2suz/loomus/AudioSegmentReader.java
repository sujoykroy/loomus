package bk2suz.loomus;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by sujoy on 1/11/15.
 */
public class AudioSegmentReader {
    private final AudioSegmentRecord mRecord;
    private final long mStartFromInByte;
    private final long mEndToInByte;
    private long mCurrentPositionInByte;
    private SkipFixedInputStream mInputStream;

    public final float mVolume;
    public final float mTempo;

    public AudioSegmentReader(AudioSegmentRecord record) {
        mRecord = record;
        mStartFromInByte = mRecord.getStartFromInByte();
        mEndToInByte = mRecord.getEndToInByte();
        mCurrentPositionInByte =  mStartFromInByte;
        mTempo = mRecord.getTempo();
        mVolume = Player.sVolume * mRecord.getVolume();
        try {
            mInputStream = new SkipFixedInputStream(new FileInputStream(mRecord.getAudioFile()));
        } catch (FileNotFoundException e) {
            return;
        }
        mInputStream.mark((int) mRecord.getLengthInByte() + 1);
        try {
            mInputStream.skipSured(mStartFromInByte);
        } catch (IOException e) {}
    }


    public short[] getShorts(int shortBufferSize) {
        short[] finalShortBuffer = new short[shortBufferSize];

        int byteBufferSize = (int) Math.floor(shortBufferSize*2*mTempo);
        if(byteBufferSize%2==1) byteBufferSize -= 1;
        if(byteBufferSize <2) byteBufferSize = 2;
        byte[] byteBuffer = new byte[byteBufferSize];
        short[] shortBuffer = new short[byteBufferSize/2];

        int totalByteReadCount = 0;
        int totalShortReadCount = 0;
        int byteReadCount;
        int zeroCount = 0;//safety
        while(totalShortReadCount<shortBufferSize && zeroCount<2) {
            try {
                byteReadCount = mInputStream.read(byteBuffer, totalByteReadCount, byteBuffer.length - totalByteReadCount);
            } catch (IOException e) {
                break;
            }
            totalByteReadCount += byteReadCount;
            if(byteReadCount<=0) {
                zeroCount++;
            } else {
                zeroCount = 0;
            }
            mCurrentPositionInByte += byteReadCount;
            if (mCurrentPositionInByte>=mEndToInByte) {
                try {
                    mInputStream.reset();
                    mInputStream.skipSured(mStartFromInByte);
                    mCurrentPositionInByte = mStartFromInByte;
                } catch (IOException e) {
                    break;
                }
            }
            ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);
            for(float i=0; i<shortBuffer.length; i+=mTempo) {
                int pos = (int) Math.floor(i);
                finalShortBuffer[totalShortReadCount] = shortBuffer[pos];
                totalShortReadCount++;
                if(totalShortReadCount>=shortBufferSize) {
                    break;
                };
            }

        }
        return finalShortBuffer;
    }

    public void clear() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {}
        }
        mInputStream = null;
    }
}
