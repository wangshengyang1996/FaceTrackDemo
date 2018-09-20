package com.wsy.facetrackdemo.previewutil;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.wsy.facetrackdemo.customview.FaceRectView;
import com.wsy.facetrackdemo.model.DrawInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wsy9057
 * @date 2018/5/2
 */

public class FaceCameraHelper implements Camera.PreviewCallback {
    private static final String TAG = "FaceCameraHelper";
    private Camera mCamera;
    private int mCameraId;
    private AFT_FSDKEngine ftEngine;
    private AFR_FSDKEngine frEngine;
    /**
     * 预览显示的view，目前仅支持surfaceView和textureView
     */
    private View previewView;
    /**
     * 自定义的绘制人脸框的view
     */
    private FaceRectView faceRectView;
    private Activity activity;
    private Camera.Size previewSize;
    private int surfaceWidth, surfaceHeight;
    private int cameraOrientation = 0;
    private int faceRectColor = Color.YELLOW;
    private int faceRectThickness = 5;
    /**
     * 是否镜像显示，只支持textureView
     */
    private boolean isMirror = false;

    /**
     * fr 线程数，建议和ft初始化时的maxFaceNum相同
     */
    private int frThreadNum = 5;

    private List<AFT_FSDKFace> ftFaceList = new ArrayList<>();
    private Integer specificCameraId = null;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FaceTrackListener faceTrackListener;
    private LinkedBlockingQueue<FaceRecognizeRunnable> faceRecognizeRunnables = new LinkedBlockingQueue<FaceRecognizeRunnable>(frThreadNum);
    //trackId相关
    private int currentTrackId = 0;
    private List<Integer> formerTrackIdList = new ArrayList<>();
    private List<Integer> currentTrackIdList = new ArrayList<>();
    private List<Rect> formerFaceRectList = new ArrayList<>();

    private ConcurrentHashMap<Integer, String> nameMap = new ConcurrentHashMap<>();
    private static final float SIMILARITY_RECT = 0.3f;

    private FaceCameraHelper(Builder builder) {
        ftEngine = builder.ftEngine;
        frEngine = builder.frEngine;
        previewView = builder.previewDisplayView;
        faceRectView = builder.faceRectView;
        activity = builder.activity;
        faceRectColor = builder.faceRectColor;
        faceRectThickness = builder.faceRectThickness;
        specificCameraId = builder.specificCameraId;
        faceTrackListener = builder.faceTrackListener;
        currentTrackId = builder.currentTrackId;

        if (builder.previewDisplayView instanceof TextureView) {
            isMirror = builder.isMirror;
        } else if (isMirror) {
            Log.e(TAG, "mirror is effective only when the preview is on a textureView");
        }

        if (builder.frThreadNum > 0) {
            frThreadNum = builder.frThreadNum;
            faceRecognizeRunnables = new LinkedBlockingQueue<FaceRecognizeRunnable>(frThreadNum);
        } else {
            Log.e(TAG, "frThread num must > 0,now using default value:" + frThreadNum);
        }
    }


    public void init() {
        if (previewView instanceof TextureView) {
            ((TextureView) this.previewView).setSurfaceTextureListener(textureListener);
        } else if (previewView instanceof SurfaceView) {
            ((SurfaceView) previewView).getHolder().addCallback(surfaceCallback);
        }
        if (isMirror) {
            previewView.setScaleX(-1);
        }
    }


    /**
     * 请求获取人脸特征数据，需要传入FR的参数，以下参数同 AFR_FSDKEngine.AFR_FSDK_ExtractFRFeature
     *
     * @param nv21     NV21格式的图像数据
     * @param faceRect 人脸框
     * @param width    图像宽度
     * @param height   图像高度
     * @param format   图像格式
     * @param ori      人脸在图像中的朝向
     * @param trackId  请求人脸特征的唯一请求码，一般使用trackId
     */
    public void requestFaceFeature(byte[] nv21, Rect faceRect, int width, int height, int format, int ori, Integer trackId) {
        if (faceTrackListener != null) {
            if (frEngine != null && faceRecognizeRunnables.size() < frThreadNum) {
                byte[] nv21Data = new byte[nv21.length];
                System.arraycopy(nv21, 0, nv21Data, 0, nv21.length);
                faceRecognizeRunnables.add(new FaceRecognizeRunnable(nv21Data, faceRect, width, height, format, ori, trackId));
                executor.execute(faceRecognizeRunnables.poll());
                Log.i(TAG, "requestFaceFeature: " + executor.hashCode() + "  " + FaceCameraHelper.this.hashCode());
            } else {
                faceTrackListener.onFaceFeatureInfoGet(null, trackId);
            }
        }
    }


