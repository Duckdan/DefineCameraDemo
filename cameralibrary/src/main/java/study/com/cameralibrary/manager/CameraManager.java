package study.com.cameralibrary.manager;

/**
 * Created by Administrator on 2017/12/11.
 */

import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import study.com.cameralibrary.utils.CameraUtils;
import study.com.cameralibrary.utils.LogUtils;

/**
 * 相机的管理类
 */
public class CameraManager {
    private static CameraManager instance;
    private final Application context;
    /**
     * camera
     */
    private Camera mCamera;
    /**
     * 视频录制
     */
    private MediaRecorder mMediaRecorder;
    /**
     * 相机闪光状态
     */
    private int cameraFlash;
    /**
     * 前后置状态
     */
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    /**
     * 是否支持前置摄像,是否支持闪光
     */
    private boolean isSupportFrontCamera, isSupportFlashCamera;
    /**
     * 录制视频的相关参数
     */
    private CamcorderProfile mProfile;
    /**
     * 0为拍照, 1为录像
     */
    private int cameraType;

    private CameraManager(Application context) {
        this.context = context;
        isSupportFrontCamera = CameraUtils.isSupportFrontCamera();
        isSupportFlashCamera = CameraUtils.isSupportFlashCamera(context);
        if (isSupportFrontCamera) {
            cameraFacing = CameraUtils.getCameraFacing(context, Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (isSupportFlashCamera) {
            cameraFlash = CameraUtils.getCameraFlash(context);
        }
    }

    /**
     * 获取相机管理者的实例
     *
     * @param context
     * @return
     */
    public synchronized static CameraManager getInstance(Application context) {
        if (instance == null) {
            instance = new CameraManager(context);
        }
        return instance;
    }

    public boolean isSupportFrontCamera() {
        return isSupportFrontCamera;
    }

    public boolean isSupportFlashCamera() {
        return isSupportFlashCamera;
    }

    public boolean isCameraFrontFacing() {
        return cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public int getCameraFlash() {
        return cameraFlash;
    }


    /**
     * 打开camera
     */
    public void openCamera(Context context, SurfaceTexture surfaceTexture, int width, int height) {
        if (mCamera == null) {
            try {
                mCamera = Camera.open(cameraFacing);//打开当前选中的摄像头
                //为了录制视频的时候调用
                mProfile = CamcorderProfile.get(cameraFacing, CamcorderProfile.QUALITY_HIGH);
                mCamera.setDisplayOrientation(90);//默认竖直拍照
                mCamera.setPreviewTexture(surfaceTexture);
                initCameraParameters(cameraFacing, width, height);
                mCamera.startPreview();
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
    }


    /**
     * 初始化相机参数
     *
     * @param cameraId
     * @param width
     * @param height
     */
    private void initCameraParameters(int cameraId, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null) {
                if (cameraType == 0) {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                } else {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                }
            }
        }
        parameters.setRotation(90);//设置旋转代码,
        switch (cameraFlash) {
            case 0:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            case 1:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                break;
            case 2:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
        }
        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        if (!isEmpty(pictureSizes) && !isEmpty(previewSizes)) {
            /*for (Camera.Size size : pictureSizes) {
                LogUtils.i("pictureSize " + size.width + "  " + size.height);
            }
            for (Camera.Size size : pictureSizes) {
                LogUtils.i("previewSize " + size.width + "  " + size.height);
            }*/
            Camera.Size optimalPicSize = getOptimalSize(pictureSizes, width, height);
            Camera.Size optimalPreSize = getOptimalSize(previewSizes, width, height);
            parameters.setPictureSize(optimalPicSize.width, optimalPicSize.height);
            parameters.setPreviewSize(optimalPreSize.width, optimalPreSize.height);
            mProfile.videoFrameWidth = optimalPreSize.width;
            mProfile.videoFrameHeight = optimalPreSize.height;
            mProfile.videoBitRate = 5000000;//此参数主要决定视频拍出大小
        }
        mCamera.setParameters(parameters);
    }

    /**
     * 缩放
     *
     * @param isZoomIn
     * @param oldDist
     * @param newDist
     */
    public void handleZoom(boolean isZoomIn, float oldDist, float newDist) {
        if (mCamera == null) return;
        Camera.Parameters params = mCamera.getParameters();
        if (params == null) return;
//        if (params.isZoomSupported()) {
//            int maxZoom = params.getMaxZoom();
//            int zoom = params.getZoom();
//            if (isZoomIn && zoom < maxZoom) {
//                zoom++;
//            } else if (zoom > 0) {
//                zoom--;
//            }
//            params.setZoom(zoom);
//            mCamera.setParameters(params);
//        } else {
//            LogUtils.i("zoom not supported");
//        }

        int scale = (int) ((newDist - oldDist) / 10f);
        if (scale >= 1 || scale <= -1) {
            Log.e("TAG", params.getZoom() + "+++");
            int zoom = params.getZoom() + scale;
            Log.e("TAG", zoom + "====" + scale + "," + oldDist + "===" + newDist);
            //zoom不能超出范围
            if (zoom > params.getMaxZoom()) zoom = params.getMaxZoom();
            if (zoom < 0)
                zoom = 0;
            params.setZoom(zoom);
//            mZoomSeekBar.setProgress(zoom);
            //将最后一次的距离设为当前距离
            oldDist = newDist;
        }
        mCamera.setParameters(params);
    }


    /**
     * 更换前后置摄像
     */
    public void changeCameraFacing(Context context, SurfaceTexture surfaceTexture, int width, int height) {
        if (isSupportFrontCamera) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
                if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) { //现在是后置，变更为前置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位为前置
                        closeCamera();
                        cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                        CameraUtils.setCameraFacing(context, cameraFacing);
                        openCamera(context, surfaceTexture, width, height);
                        break;
                    }
                } else {//现在是前置， 变更为后置
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位
                        closeCamera();
                        cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        CameraUtils.setCameraFacing(context, cameraFacing);
                        openCamera(context, surfaceTexture, width, height);
                        break;
                    }
                }
            }
        } else { //不支持摄像机
            Toast.makeText(context, "您的手机不支持前置摄像", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 集合不为空
     *
     * @param list
     * @param <E>
     * @return
     */
    private <E> boolean isEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 获取最佳预览相机Size参数
     *
     * @return
     */
    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        Camera.Size optimalSize = null;
        float targetRadio = h / (float) w;
        float optimalDif = Float.MAX_VALUE; //最匹配的比例
        int optimalMaxDif = Integer.MAX_VALUE;//最优的最大值差距
        for (Camera.Size size : sizes) {
            float newOptimal = size.width / (float) size.height;
            float newDiff = Math.abs(newOptimal - targetRadio);
            if (newDiff < optimalDif) { //更好的尺寸
                optimalDif = newDiff;
                optimalSize = size;
                optimalMaxDif = Math.abs(h - size.width);
            } else if (newDiff == optimalDif) {//更好的尺寸
                int newOptimalMaxDif = Math.abs(h - size.width);
                if (newOptimalMaxDif < optimalMaxDif) {
                    optimalDif = newDiff;
                    optimalSize = size;
                    optimalMaxDif = newOptimalMaxDif;
                }
            }
        }
        return optimalSize;
    }

    /**
     * 改变闪光状态
     */
    public void changeCameraFlash(SurfaceTexture surfaceTexture, int width, int height) {
        if (!isSupportFlashCamera) {
            Toast.makeText(context, "您的手机不支闪光", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters != null) {
                int newState = cameraFlash;
                switch (cameraFlash) {
                    case 0: //自动
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        newState = 1;
                        break;
                    case 1://open
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        newState = 2;
                        break;
                    case 2: //close
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        newState = 0;
                        break;
                }
                cameraFlash = newState;
                CameraUtils.setCameraFlash(context, newState);
                mCamera.setParameters(parameters);
            }
        }
    }

    /**
     * 拍照
     */
    public void takePhoto(Camera.PictureCallback callback) {
        if (mCamera != null) {
            try {
                mCamera.takePicture(null, null, callback);
            } catch (Exception e) {
                Toast.makeText(context, "拍摄失败", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * 开始录制视频
     */
    public void startMediaRecord(String savePath) {
        if (mCamera == null || mProfile == null)
            return;
        //录制视频时必须调用，目的是解锁方便其他进程调用
        mCamera.unlock();
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
            //设置视频回放的输出方向，其参数必须是0,90,180,270中的一个
            mMediaRecorder.setOrientationHint(90);
        }
        if (isCameraFrontFacing()) {
            mMediaRecorder.setOrientationHint(270);
        }
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setProfile(mProfile);
        mMediaRecorder.setOutputFile(savePath);
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * 停止录制
     */
    public void stopMediaRecord() {
        this.cameraType = 0;
        //停止录制
        stopRecorder();
        //释放资源
        releaseMediaRecorder();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
                mCamera.lock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 设置对焦类型
     *
     * @param cameraType
     */
    public void setCameraType(int cameraType) {
        this.cameraType = cameraType;
        if (mCamera != null) {//拍摄视频时
            if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null) {
                    if (cameraType == 0) {
                        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        }
                    } else {
                        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        }
                    }
                }
            }
        }
    }

    /**
     * 对焦
     *
     * @param x
     * @param y
     */
    public void handleFocusMetering(float x, float y) {
        //获取相机服务当前的参数
        Camera.Parameters params = mCamera.getParameters();
        //获取预览图像的大小尺寸
        Camera.Size previewSize = params.getPreviewSize();
        //聚焦矩形
        Rect focusRect = calculateTapArea(x, y, 1f, previewSize);
        //测量矩形
        Rect meteringRect = calculateTapArea(x, y, 1.5f, previewSize);
        mCamera.cancelAutoFocus();

        //返回照相机支持的焦点区域的最大数目
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 1000));
            //设置焦点区域
            params.setFocusAreas(focusAreas);
        } else {
            LogUtils.i("focus areas not supported");
        }

        //返回相机支持的计量区域的最大数目
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 1000));
            //设置计量区域
            params.setMeteringAreas(meteringAreas);
        } else {
            LogUtils.i("metering areas not supported");
        }
        //获取聚焦模式
        final String currentFocusMode = params.getFocusMode();
        //设置聚焦模式
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);

        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }

    /**
     * 计算点击的区域
     *
     * @param x           手指触摸的x坐标
     * @param y           手指触摸的y坐标
     * @param coefficient 焦点面积的倍数
     * @param previewSize 图像预览的大小
     * @return
     */
    private Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    /**
     * 限定坐标值大小的范围
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    /**
     * 释放摄像头
     */
    public void closeCamera() {
        this.cameraType = 0;
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
    }

}
