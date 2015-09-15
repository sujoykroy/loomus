package bk2suz.loomus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by sujoy on 15/9/15.
 */
public class AudioSegmentView extends View {
    private enum TouchedSide {
        None, Left, Right, Middle
    }
    private TouchedSide mTouchedSide = TouchedSide.None;

    private float mHeadPosition = 0;
    private float mLeftPosition = 0;
    private float mRightPosition = 1F;

    private Paint mBackgroundPaint;
    private Paint mPlayHeadPaint;
    private Paint mSidePaint;

    private float mWidth, mHeight;
    private OnSegmentChangeListener mOnSegmentChangeListener = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getX();

                if(x<mWidth*.1F || x<=mWidth*mLeftPosition) {
                    mTouchedSide = TouchedSide.Left;
                } else if(x>mWidth*.9F || x>=mWidth*mRightPosition) {
                    mTouchedSide = TouchedSide.Right;
                } else {
                    mTouchedSide = TouchedSide.Middle;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float position = event.getX()/mWidth;
                switch (mTouchedSide) {
                    case Left:
                        mLeftPosition = position;
                        if(mOnSegmentChangeListener != null) {
                            mOnSegmentChangeListener.onRegionChange(mLeftPosition, mRightPosition, true);
                        }
                        break;
                    case Right:
                        mRightPosition = position;
                        if(mOnSegmentChangeListener != null) {
                            mOnSegmentChangeListener.onRegionChange(mLeftPosition, mRightPosition, true);
                        }
                        break;
                    case Middle: mHeadPosition = position; break;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(mOnSegmentChangeListener != null) {
                    switch (mTouchedSide) {
                        case Left:
                        case Right:
                            mOnSegmentChangeListener.onRegionChange(mLeftPosition, mRightPosition, false);
                            break;
                        case Middle:
                            mOnSegmentChangeListener.onHeadChange(mHeadPosition);
                            break;
                    }
                }
                mTouchedSide = TouchedSide.None;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(new RectF(0, 0, mWidth, mHeight), 3, 3, mBackgroundPaint);
        canvas.drawRoundRect(new RectF(
                0, 0,
                mWidth * mHeadPosition, mHeight), 3, 3, mPlayHeadPaint);
        if(mLeftPosition>0) {
            canvas.drawRoundRect(new RectF(
                    0, 0,
                    mWidth * mLeftPosition, mHeight), 3, 3, mSidePaint);
        }
        if(mRightPosition<1F) {
            canvas.drawRoundRect(new RectF(
                    mWidth*mRightPosition, 0,
                    mWidth, mHeight), 3, 3, mSidePaint);
        }
    }

    public void setOnSegmentChangeListener(OnSegmentChangeListener listener) {
        mOnSegmentChangeListener = listener;
    }

    private void doInit(Context context) {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(Color.parseColor("#75cb75"));

        mPlayHeadPaint = new Paint();
        mPlayHeadPaint.setStyle(Paint.Style.FILL);
        mPlayHeadPaint.setColor(Color.parseColor("#00a7b7"));

        mSidePaint = new Paint();
        mSidePaint.setStyle(Paint.Style.FILL);
        mSidePaint.setColor(Color.parseColor("#55ffffff"));
    }

    public void setHead(float value) {
        mHeadPosition = value;
        invalidate();
    }

    public void setRegion(float left, float right) {
        mLeftPosition = left;
        mRightPosition = right;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    public AudioSegmentView(Context context) {
        super(context);
        doInit(context);
    }

    public AudioSegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInit(context);
    }

    public AudioSegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        doInit(context);
    }

    public AudioSegmentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        doInit(context);
    }

    public abstract static class OnSegmentChangeListener {
        public abstract void onRegionChange(float left, float right, boolean ongoing);
        public abstract void onHeadChange(float head);
    }
}
