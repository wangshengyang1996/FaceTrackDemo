package com.wsy.facetrackdemo.previewutil;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKFace;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wsy9057
 * @date 2018/5/2
 */

public class FaceCameraHelper implements Camera.PreviewCallback {
    private Camera mCamera;
    private int mCameraId;
    private AFT_FSDKEngine ftEngine;
    private SurfaceView surfaceViewPreview, surfaceViewRect;
    private Activity activity;
    private Camera.Size previewSize;
    private int surfaceWidth, surfaceHeight;
    private int cameraOrientation = 0;
    private int faceRectColor = Color.YELLOW;
    private int faceRectThickness = 5;
    private List<AFT_FSDKFace> ftFaceList = new ArrayList<>();
    private Integer specificCameraId = null;
    public interface FaceTrackListener {

        /**
         * 回传相机预览数据和人脸框位置
         *
         * @param nv21 相机预览数据
         * @param ftFaceList 待处理的人脸列表
         */
        void onPreviewData(byte[] nv21, List<AFT_FSDKFace> ftFaceList);


        /**
         * 当出现异常时执行
         *
         * @param e 异常信息
         */
        void onFail(Exception e);


        /**
         * 当相机打开时执行
         *
         * @param camera 相机实例
         */
        void onCameraOpened(Camera camera);

        /**
         * 根据自己的需要可以删除部分人脸，比如指定区域、留下最大人脸等
         *
         * @param ftFaceList 人脸列表
         */
        void adjustFaceRectList(List<AFT_FSDKFace> ftFaceList);
    }

    /**
     * 设置人脸框的颜色
     * @param faceRectColor 人脸框的颜色
     */
    public void setFaceRectColor(@ColorInt int faceRectColor) {
        this.faceRectColor = faceRectColor;
    }

    /**
     * 设置指定的相机ID
     * @param specificCameraId 指定的相机ID
     */
    public void setSpecificCameraId(int specificCameraId) {
        this.specificCameraId = specificCameraId;
    }

    /**
     * 设置绘制的人脸框宽度
     * @param faceRectThickness  人脸框宽度
     */
    public void setFaceRectThickness(int faceRectThickness) {
        this.faceRectThickness = faceRectThickness;
    }

    private FaceTrackListener faceTrackListener;


    public void setFaceTrackListener(FaceTrackListener faceTrackListener) {
        this.faceTrackListener = faceTrackListener;
    }

    public FaceCameraHelper(WeakReference<Activity> activity, AFT_FSDKEngine ftEngine) {
        if (ftEngine == null) {
            throw new RuntimeException("ftEngine is null");
        }
        this.ftEngine = ftEngine;
        this.activity = activity.get();
    }

    public void init(@NonNull SurfaceView surfaceViewPreview, @Nullable SurfaceView surfaceViewRect) {
        this.surfaceViewPreview = surfaceViewPreview;
        this.surfaceViewRect = surfaceViewRect;
        this.surfaceViewPreview.getHolder().addCallback(mPreviewSurfaceCallBack);

        if (this.surfaceViewRect != null) {
            this.surfaceViewRect.setZOrderMediaOverlay(true);
            this.surfaceViewRect.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        }
    }


    public void start() {
        //相机数量为2则打开1,1则打开0,相机ID 1为前置，0为后置
        mCameraId = Camera.getNumberOfCameras() - 1;
        if (specificCameraId != null) {
            mCameraId = specificCameraId;
        }
        //没有相机
        if (mCameraId == -1) {
            if (faceTrackListener != null) {
                faceTrackListener.onFail(new RuntimeException("camera not found"));
            }
            return;
        }
        mCamera = Camera.open(mCameraId);
        cameraOrientation = getCameraOri();
        mCamera.setDisplayOrientation(cameraOrientation);
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            surfaceWidth = metrics.widthPixels;
            surfaceHeight = metrics.heightPixels;
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            previewSize = getBestSupportedSize(parameters.getSupportedPreviewSizes(), metrics);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(surfaceViewPreview.getHolder());
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            if (faceTrackListener != null) {
                faceTrackListener.onCameraOpened(mCamera);
            }
        } catch (IOException e) {
            if (faceTrackListener != null) {
                faceTrackListener.onFail(e);
            }
        }
    }

    public void stop() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        try {
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewDisplay(null);
            mCamera = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 一般情况下
     * landscape 0
     * portrait 90
     * reverseLandscape 180
     * reversePortrait 90
     *
     * @return  相机预览数据的展示旋转角度
     */
    private int getCameraOri() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }


    private Camera.Size getBestSupportedSize(List<Camera.Size> sizes, DisplayMetrics metrics) {
        Camera.Size bestSize = sizes.get(0);
        float screenRatio = (float) metrics.widthPixels / (float) metrics.heightPixels;
        if (screenRatio > 1) {
            screenRatio = 1 / screenRatio;
        }

        for (Camera.Size s : sizes) {
            if (Math.abs((s.height / (float) s.width) - screenRatio) < Math.abs(bestSize.height / (float) bestSize.width - screenRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    @Override
    public void onPreviewFrame(byte[] nv21, Camera camera) {
        if (faceTrackListener != null) {
            ftFaceList.clear();
            int ftCode = ftEngine.AFT_FSDK_FaceFeatureDetect(nv21, previewSize.width, previewSize.height, AFT_FSDKEngine.CP_PAF_NV21, ftFaceList).getCode();
            if (ftCode != 0) {
                faceTrackListener.onFail(new Exception("ft failed,code is " + ftCode));
            }
            faceTrackListener.adjustFaceRectList(ftFaceList);
            if (surfaceViewRect != null) {
                Canvas canvas = surfaceViewRect.getHolder().lockCanvas();
                if (canvas == null){
                    faceTrackListener.onFail(new Exception("can not get canvas of surfaceViewRect"));
                    return;
                }
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                if (ftFaceList.size() > 0) {
                    for (AFT_FSDKFace ftFace : ftFaceList) {
                        Rect adjustedRect = DrawUtil.adjustRect(new Rect(ftFace.getRect()), previewSize.width, previewSize.height, surfaceWidth, surfaceHeight, cameraOrientation, mCameraId);
                        DrawUtil.drawFaceRect(canvas, adjustedRect, faceRectColor, faceRectThickness);
                    }
                }
                surfaceViewRect.getHolder().unlockCanvasAndPost(canvas);
            }

            faceTrackListener.onPreviewData(nv21, ftFaceList);
        }
    }

    private SurfaceHolder.Callback mPreviewSurfaceCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if (ftEngine != null) {
                start();
            } else if (faceTrackListener != null) {
                faceTrackListener.onFail(new RuntimeException("ftEngine is null"));
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            surfaceWidth = width;
            surfaceHeight = height;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            stop();
        }
    };

}