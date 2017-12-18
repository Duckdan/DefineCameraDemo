package study.com.cameralibrary.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2017/12/8.
 */

/**
 * 自定义进度条，用于相机录视频
 */
public class DefineSeekBar extends View {

    //背景圆的画笔
    private Paint circlePaint;
    private Paint progressPaint;
    //控件的宽高
    private int width = -1;
    private int height = -1;
    //圆心
    private float centerX = -1;
    private float centerY = -1;
    //半径
    private float radius = 100;
    //所画圆的外界矩形
    RectF rectF = new RectF(-radius + 6, -radius + 6, radius - 6, radius - 6);
    //起始角度
    private float startAngle = -90;
    //变换角度
    private float sweepAngle = 1;
    //是否开启录像
    boolean flag = false;
    //是否录制了一圈
    static boolean completeFlag = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (sweepAngle <= 360) {
                invalidate();
                handler.sendEmptyMessageDelayed(0, 100);
            } else {
                handler.removeMessages(0);
                flag = false;
                completeFlag = true;
                if (listener != null) {
                    listener.complete(DefineSeekBar.this);
                }
            }
        }
    };
    private OnCompleteListener listener;


    public DefineSeekBar(Context context) {
        this(context, null);
    }

    public DefineSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefineSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint = new Paint();
        circlePaint.setColor(Color.LTGRAY);
        circlePaint.setAntiAlias(true);
        //描边并填充
        circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setColor(Color.GREEN);
        progressPaint.setStrokeWidth(12);
        //描边
        progressPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (width == -1 || height == -1) {
            width = getMeasuredWidth();
            height = getMeasuredHeight();
        }
        if (centerX == -1 || centerY == -1) {
            centerX = width * 1.0f / 2;
            centerY = height * 1.0f / 2;
        }

        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        if (flag) {
            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.drawArc(rectF, startAngle, sweepAngle, false, progressPaint);
            canvas.restore();
            sweepAngle += 2;
        }

    }

    /**
     * 开始录像
     */
    public void startRecorder() {
        flag = true;
        if (handler != null) {
            handler.sendEmptyMessage(0);
        }

    }

    /**
     * 停止录像
     */
    public void stopRecorder() {
        flag = false;
        if (handler != null) {
            sweepAngle = 1;
            handler.removeMessages(0);
            invalidate(); //此处再一次调invalidate()方法是为了去除环状图形
        }
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.listener = listener;
    }

    public interface OnCompleteListener {
        void complete(View view);
    }
}
