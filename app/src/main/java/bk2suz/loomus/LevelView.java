package bk2suz.loomus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by sujoy on 3/11/15.
 */
public class LevelView extends View {
    private static Paint sFillPaint = new Paint();

    static {
        sFillPaint.setStyle(Paint.Style.FILL);
        sFillPaint.setColor(Color.parseColor("#02bc02"));
    }

    private float mValue = 0;
    private float mWidth, mHeight;

    public void setValue(float value) {
        mValue = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(new RectF(0, 0, mValue*mWidth, mHeight), 5, 5, sFillPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    public LevelView(Context context) {
        super(context);
    }

    public LevelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
