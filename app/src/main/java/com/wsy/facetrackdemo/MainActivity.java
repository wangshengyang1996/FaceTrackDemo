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

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.wsy.facetrackdemo.previewutil.FaceCameraHelper;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements FaceCameraHelper.FaceTrackListener {
    private static final String TAG = "MainActivity";
    private static final int ACTION_REQUEST_CAMERA = 0x0001;
    private SurfaceView surfaceViewPreview, surfaceViewRect;
    private AFT_FSDKEngine ftEngine;
    private FaceCameraHelper faceCameraHelper;

    private static final String APP_ID = "3jV31oGD5YcGBiM4PrCmV82drJSJ2YqFmfZi2WDm3RFp";
    private static final String SDK_KEY = "9ZmfHG9Em8EUXJ5xNrMxv5K5Bc2RiLLYfK9MiqszyvDh";

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
        Log.i(TAG, "previewData length: " +nv21.length + ", face num: " + ftFaceList.size());
    }

    @Override
    public void onFail(Exception e) {
        Log.i(TAG, "onFail: " + e.getMessage());
    }

    @Override
    public void onCameraOpened(Camera camera) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        Log.i(TAG, "onCameraOpened:  previewSize is " + previewSize.width + "x" + previewSize.height);
    }

    @Override
    public void adjustFaceRectList(List<AFT_FSDKFace> ftFaceList) {

    }

    public void initCamera(@NonNull SurfaceView surfaceViewPreview, @Nullable SurfaceView surfaceViewRect, AFT_FSDKEngine ftEngine) {
        faceCameraHelper = new FaceCameraHelper(new WeakReference<Activity>(this), ftEngine);
        faceCameraHelper.setFaceRectColor(Color.YELLOW);
        faceCameraHelper.setFaceRectThickness(5);
        faceCameraHelper.init(surfaceViewPreview, surfaceViewRect);
        faceCameraHelper.setFaceTrackListener(this);
    }

    private void initEngine() {
        ftEngine = new AFT_FSDKEngine();
        AFT_FSDKError ftError = ftEngine.AFT_FSDK_InitialFaceEngine(APP_ID, SDK_KEY, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.i(TAG, "initEngine: " + ftError.getCode());
    }

    private void unInitEngine() {
        ftEngine.AFT_FSDK_UninitialFaceEngine();
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
