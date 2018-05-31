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
import com.wsy.facetrackdemo.previewutil.FaceCameraHelper;

import java.lang.ref.WeakReference;
import java.util.List;
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


    private static final String APP_ID = "3jV31oGD5YcGBiM4PrCmV82drJSJ2YqFmfZi2WDm3RFp";
    private static final String FT_SDK_KEY = "9ZmfHG9Em8EUXJ5xNrMxv5K5Bc2RiLLYfK9MiqszyvDh";
    private static final String FR_SDK_KEY = "9ZmfHG9Em8EUXJ5xNrMxv5KZqD58ujRfAogS55ujkmKQ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceViewPreview = findViewById(R.id.surfaceview_preview);
        surfaceViewRect = findViewById(R.id.surfaceview_rect);
        initEngine();

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
    public void onPreviewData(byte[] nv21, List<AFT_FSDKFace> ftFaceList) {
        if (ftFaceList.size()>0&&previewSize!=null) {
            faceCameraHelper.requestFaceFeature(nv21,ftFaceList.get(0).getRect(),previewSize.width,previewSize.height,AFR_FSDKEngine.CP_PAF_NV21,ftFaceList.get(0).getDegree());
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
    public void adjustFaceRectList(List<AFT_FSDKFace> ftFaceList) {

    }
    AFR_FSDKFace firstFace;
    @Override
    public void onFaceFeatureInfoGet(AFR_FSDKFace frFace) {
        if (frFace != null) {
            if (firstFace == null){
                firstFace = new AFR_FSDKFace(frFace);
            }
            AFR_FSDKMatching matching = new AFR_FSDKMatching();
            frEngine.AFR_FSDK_FacePairMatching(firstFace,frFace,matching);
            Log.i(TAG, "onFaceFeatureInfoGet: " + frFace.getFeatureData().length +"   " + matching.getScore());
        }

    }

    public void initCamera(@NonNull SurfaceView surfaceViewPreview, @Nullable SurfaceView surfaceViewRect, AFT_FSDKEngine ftEngine) {
        faceCameraHelper = new FaceCameraHelper(new WeakReference<Activity>(this), ftEngine);
        faceCameraHelper.setFaceRectColor(Color.YELLOW);
        faceCameraHelper.setFaceRectThickness(5);
        faceCameraHelper.init(surfaceViewPreview, surfaceViewRect);
        faceCameraHelper.setFaceTrackListener(this);
        faceCameraHelper.setFrEngine(frEngine);
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
        if (ftError.getCode() == 0){
            ftEngine.AFT_FSDK_UninitialFaceEngine();
        }
        if (frError.getCode() == 0){
            frEngine.AFR_FSDK_UninitialEngine();
        }
    }

    @Override
    protected void onPause() {
        faceCameraHelper.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkCameraPermission()) {
            initCamera(surfaceViewPreview, surfaceViewRect, ftEngine);
        }
    }
}
