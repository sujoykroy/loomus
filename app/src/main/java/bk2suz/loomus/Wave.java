package bk2suz.loomus;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by sujoy on 1/11/15.
 */
public class Wave {
    private static final int ChunkSizeCount = 4;

    public static byte[] intToDWORD(int value) {
        byte[] bytes = {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff)
        };
        return bytes;
    }

    public static byte[] intToWORD(int value) {
        byte[] bytes = {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)
        };
        return bytes;
    }

    /*
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    */

    public static boolean save(int channel, int sampleRate, int bitsPerSample, File rawInputFile, File outputFile) {
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(rawInputFile));
        } catch (FileNotFoundException e) {
            return false;
        }
        BufferedOutputStream outputStream = null;
        try {
            outputStream  = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            try {
                inputStream.close();
            } catch (IOException e1) {}
            return false;
        }
        byte[] riffBytes = {'R', 'I', 'F', 'F'};
        byte[] waveBytes = {'W', 'A', 'V', 'E'};

        byte[] fmtBytes = {'f', 'm', 't', ' '};
        byte[] formatTagBytes = intToWORD(1);
        byte[] channelBytes = intToWORD(channel);
        byte[] samplePerSecBytes = intToDWORD(sampleRate);
        byte[] avgBytePerSecBytes = intToDWORD((int) Math.ceil(channel * sampleRate * bitsPerSample / 8f));
        byte[] blockAlignBytes = intToWORD((int) Math.ceil(channel * bitsPerSample / 8f));
        byte[] bitsPerSampleBytes = intToWORD(bitsPerSample);

        int fmtChunkSize =  formatTagBytes.length +
                            channelBytes.length +
                            samplePerSecBytes.length +
                            avgBytePerSecBytes.length +
                            blockAlignBytes.length +
                            bitsPerSampleBytes.length;
        byte[] fmtChunkSizeBytes = intToDWORD(fmtChunkSize);


        byte[] dataBytes = {'d', 'a', 't', 'a'};

        int dataChunkSize = (int) rawInputFile.length();
        byte[] dataChunkSizeBytes = intToDWORD(dataChunkSize);

        int riffChunkSize = waveBytes.length +
                            fmtBytes.length +  fmtChunkSizeBytes.length + fmtChunkSize +
                            dataBytes.length + dataChunkSizeBytes.length + dataChunkSize;
        byte[] riffChunkSizeBytes = intToDWORD(riffChunkSize);

        boolean error = false;
        try {
            outputStream.write(riffBytes, 0, riffBytes.length);
            outputStream.write(riffChunkSizeBytes, 0, riffChunkSizeBytes.length);
            outputStream.write(waveBytes, 0, waveBytes.length);

            outputStream.write(fmtBytes, 0, fmtBytes.length);
            outputStream.write(fmtChunkSizeBytes, 0, fmtChunkSizeBytes.length);
            outputStream.write(formatTagBytes, 0, formatTagBytes.length);
            outputStream.write(channelBytes, 0, channelBytes.length);
            outputStream.write(samplePerSecBytes, 0, samplePerSecBytes.length);
            outputStream.write(avgBytePerSecBytes, 0, avgBytePerSecBytes.length);
            outputStream.write(blockAlignBytes, 0, blockAlignBytes.length);
            outputStream.write(bitsPerSampleBytes, 0, bitsPerSampleBytes.length);

            outputStream.write(dataBytes, 0, dataBytes.length);
            outputStream.write(dataChunkSizeBytes, 0, dataChunkSizeBytes.length);
            outputStream.flush();
            byte[] buffer = new byte[1024];
            int readCount;
            while((readCount=inputStream.read(buffer, 0, buffer.length))>0) {
                outputStream.write(buffer, 0, readCount);
                outputStream.flush();
            }
        } catch (IOException e) {
            error = true;
        }
        return !error;
    }
}
