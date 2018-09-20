package com.wsy.facetrackdemo;

import android.Manifest;
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
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.wsy.facetrackdemo.common.RequestFeatureStatus;
import com.wsy.facetrackdemo.customview.FaceRectView;
import com.wsy.facetrackdemo.previewutil.FaceCameraHelper;
import com.wsy.facetrackdemo.previewutil.FaceTrackListener;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity implements FaceTrackListener {
    private static final String TAG = "MainActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x0001;
    private TextureView textureViewPreview;
    private FaceRectView faceRectView;
    private Camera.Size previewSize;
    private AFT_FSDKEngine ftEngine;
    private AFR_FSDKEngine frEngine;
    private AFT_FSDKError ftError;
    private AFR_FSDKError frError;
    private FaceCameraHelper faceCameraHelper;
    private volatile ConcurrentHashMap<Integer, RequestFeatureStatus> requestFeatureStatusMap = new ConcurrentHashMap<>();

    /**
     * 替换为自己的APP_ID和KEY，若还是运行不了，可能是库的版本问题，还需要替换jar和so
     * SDK申请地址：https://ai.arcsoft.com.cn/product/arcface.html
     */

    private static final String APP_ID = "APP_ID";
    private static final String FT_SDK_KEY = "FT_SDK_KEY";
    private static final String FR_SDK_KEY = "FR_SDK_KEY";


    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

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

        textureViewPreview = findViewById(R.id.textureview_preview);
        faceRectView = findViewById(R.id.facerect_view);
        initEngine();

        if (checkPermissions()) {
            initCamera(textureViewPreview, faceRectView, ftEngine);
        }
    }

    private boolean checkPermissions() {
        for (int i = 0; i < NEEDED_PERMISSIONS.length; i++) {
            if (ContextCompat.checkSelfPermission(this, NEEDED_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                break;
            }
            return true;
        }
        ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                initCamera(textureViewPreview, faceRectView, ftEngine);
                faceCameraHelper.start();
            } else {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
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
        Log.i(TAG, "onPreviewData: " + trackIdList.size());
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
        boolean success = new Random().nextBoolean();
        if (frFace != null) {

            requestFeatureStatusMap.put(requestId, success ? RequestFeatureStatus.SUCCEED : RequestFeatureStatus.FAILED);
            //模拟搜索成功后设置姓名
            if (success) {
                faceCameraHelper.putName(requestId, "requestId:" + requestId);
            }
        } else {
            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        }

    }

    public void initCamera(@NonNull View previewView, @Nullable FaceRectView faceRectView, AFT_FSDKEngine ftEngine) {
        faceCameraHelper = new FaceCameraHelper.Builder()
                .activity(this)
                .specificCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false) //是否镜像显示，只有TextureView支持此选项
                .faceRectColor(Color.YELLOW)    // 人脸框颜色
                .faceRectThickness(5)   //人脸框厚度
                .previewOn(previewView) //预览画面显示控件，支持SurfaceView和TextureView
                .faceTrackListener(this)    //监听回调设置
                .frEngine(frEngine)
                .ftEngine(ftEngine)
                .faceRectView(faceRectView) //人脸框绘制的控件
                .frThreadNum(5) //FR线程队列的数量
                .currentTrackId(1)  // 设置一个初始的trackID,后续在此增加
                .build();
        faceCameraHelper.init();
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