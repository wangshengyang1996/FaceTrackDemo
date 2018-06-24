package com.wsy.facetrackdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.wsy.facetrackdemo.common.RequestFeatureStatus;
import com.wsy.facetrackdemo.previewutil.FaceCameraHelper;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity implements FaceCameraHelper.FaceTrackListener {
    private static final String TAG = "MainActivity";
    private static final int ACTION_REQUEST_CAMERA = 0x0001;
    private SurfaceView surfaceViewPreview, surfaceViewRect;
    private Camera.Size previewSize;
    private AFT_FSDKEngine ftEngine;
    private AFR_FSDKEngine frEngine;
    private AFT_FSDKError ftError;
    private AFR_FSDKError frError;
    private FaceCameraHelper faceCameraHelper;
    private volatile ConcurrentHashMap<Integer, RequestFeatureStatus> requestFeatureStatusMap = new ConcurrentHashMap<>();

    private static final String APP_ID = "3jV31oGD5YcGBiM4PrCmV82drJSJ2YqFmfZi2WDm3RFp";
    private static final String FT_SDK_KEY = "9ZmfHG9Em8EUXJ5xNrMxv5K5Bc2RiLLYfK9MiqszyvDh";
    private static final String FR_SDK_KEY = "9ZmfHG9Em8EUXJ5xNrMxv5KZqD58ujRfAogS55ujkmKQ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceViewPreview = findViewById(R.id.surfaceview_preview);
        surfaceViewRect = findViewById(R.id.surfaceview_rect);
        initEngine();

        if (checkCameraPermission()) {
            initCamera(surfaceViewPreview, surfaceViewRect, ftEngine);
        }
    }


    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ACTION_REQUEST_CAMERA);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera(surfaceViewPreview, surfaceViewRect, ftEngine);
                faceCameraHelper.start();

            } else {
                Toast.makeText(this, "权限不足，相机打开失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        unInitEngine();
        super.onDestroy();
    }

    @Override
    public void onPreviewData(byte[] nv21, List<AFT_FSDKFace> ftFaceList, List<Integer> trackIdList) {
        //请求获取人脸特征数据
        if (ftFaceList.size() > 0 && previewSize != null) {
            for (int i = 0; i < ftFaceList.size(); i++) {
                if (requestFeatureStatusMap.get(trackIdList.get(i)) == null
                        || requestFeatureStatusMap.get(trackIdList.get(i)) == RequestFeatureStatus.FAILED) {
                    faceCameraHelper.requestFaceFeature(nv21, ftFaceList.get(i).getRect(), previewSize.width, previewSize.height, AFR_FSDKEngine.CP_PAF_NV21, ftFaceList.get(i).getDegree(), trackIdList.get(i));
                    requestFeatureStatusMap.put(trackIdList.get(i), RequestFeatureStatus.SEARCHING);
                }
                clearLeftFace(trackIdList);
            }
        }
    }


    @Override
    public void onFail(Exception e) {
        Log.i(TAG, "onFail: " + e.getMessage());
    }

    @Override
    public void onCameraOpened(Camera camera) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        Log.i(TAG, "onCameraOpened:  previewSize is " + previewSize.width + "x" + previewSize.height);
        this.previewSize = previewSize;
    }

    @Override
    public void adjustFaceRectList(List<AFT_FSDKFace> ftFaceList, List<Integer> trackIdList) {

    }


    @Override
    public void onFaceFeatureInfoGet(AFR_FSDKFace frFace, Integer requestId) {
        //模拟网络搜索人脸是否成功
        boolean success =new Random().nextBoolean();
        if (frFace != null) {

            requestFeatureStatusMap.put(requestId, success ? RequestFeatureStatus.SUCCEED : RequestFeatureStatus.FAILED);
            //模拟搜索成功后设置姓名
            if (success) {
                faceCameraHelper.putName(requestId, "requestId:" + requestId);
            }
        } else {
            requestFeatureStatusMap.put(requestId,RequestFeatureStatus.FAILED);
        }

    }

    public void initCamera(@NonNull SurfaceView surfaceViewPreview, @Nullable SurfaceView surfaceViewRect, AFT_FSDKEngine ftEngine) {
        faceCameraHelper = new FaceCameraHelper(new WeakReference<Activity>(this), ftEngine);
        faceCameraHelper.setFaceRectColor(Color.YELLOW);
        faceCameraHelper.setFaceRectThickness(5);
        faceCameraHelper.setSpecificCameraId(0);
        faceCameraHelper.setFaceTrackListener(this);
        faceCameraHelper.setFrEngine(frEngine);
        faceCameraHelper.init(surfaceViewPreview, surfaceViewRect);
    }

    private void initEngine() {
        ftEngine = new AFT_FSDKEngine();
        ftError = ftEngine.AFT_FSDK_InitialFaceEngine(APP_ID, FT_SDK_KEY, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.i(TAG, "init ftEngine: " + ftError.getCode());
        frEngine = new AFR_FSDKEngine();
        frError = frEngine.AFR_FSDK_InitialEngine(APP_ID, FR_SDK_KEY);
        Log.i(TAG, "init frEngine: " + frError.getCode());
    }

    private void unInitEngine() {
        if (ftError.getCode() == 0) {
            ftEngine.AFT_FSDK_UninitialFaceEngine();
        }
        if (frError.getCode() == 0) {
            frEngine.AFR_FSDK_UninitialEngine();
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param trackIdList
     */
    private void clearLeftFace(List<Integer> trackIdList) {
        Set<Integer> keySet = requestFeatureStatusMap.keySet();
        for (Integer integer : keySet) {
            if (!trackIdList.contains(integer)) {
                requestFeatureStatusMap.remove(integer);
            }
        }
    }

}