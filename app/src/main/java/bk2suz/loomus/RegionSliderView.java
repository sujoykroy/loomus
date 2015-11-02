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
 * Created by sujoy on 19/9/15.
 */
public class RegionSliderView extends View {
    private static Paint sLeftBoxFillPaint = new Paint();
    private static Paint sRightBoxFillPaint = new Paint();
    private static Paint sBoxBorderPaint = new Paint();
    private static float BoxSizeFraction = .2F;

    private enum TouchedSide {
        None, Left, Right
    }

    static {
        sLeftBoxFillPaint.setStyle(Paint.Style.FILL);
        sLeftBoxFillPaint.setColor(Color.parseColor("#3b1e38"));

        sRightBoxFillPaint.setStyle(Paint.Style.FILL);
        sRightBoxFillPaint.setColor(Color.parseColor("#c77dbf"));

        sBoxBorderPaint.setStrokeWidth(1);
        sBoxBorderPaint.setStyle(Paint.Style.STROKE);
        sBoxBorderPaint.setColor(Color.parseColor("#597de5"));
    }

    private float mLeftFraction;
    private float mRightFraction;

    private RectF mLeftBox;
    private RectF mRightBox;
    private float mWidth = 400, mHeight = 300;

    private TouchedSide mTouchedSide = TouchedSide.None;
    private float mTouchOffsetX;
    private OnRegionChangeListener mOnRegionChangeListener = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(mLeftBox.contains(event.getX(), event.getY())) {
                    mTouchOffsetX = event.getX()-mLeftBox.left;
                    mTouchedSide = TouchedSide.Left;
                } else if (mRightBox.contains(event.getX(), event.getY())) {
                    mTouchOffsetX = -(mRightBox.right-event.getX());
                    mTouchedSide = TouchedSide.Right;
                } else {
                    mTouchedSide = TouchedSide.None;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(mTouchedSide == TouchedSide.None) break;
                float position = (event.getX()-mTouchOffsetX)/mWidth;
                if(position<0) position = 0F;
                else if(position>1F) position = 1F;
                switch (mTouchedSide) {
                    case Left:
                        if(position>mRightFraction) break;
                        mLeftFraction = position;
                        buildBox();
                        if(mOnRegionChangeListener != null) {
                            mOnRegionChangeListener.onRegionChange(mLeftFraction, mRightFraction, true);
                        }
                        break;
                    case Right:
                        if(position<mLeftFraction) break;
                        mRightFraction = position;
                        buildBox();
                        if(mOnRegionChangeListener != null) {
                            mOnRegionChangeListener.onRegionChange(mLeftFraction, mRightFraction, true);
                        }
                        break;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(mTouchedSide != TouchedSide.None && mOnRegionChangeListener != null) {
                    mOnRegionChangeListener.onRegionChange(mLeftFraction, mRightFraction, false);
                }
                mTouchedSide = TouchedSide.None;
                break;
        }
        return true;
    }

    public void setRegion(float left, float right) {
        mLeftFraction = left;
        mRightFraction = right;
        buildBox();
        invalidate();
    }

    public void setOnRegionChangeListener(OnRegionChangeListener onRegionChangeListener) {
        mOnRegionChangeListener = onRegionChangeListener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(mLeftBox, 5, 5, sLeftBoxFillPaint);
        canvas.drawRoundRect(mRightBox, 5, 5, sRightBoxFillPaint);

        canvas.drawRoundRect(mLeftBox, 5, 5, sBoxBorderPaint);
        canvas.drawRoundRect(mRightBox, 5, 5, sBoxBorderPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        buildBox();
    }

    private void buildBox() {
        mLeftBox.left = mWidth*mLeftFraction;
        mLeftBox.right = mLeftBox.left + mWidth*BoxSizeFraction;
        mLeftBox.top = 0;
        mLeftBox.bottom = mHeight-sBoxBorderPaint.getStrokeWidth();

        mRightBox.right = mWidth*mRightFraction;
        mRightBox.left = mRightBox.right - mWidth*BoxSizeFraction;
        mRightBox.top = 0;
        mRightBox.bottom = mHeight-sBoxBorderPaint.getStrokeWidth();
    }

    private void doInit(Context context) {

        mLeftFraction = 0F;
        mRightFraction = 1F;

        mLeftBox = new RectF();
        mRightBox = new RectF();
        buildBox();
    }

    public RegionSliderView(Context context) {
        super(context);
        doInit(context);
    }

    public RegionSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInit(context);
    }

    public RegionSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        doInit(context);
    }

    public RegionSliderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        doInit(context);
    }

    public abstract static class OnRegionChangeListener {
        public abstract void onRegionChange(float left, float right, boolean ongoing);
    }
}
