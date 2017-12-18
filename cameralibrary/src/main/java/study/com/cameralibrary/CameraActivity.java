package study.com.cameralibrary;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import study.com.cameralibrary.manager.CameraManager;
import study.com.cameralibrary.manager.MediaPlayerManager;
import study.com.cameralibrary.utils.FileUtils;
import study.com.cameralibrary.view.CameraView;
import study.com.cameralibrary.view.OnOffView;

import static android.view.View.VISIBLE;
import static study.com.cameralibrary.R.id.rl_camera;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView tvTexture;
    private TextView tvFlash;
    private ImageView ivFacing;
    private OnOffView oov;
    private TextView tvOk;
    private TextView tvBack;


    private ObjectAnimator leftAnimation, rightAnimation;
    /**
     * 相机管理类
     */
    private CameraManager cameraManager;
    /**
     * player manager 管理播放视频
     */
    private MediaPlayerManager playerManager;

    private RelativeLayout rlCamera;
    /**
     * 当前是拍照还是录像
     */
    private boolean isSupportRecord;
    /**
     * 视频录制地址
     */
    private String recorderPath;
    /**
     * 图片路径
     */
    private String photo;
    /**
     *
     */
    private boolean isPhotoTakingState;
    private Subscription takePhotoSubscription;
    private int widthPixels;
    private CameraView cv;
    /**
     * 是否正在录制
     */
    private boolean isRecording;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //全屏模式，处理状态栏的代码
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        widthPixels = getResources().getDisplayMetrics().widthPixels;

        initViewAndAnimation();
        initListener();
        initCamera();

    }

    /**
     * 初始化控件和动画
     */
    private void initViewAndAnimation() {
        tvTexture = (TextureView) findViewById(R.id.tv_texture);
        rlCamera = (RelativeLayout) findViewById(rl_camera);
        tvFlash = (TextView) findViewById(R.id.tv_flash);
        ivFacing = (ImageView) findViewById(R.id.iv_facing);
        oov = (OnOffView) findViewById(R.id.oov);
        tvOk = (TextView) findViewById(R.id.tv_ok);
        tvBack = (TextView) findViewById(R.id.tv_back);
        cv = (CameraView) findViewById(R.id.cv);


        leftAnimation = ObjectAnimator.ofFloat(tvBack, "translationX", 0f, -widthPixels * 0.3f);
        leftAnimation.setInterpolator(new OvershootInterpolator());
        leftAnimation.setDuration(300);

        rightAnimation = ObjectAnimator.ofFloat(tvOk, "translationX", 0f, widthPixels * 0.3f);
        rightAnimation.setInterpolator(new OvershootInterpolator());
        rightAnimation.setDuration(300);


    }


    /**
     * 初始化事件监听器
     */
    private void initListener() {


        /**
         * 设置
         */
        cv.setOnViewTouchListener(new CameraView.OnViewTouchListener() {
            @Override
            public void handleFocus(float x, float y) {
                cameraManager.handleFocusMetering(x, y);
            }

            @Override
            public void handleZoom(boolean zoom, float oldDist, float newDist) {
                cameraManager.handleZoom(zoom, oldDist, newDist);
            }
        });

        tvFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraManager.changeCameraFlash(tvTexture.getSurfaceTexture(),
                        tvTexture.getWidth(), tvTexture.getHeight());
                setCameraFlashState();
            }
        });

        ivFacing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraManager.changeCameraFacing(CameraActivity.this, tvTexture.getSurfaceTexture(),
                        tvTexture.getWidth(), tvTexture.getHeight());
            }
        });

        oov.setOnClickListener(new OnOffView.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CameraActivity.this, "轻触拍照", Toast.LENGTH_SHORT).show();
                cameraManager.takePhoto(callback);
            }
        });

        oov.setOnLongClickListener(new OnOffView.OnLongClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CameraActivity.this, "长按录像", Toast.LENGTH_SHORT).show();
                isSupportRecord = true;
                cameraManager.setCameraType(1);

                recorderPath = FileUtils.getUploadVideoFile(CameraActivity.this);
                cameraManager.startMediaRecord(recorderPath);

                isRecording = true;
            }
        });


        oov.setOnRecordCompleteListener(new OnOffView.onRecordCompleteListener() {
            @Override
            public void onRecordComplete(View view) {
                endDeal();
                stopRecorder();
            }
        });

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("complete","tvBack===");
                finish();
            }
        });

        tvOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorderPath == null) {
                    Toast.makeText(CameraActivity.this, "拍照成功！", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent();
                    intent.putExtra("path", photo);
                    setResult(200, intent);
                    finish();
                } else {
                    Toast.makeText(CameraActivity.this, "录制成功！", Toast.LENGTH_SHORT).show();

                }
            }
        });

        tvTexture.setSurfaceTextureListener(this);
    }

    /**
     * 设置闪光状态
     */
    private void setCameraFlashState() {
        int flashState = cameraManager.getCameraFlash();
        switch (flashState) {
            case 0: //自动
                tvFlash.setSelected(true);
                tvFlash.setText("自动");
                break;
            case 1://open
                tvFlash.setSelected(true);
                tvFlash.setText("开启");
                break;
            case 2: //close
                tvFlash.setSelected(false);
                tvFlash.setText("关闭");
                break;
        }
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        cameraManager = CameraManager.getInstance(getApplication());
        playerManager = MediaPlayerManager.getInstance(getApplication());
        cameraManager.setCameraType(isSupportRecord ? 1 : 0);
        setCameraFlashState();
        tvFlash.setVisibility(cameraManager.isSupportFlashCamera() ? View.VISIBLE : View.GONE);
        ivFacing.setVisibility(cameraManager.isSupportFrontCamera() ? View.VISIBLE : View.GONE);
        rlCamera.setVisibility(cameraManager.isSupportFlashCamera()
                || cameraManager.isSupportFrontCamera() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTexture.isAvailable()) {
            //路径不为空，优先播放视频
            if (recorderPath != null) {
                playerManager.playMedia(new Surface(tvTexture.getSurfaceTexture()), recorderPath);
            } else {
                cameraManager.openCamera(this, tvTexture.getSurfaceTexture(),
                        tvTexture.getWidth(), tvTexture.getHeight());
            }
        } else {
            tvTexture.setSurfaceTextureListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (takePhotoSubscription != null) {
            takePhotoSubscription.unsubscribe();
        }
        if (isRecording) {
            stopRecorder();
        }
        cameraManager.closeCamera();
        playerManager.stopMedia();
    }

    /**
     * 停止拍摄之后开始播放
     */
    private void stopRecorder() {
        isRecording = false;
        cameraManager.stopMediaRecord();

        if (tvTexture != null && tvTexture.isAvailable() && recorderPath != null) {
            cameraManager.closeCamera();
            playerManager.playMedia(new Surface(tvTexture.getSurfaceTexture()), recorderPath);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        if (recorderPath != null) {
            playerManager.playMedia(new Surface(texture), recorderPath);
        } else {
            cameraManager.openCamera(CameraActivity.this, texture, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    private Camera.PictureCallback callback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            takePhotoSubscription = Observable.
                    create(new Observable.OnSubscribe<Boolean>() {
                        @Override
                        public void call(Subscriber<? super Boolean> subscriber) {
                            if (!subscriber.isUnsubscribed()) {
                                String photoPath = FileUtils.getUploadPhotoFile(CameraActivity.this);
                                //保存拍摄的图片
                                isPhotoTakingState = FileUtils.savePhoto(photoPath, data, cameraManager.isCameraFrontFacing());
                                if (isPhotoTakingState) {
                                    photo = photoPath;
                                }
                                subscriber.onNext(isPhotoTakingState);
                            }
                        }
                    }).
                    subscribeOn(Schedulers.io()).
                    observeOn(AndroidSchedulers.mainThread()).
                    subscribe(new Subscriber<Boolean>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Boolean aBoolean) {
                            if (aBoolean != null && aBoolean) {
                                endDeal();
                            }
                        }
                    });
        }
    };

    /**
     * 拍照完成或者录制视频完成之后调用
     */
    private void endDeal() {
        tvOk.setVisibility(VISIBLE);
        tvBack.setVisibility(VISIBLE);
        ivFacing.setVisibility(View.GONE);
        tvFlash.setVisibility(View.GONE);
        oov.setVisibility(View.GONE);
        cv.setEnabled(false);
        rightAnimation.start();
        leftAnimation.start();
    }
}
