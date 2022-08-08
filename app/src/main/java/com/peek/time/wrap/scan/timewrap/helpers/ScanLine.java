package com.peek.time.wrap.scan.timewrap.helpers;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.peek.time.wrap.scan.timewrap.R;


public class ScanLine extends View {
    private Paint paint = new Paint();
    private float mPosY = 1f;
    private boolean runAnimation = false;
    private boolean showLine = false;
    private Handler handler;
    private Runnable refreshRunnable;
    private boolean isGoingDown = true;
    private int DELAY = 0;


    public ScanLine(Context context) {
        super(context);
        init(context);
    }

    public ScanLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScanLine(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paint.setColor(context.getColor(R.color.colorAccent));
        }else {
            paint.setColor(Color.WHITE);
        }
        paint.setStrokeWidth(10.0f);//make sure add stroke width otherwise line not display
        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshView();
            }
        };
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (showLine) {
            canvas.drawLine(0, mPosY, getWidth(), mPosY, paint);
        }
        if (runAnimation) {
            handler.postDelayed(refreshRunnable, DELAY);
        }
    }

    public void startAnimation() {
        runAnimation = true;
        showLine = true;
        this.invalidate();
    }

    public void stopAnimation() {
        runAnimation = false;
        showLine = false;
        reset();
        this.invalidate();
    }

    public void pauseAnimation() {
        runAnimation = false;
        this.invalidate();
    }

    private void reset() {
        mPosY = 1f;
        isGoingDown = true;
        runAnimation = false;
    }

    private void refreshView() {
        //Update new position of the line
        if (isGoingDown) {
            mPosY += Constant.speed/2.8;
            if (mPosY > getHeight()){
                reset();
            }
                this.invalidate();
//            }

        }
    }

}