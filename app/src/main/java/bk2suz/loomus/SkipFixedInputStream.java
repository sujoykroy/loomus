package bk2suz.loomus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sujoy on 31/10/15.
 */
public class SkipFixedInputStream extends BufferedInputStream {
    public SkipFixedInputStream(InputStream in) {
        super(in);
    }

    public SkipFixedInputStream(InputStream in, int size) {
        super(in, size);
    }

    public synchronized long skipSured(long byteCount) throws IOException {
        int loopLimit = 1000;
        int loopCount = 0;
        int skipAmount = 0;
        while(skipAmount<byteCount && loopCount<loopLimit) {
            loopCount++;
            skipAmount += skip(byteCount-skipAmount);
        }
        return skipAmount;
    }
}
