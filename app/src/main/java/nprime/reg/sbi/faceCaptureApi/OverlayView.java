package nprime.reg.sbi.faceCaptureApi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

public class OverlayView extends View {
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint;
    private Rect mRect;
    private Rect[] mRectArray;
    private static final int TOUCH_TOLERANCE_DP = 24;
    private static final int BACKGROUND = Color.TRANSPARENT;
    private int mTouchTolerance;


    public OverlayView(Context context) {
        super(context);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mCanvas = new Canvas();
        //mRect = new Rect(10,10, 200, 200);
        initPaint();
    }

    public void clear() {
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mBitmap.eraseColor(BACKGROUND);
        mCanvas.setBitmap(mBitmap);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        clear();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BACKGROUND);
        canvas.drawBitmap(mBitmap, 0, 0, null);

        mPaint.setColor(Color.argb(127, 255, 255, 255));
        if (null != mRect) {
            //canvas.drawRect(mRect, mPaint);
            canvas.drawCircle(mRect.centerX(), mRect.centerY(), (float) (1.25 * mRect.width()/2), mPaint);
            Log.i("Test", "x : " + mRect.centerX() + " y : " + mRect.centerY() + " width : " + mRect.width());
        }
        /*if(null != mRectArray && mRectArray.length > 0){
            mPaint.setColor(Color.argb(127, 255, 255, 255));
            for (Rect rect: mRectArray) {
                //canvas.scale(1, -1);
                canvas.drawCircle(rect.centerX(), rect.centerY(), (float) (1.45 * (rect.right - rect.left)/2), mPaint);
                Log.i("Test", "x : " + rect.centerX() + " y : " + rect.centerY());
            }
        }*/
    }

    /**
     * Sets up paint attributes.
     */
    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
        mTouchTolerance = dp2px(TOUCH_TOLERANCE_DP);
    }

    /**
     * Converts dpi units to px
     *
     * @param dp
     * @return
     */
    private int dp2px(int dp) {
        Resources r = getContext().getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return (int) px;
    }

    public void setPaint(Paint paint) {
        this.mPaint = paint;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Rect getRect() {
        return mRect;
    }

    public void setRect(Rect rect) {
        this.mRect = rect;
        this.clear();
    }

    public void setRectArray(Rect[] rectArray){
        this.mRectArray = rectArray;
        this.clear();
    }
}
