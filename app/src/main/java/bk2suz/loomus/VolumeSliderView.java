package bk2suz.loomus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by sujoy on 20/9/15.
 */
public class VolumeSliderView extends View   {
    private static float BoxSizeFraction = 0.1F;
    private static Paint sBoxBorderPaint = new Paint();
    private static Paint sBoxFillPaint = new Paint();
    private static Paint sSlopeFillPaint = new Paint();


    static {
        sBoxBorderPaint.setStyle(Paint.Style.STROKE);
        sBoxBorderPaint.setColor(Color.parseColor("#55000000"));
        sBoxBorderPaint.setStrokeWidth(2);
        sBoxBorderPaint.setAntiAlias(true);

        sBoxFillPaint.setStyle(Paint.Style.FILL);
        sBoxFillPaint.setColor(Color.parseColor("#55ffffff"));

        sSlopeFillPaint.setStyle(Paint.Style.FILL);
        sSlopeFillPaint.setDither(true);
        sSlopeFillPaint.setAntiAlias(true);
        sSlopeFillPaint.setColor(Color.parseColor("#a7ad17"));
    }

    private float mValue = 0.5F;
    private float mWidth, mHeight;
    private RectF mBox = new RectF();
    private OnVolumeChangeListener mOnVolumeChangeListener;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mValue = event.getX()/(mWidth-mBox.width());
                if(mValue<0) mValue = 0F;
                else if (mValue>1F) mValue=1F;
                buildBox();
                if(mOnVolumeChangeListener != null) mOnVolumeChangeListener.onVolumeChange(mValue, true);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(mOnVolumeChangeListener != null) mOnVolumeChangeListener.onVolumeChange(mValue, false);
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path slopePath = new Path();
        slopePath.moveTo(mBox.width(), mBox.bottom);
        slopePath.lineTo(mWidth, 0);
        slopePath.lineTo(mWidth,mBox.bottom);
        slopePath.close();

        canvas.drawPath(slopePath, sSlopeFillPaint);
        canvas.drawRoundRect(mBox, 5, 5, sBoxFillPaint);
        canvas.drawRoundRect(mBox, 5, 5, sBoxBorderPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        buildBox();
    }

    private void buildBox() {
        mBox.right = mWidth*(BoxSizeFraction + mValue*(1-BoxSizeFraction));
        mBox.left = mBox.right - mWidth*BoxSizeFraction;
        mBox.top = 0;
        mBox.bottom = mHeight-sBoxBorderPaint.getStrokeWidth();
    }

    public void setOnVolumeChangeListener(OnVolumeChangeListener onVolumeChangeListener) {
        mOnVolumeChangeListener = onVolumeChangeListener;
    }

    public void setValue(float value) {
        mValue = value;
        if(mValue<0) mValue = 0F;
        else if(mValue>1F) mValue = 1F;
        buildBox();
        invalidate();
    }

    private void doInit(Context context) {
        buildBox();
    }

    public VolumeSliderView(Context context) {
        super(context);
        doInit(context);
    }

    public VolumeSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInit(context);
    }

    public VolumeSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        doInit(context);
    }

    public VolumeSliderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        doInit(context);
    }

    public static abstract class OnVolumeChangeListener {
        public abstract void onVolumeChange (float volume, boolean ongoing);
    }
}
