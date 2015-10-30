package bk2suz.loomus;

import android.content.Context;
import android.content.res.TypedArray;
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
public class SliderView extends View   {
    private static float BoxSizeFraction = 0.1F;
    private static Paint sBoxBorderPaint = new Paint();


    static {
        sBoxBorderPaint.setStyle(Paint.Style.STROKE);
        sBoxBorderPaint.setColor(Color.parseColor("#55000000"));
        sBoxBorderPaint.setStrokeWidth(2);
        sBoxBorderPaint.setAntiAlias(true);
    }

    private Paint mBoxFillPaint = new Paint();
    private Paint mSlopeFillPaint = new Paint();

    private float mMinValue = 0;
    private float mMaxValue = 1;
    private float mValue = 0.5F;
    private float mWidth, mHeight;
    private RectF mBox = new RectF();
    private OnChangeListener mOnChangeListener;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mValue = event.getX()/(mWidth-mBox.width());
                if(mValue<0) mValue = 0F;
                else if (mValue>1F) mValue=1F;
                buildBox();
                if(mOnChangeListener != null) mOnChangeListener.onChange(mValue, true);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(mOnChangeListener != null) mOnChangeListener.onChange(mValue, false);
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

        canvas.drawPath(slopePath, mSlopeFillPaint);
        canvas.drawRoundRect(mBox, 5, 5, mBoxFillPaint);
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

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
    }

    public void setValue(float value) {
        mValue = value;
        if(mValue<mMinValue) mValue = mMinValue;
        else if(mValue>mMaxValue) mValue = mMaxValue;
        buildBox();
        invalidate();
    }

    private void doInit(Context context, AttributeSet attrs) {
        int indicatorColor = Color.parseColor("#55ffffff");
        int spanColor = Color.parseColor("#a7ad17");
        if(attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderView);
            indicatorColor = a.getColor(R.styleable.SliderView_indicatorColor, indicatorColor);
            spanColor = a.getColor(R.styleable.SliderView_spanColor, spanColor);
            mMaxValue = a.getFloat(R.styleable.SliderView_maxValue, mMaxValue);
            mMinValue = a.getFloat(R.styleable.SliderView_minValue, mMinValue);
        }
        mBoxFillPaint.setStyle(Paint.Style.FILL);
        mBoxFillPaint.setColor((indicatorColor));

        mSlopeFillPaint.setStyle(Paint.Style.FILL);
        mSlopeFillPaint.setDither(true);
        mSlopeFillPaint.setAntiAlias(true);
        mSlopeFillPaint.setColor(spanColor);

        buildBox();
    }

    public SliderView(Context context) {
        super(context);
        doInit(context, null);
    }

    public SliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInit(context, attrs);
    }

    public SliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        doInit(context, attrs);
    }

    public SliderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        doInit(context, attrs);
    }

    public static abstract class OnChangeListener {
        public abstract void onChange(float value, boolean ongoing);
    }
}
