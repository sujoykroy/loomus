package bk2suz.loomus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

    private Paint sBackgroundPaint;
    private Paint sPlayHeadPaint;
    private Paint sSidePaint;
    private Paint sSliderPaint;
    private Bitmap mBackgroundBitmap = null;

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
                if(position<0) position = 0F;
                else if(position>1F) position=1F;
                switch (mTouchedSide) {
                    case Left:
                        if (position>=mRightPosition) break;
                        mLeftPosition = position;
                        if(mOnSegmentChangeListener != null) {
                            mOnSegmentChangeListener.onRegionChange(mLeftPosition, mRightPosition, true);
                        }
                        break;
                    case Right:
                        if (position<=mLeftPosition) break;
                        mRightPosition = position;
                        if(mOnSegmentChangeListener != null) {
                            mOnSegmentChangeListener.onRegionChange(mLeftPosition, mRightPosition, true);
                        }
                        break;
                    case Middle:
                        if(position<mLeftPosition || position>mRightPosition) break;
                        mHeadPosition = position; break;
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
        canvas.drawRoundRect(new RectF(0, 0, mWidth, mHeight), 3, 3, sBackgroundPaint);
        if (mBackgroundBitmap != null) {
            canvas.drawBitmap(
                    mBackgroundBitmap,
                    new Rect(0, 0, mBackgroundBitmap.getWidth(), mBackgroundBitmap.getHeight()),
                    new RectF(0, 0, mWidth, mHeight),
                    sBackgroundPaint
            );
        }
        canvas.drawRoundRect(new RectF(
                0, 0,
                mWidth * mLeftPosition + mWidth * (mRightPosition-mLeftPosition) * mHeadPosition, mHeight), 3, 3, sPlayHeadPaint);
        if(mLeftPosition>0) {
            canvas.drawRoundRect(new RectF(
                    0, 0,
                    mWidth * mLeftPosition, mHeight), 3, 3, sSidePaint);
        }
        if(mRightPosition<1F) {
            canvas.drawRoundRect(new RectF(
                    mWidth * mRightPosition, 0,
                    mWidth, mHeight), 3, 3, sSidePaint);
        }
    }

    public void setBackgroundBitmap(Bitmap backgroundBitmap) {
        mBackgroundBitmap = backgroundBitmap;
        invalidate();
    }

    public void setOnSegmentChangeListener(OnSegmentChangeListener listener) {
        mOnSegmentChangeListener = listener;
    }

    private void doInit(Context context) {
        sBackgroundPaint = new Paint();
        sBackgroundPaint.setStyle(Paint.Style.FILL);
        sBackgroundPaint.setColor(Color.parseColor("#5d395f"));

        sPlayHeadPaint = new Paint();
        sPlayHeadPaint.setStyle(Paint.Style.FILL);
        sPlayHeadPaint.setColor(Color.parseColor("#5500a7b7"));

        sSidePaint = new Paint();
        sSidePaint.setStyle(Paint.Style.FILL);
        sSidePaint.setColor(Color.parseColor("#55ffffff"));
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
