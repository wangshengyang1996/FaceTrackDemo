package com.wsy.facetrackdemo.previewutil;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Camera;

class DrawUtil {

    static Rect adjustRect(Rect rect, int previewWidth, int previewHeight, int screenWidth, int screenHeight, int cameraOri, int mCameraId) {
        if (rect == null) {
            return null;
        }
        if (screenWidth < screenHeight) {
            int t = previewHeight;
            previewHeight = previewWidth;
            previewWidth = t;
        }
        float horizontalRatio = (float) screenWidth / (float) previewWidth;
        float verticalRatio = (float) screenHeight / (float) previewHeight;

        rect.left *= horizontalRatio;
        rect.right *= horizontalRatio;
        rect.top *= verticalRatio;
        rect.bottom *= verticalRatio;

        Rect newRect = new Rect();

        switch (cameraOri) {
            case 0:
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.left = screenWidth - rect.left;
                    newRect.right = screenWidth - rect.right;
                } else {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                }
                newRect.top = rect.top;
                newRect.bottom = rect.bottom;
                break;
            case 90:
                newRect.right = screenWidth - rect.top;
                newRect.left = screenWidth - rect.bottom;
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.top = screenHeight - rect.left;
                    newRect.bottom = screenHeight - rect.right;
                } else {
                    newRect.top = rect.left;
                    newRect.bottom = rect.right;
                }
                break;
            case 180:
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                } else {
                    newRect.left = screenWidth - rect.left;
                    newRect.right = screenWidth - rect.right;
                }

                newRect.top = screenHeight - rect.top;
                newRect.bottom = screenHeight - rect.bottom;
                break;
            default:
                break;
        }

        return newRect;
    }

    static void drawFaceRect(Canvas canvas, Rect rect, int color, int faceRectThickness) {
        if (canvas == null || rect == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(faceRectThickness);
        paint.setColor(color);
        Path mPath = new Path();
        mPath.moveTo(rect.left, rect.top + rect.height() / 4);
        mPath.lineTo(rect.left, rect.top);
        mPath.lineTo(rect.left + rect.width() / 4, rect.top);
        mPath.moveTo(rect.right - rect.width() / 4, rect.top);
        mPath.lineTo(rect.right, rect.top);
        mPath.lineTo(rect.right, rect.top + rect.height() / 4);
        mPath.moveTo(rect.right, rect.bottom - rect.height() / 4);
        mPath.lineTo(rect.right, rect.bottom);
        mPath.lineTo(rect.right - rect.width() / 4, rect.bottom);
        mPath.moveTo(rect.left + rect.width() / 4, rect.bottom);
        mPath.lineTo(rect.left, rect.bottom);
        mPath.lineTo(rect.left, rect.bottom - rect.height() / 4);
        canvas.drawPath(mPath, paint);
    }
}
