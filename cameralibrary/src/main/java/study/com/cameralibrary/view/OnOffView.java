package study.com.cameralibrary.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import study.com.cameralibrary.R;

/**
 * Created by Administrator on 2017/12/8.
 */

/**
 * 组合控件用于拍摄和录像的触发按钮
 */
public class OnOffView extends FrameLayout {

    private DefineSeekBar dsb;
    private ImageView iv;


    /**
     * 按下的时间
     */
    private long downTime = 0;
    /**
     * 移动的时间
     */
    private long moveTime = 0;
    /**
     * 抬起的时间
     */
    private long upTime = 0;

    //用于标记是否移动
    private boolean isMoved = false;

    private ScaleAnimation backgroundAnimation;
    private ScaleAnimation frontAnimation;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("angle", "===" + isMoved);
            if (isMoved) {
                handler.removeMessages(0);
                return;
            }

            if (longListener != null) {
                longListener.onClick(OnOffView.this);
            }

            iv.startAnimation(backgroundAnimation);
            dsb.startAnimation(frontAnimation);
            dsb.startRecorder();
        }
    };
    private OnClickListener listener;
    private onRecordCompleteListener recordListener;
    private OnLongClickListener longListener;


    public OnOffView(Context context) {
        this(context, null);
    }

    public OnOffView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OnOffView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_on_off_layout, this);
        dsb = (DefineSeekBar) findViewById(R.id.dsb);
        iv = (ImageView) findViewById(R.id.iv);


        backgroundAnimation = new ScaleAnimation(1, 0.5f, 1, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        backgroundAnimation.setInterpolator(new OvershootInterpolator());
        backgroundAnimation.setFillAfter(true);
        backgroundAnimation.setDuration(300);

        frontAnimation = new ScaleAnimation(1, 1.2f, 1, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        frontAnimation.setInterpolator(new OvershootInterpolator());
        frontAnimation.setFillAfter(true);
        frontAnimation.setDuration(300);

        dsb.setOnCompleteListener(new DefineSeekBar.OnCompleteListener() {
            @Override
            public void complete(View view) {
                if (recordListener != null) {
                    recordListener.onRecordComplete(OnOffView.this);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoved = false;
                downTime = System.currentTimeMillis();
                handler.sendEmptyMessageDelayed(0, 1000);
                break;
            case MotionEvent.ACTION_MOVE:
//                moveTime = System.currentTimeMillis();
                //当两者之差大于2000ms时说明当前事件为长按事件
//                if (moveTime - downTime > 1000) {
//
//                }
                break;
            case MotionEvent.ACTION_UP:
                isMoved = true;
                upTime = System.currentTimeMillis();
                //当两者之差小于2000ms时说明当前事件为单击事件
                if (upTime - downTime < 1000) {
                    if (listener != null) {
                        listener.onClick(this);
                    }
                } else {
                    iv.clearAnimation();
                    dsb.clearAnimation();
                    dsb.stopRecorder();
                    //防止录制了一圈之后重复执行onRecordComplete
                    if (recordListener != null && !DefineSeekBar.completeFlag) {
                        recordListener.onRecordComplete(this);
                    }
                    DefineSeekBar.completeFlag = false;

                }
                downTime = 0;
                moveTime = 0;
                upTime = 0;
                break;
        }

        Log.d("angle", "===" + event.getAction());
        return true;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    /**
     * 单击监听
     */
    public interface OnClickListener {
        void onClick(View view);
    }

    public void setOnLongClickListener(OnLongClickListener longListener) {
        this.longListener = longListener;
    }

    /**
     * 长按监听
     */
    public interface OnLongClickListener {
        void onClick(View view);
    }

    public void setOnRecordCompleteListener(onRecordCompleteListener listener) {
        this.recordListener = listener;
    }

    /**
     * 录制完成的监听
     */
    public interface onRecordCompleteListener {
        void onRecordComplete(View view);
    }
}