    public void start() {
        Log.i(TAG, "start: ");
        //相机数量为2则打开1,1则打开0,相机ID 1为前置，0为后置
        mCameraId = Camera.getNumberOfCameras() - 1;
        if (specificCameraId != null && specificCameraId <= mCameraId) {
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
            final Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            previewSize = getBestSupportedSize(parameters.getSupportedPreviewSizes(), metrics);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            //对焦模式设置
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.size() > 0) {
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }
            mCamera.setParameters(parameters);
            if (previewView instanceof TextureView) {
                mCamera.setPreviewTexture(((TextureView) previewView).getSurfaceTexture());
            } else {
                mCamera.setPreviewDisplay(((SurfaceView) previewView).getHolder());
            }
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            if (faceTrackListener != null) {
                faceTrackListener.onCameraOpened(mCamera);
            }
        } catch (Exception e) {
            if (faceTrackListener != null) {
                faceTrackListener.onFail(e);
            }
        }
    }

    private void stop() {
        Log.i(TAG, "stop: ");
        activity = null;
        if (mCamera == null) {
            return;
        }
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
        try {
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewDisplay(null);
            mCamera.stopPreview();
            mCamera.release();
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
     * reversePortrait 270
     *
     * @return 相机预览数据的展示旋转角度
     */
    private int getCameraOri() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = rotation * 90;
        //按照以下常量值的规律，degrees = rotation * 90
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
            if (ftEngine != null) {
                ftFaceList.clear();
                int ftCode = ftEngine.AFT_FSDK_FaceFeatureDetect(nv21, previewSize.width, previewSize.height, AFT_FSDKEngine.CP_PAF_NV21, ftFaceList).getCode();
                if (ftCode != 0) {
                    faceTrackListener.onFail(new Exception("ft failed,code is " + ftCode));
                }
                refreshTrackId(ftFaceList);
                faceTrackListener.adjustFaceRectList(ftFaceList, currentTrackIdList);
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                    if (ftFaceList.size() > 0) {
                        for (int i = 0; i < ftFaceList.size(); i++) {
                            Rect adjustedRect = TrackUtil.adjustRect(new Rect(ftFaceList.get(i).getRect()), previewSize.width, previewSize.height, surfaceWidth, surfaceHeight, cameraOrientation, mCameraId, isMirror);
                            faceRectView.addDrawInfo(new DrawInfo(adjustedRect, faceRectColor, faceRectThickness, currentTrackIdList.get(i), nameMap.get(currentTrackIdList.get(i))));
                        }
                    }
                }
            }
            faceTrackListener.onPreviewData(nv21, ftFaceList, currentTrackIdList);
        }
    }

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            start();
            if (ftEngine == null && faceTrackListener != null) {
                faceTrackListener.onFail(new Exception("ftEngine is null"));
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            surfaceWidth = width;
            surfaceHeight = height;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            stop();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            start();
            if (ftEngine == null && faceTrackListener != null) {
                faceTrackListener.onFail(new Exception("ftEngine is null"));
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            surfaceWidth = width;
            surfaceHeight = height;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stop();
        }
    };

    /**
     * 人脸解析的线程
     */
    public class FaceRecognizeRunnable implements Runnable {
        private Rect faceRect;
        private int width;
        private int height;
        private int format;
        private int ori;
        private Integer trackId;
        private byte[] nv21Data;

        private FaceRecognizeRunnable(byte[] nv21Data, Rect faceRect, int width, int height, int format, int ori, Integer trackId) {
            if (nv21Data == null) {
                return;
            }
            this.nv21Data = new byte[nv21Data.length];
            System.arraycopy(nv21Data, 0, this.nv21Data, 0, nv21Data.length);
            this.faceRect = new Rect(faceRect);
            this.width = width;
            this.height = height;
            this.format = format;
            this.ori = ori;
            this.trackId = trackId;
        }

        @Override
        public void run() {
            if (faceTrackListener != null && nv21Data != null) {
                if (frEngine != null) {
                    AFR_FSDKFace frFace = new AFR_FSDKFace();
                    int frCode = frEngine.AFR_FSDK_ExtractFRFeature(nv21Data, width, height, format, faceRect, ori, frFace).getCode();
                    if (frCode == 0) {
                        faceTrackListener.onFaceFeatureInfoGet(frFace, trackId);
                    } else {
                        faceTrackListener.onFaceFeatureInfoGet(null, trackId);
                        faceTrackListener.onFail(new Exception("fr failed errorCode is " + frCode));
                    }
                    nv21Data = null;
                } else {
                    faceTrackListener.onFaceFeatureInfoGet(null, trackId);
                    faceTrackListener.onFail(new Exception("fr failed ,frEngine is null"));
                }
                if (faceRecognizeRunnables.size() > 0) {
                    executor.execute(faceRecognizeRunnables.poll());
                }
            }
        }
    }


    /**
     * 刷新trackId
     *
     * @param ftFaceList 传入的人脸列表
     */
    private void refreshTrackId(List<AFT_FSDKFace> ftFaceList) {
        currentTrackIdList.clear();
        //每项预先填充-1
        for (int i = 0; i < ftFaceList.size(); i++) {
            currentTrackIdList.add(-1);
        }
        //前一次无人脸现在有人脸，填充新增TrackId
        if (formerTrackIdList.size() == 0) {
            for (int i = 0; i < ftFaceList.size(); i++) {
                currentTrackIdList.set(i, ++currentTrackId);
            }
        } else {
            //前后都有人脸,对于每一个人脸框
            for (int i = 0; i < ftFaceList.size(); i++) {
                //遍历上一次人脸框
                for (int j = 0; j < formerFaceRectList.size(); j++) {
                    //若是同一张人脸
                    if (TrackUtil.isSameFace(SIMILARITY_RECT, formerFaceRectList.get(j), ftFaceList.get(i).getRect())) {
                        //记录ID
                        currentTrackIdList.set(i, formerTrackIdList.get(j));
                        break;
                    }
                }
            }
        }
        //上一次人脸框不存在此人脸，新增
        for (int i = 0; i < currentTrackIdList.size(); i++) {
            if (currentTrackIdList.get(i) == -1) {
                currentTrackIdList.set(i, ++currentTrackId);
            }
        }
        formerTrackIdList.clear();
        formerFaceRectList.clear();
        for (int i = 0; i < ftFaceList.size(); i++) {
            formerFaceRectList.add(new Rect(ftFaceList.get(i).getRect()));
            formerTrackIdList.add(currentTrackIdList.get(i));
        }

        //刷新nameMap
        clearLeftName(currentTrackIdList);
    }

    /**
     * 获取当前的最大trackID,可用于退出时保存
     *
     * @return
     */
    public int getCurrentTrackId() {
        return currentTrackId;
    }

    /**
     * 新增搜索成功的人脸
     *
     * @param trackId 指定的trackId
     * @param name    trackId对应的人脸
     */
    public void putName(int trackId, String name) {
        nameMap.put(trackId, name);
    }

    /**
     * 清除map中已经离开的人脸
     *
     * @param trackIdList 最新的trackIdList
     */
    private void clearLeftName(List<Integer> trackIdList) {
        Set<Integer> keySet = nameMap.keySet();
        for (Integer integer : keySet) {
            if (!trackIdList.contains(integer)) {
                nameMap.remove(integer);
            }
        }
    }

    public static final class Builder {
        private AFT_FSDKEngine ftEngine;
        private AFR_FSDKEngine frEngine;
        private View previewDisplayView;
        private FaceRectView faceRectView;
        private Activity activity;
        private int faceRectColor;
        private int faceRectThickness;
        private boolean isMirror;
        private Integer specificCameraId;
        private FaceTrackListener faceTrackListener;
        private int frThreadNum;
        private int currentTrackId;

        public Builder() {
        }


        public Builder ftEngine(AFT_FSDKEngine val) {
            ftEngine = val;
            return this;
        }

        public Builder frEngine(AFR_FSDKEngine val) {
            frEngine = val;
            return this;
        }

        public Builder previewOn(View val) {
            if (val instanceof SurfaceView || val instanceof TextureView) {
                previewDisplayView = val;
                return this;
            } else {
                throw new RuntimeException("you must preview on a textureView or a surfaceView");
            }
        }

        public Builder faceRectView(FaceRectView val) {
            faceRectView = val;
            return this;
        }

        public Builder activity(Activity val) {
            activity = val;
            return this;
        }

        public Builder faceRectColor(int val) {
            faceRectColor = val;
            return this;
        }

        public Builder faceRectThickness(int val) {
            faceRectThickness = val;
            return this;
        }

        public Builder isMirror(boolean val) {
            isMirror = val;
            return this;
        }

        public Builder specificCameraId(Integer val) {
            specificCameraId = val;
            return this;
        }

        public Builder faceTrackListener(FaceTrackListener val) {
            faceTrackListener = val;
            return this;
        }

        public Builder frThreadNum(int val) {
            frThreadNum = val;
            return this;
        }

        public Builder currentTrackId(int val) {
            currentTrackId = val;
            return this;
        }

        public FaceCameraHelper build() {
            return new FaceCameraHelper(this);
        }
    }
}
