package com.wsy.facetrackdemo.previewutil;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Camera;

class TrackUtil {
    /**
     * @param rect          FT人脸框
     * @param previewWidth  相机预览的宽度
     * @param previewHeight 相机预览高度
     * @param canvasWidth   画布的宽度
     * @param canvasHeight  画布的高度
     * @param cameraOri     相机预览方向
     * @param mCameraId     相机ID
     * @return
     */
    static Rect adjustRect(Rect rect, int previewWidth, int previewHeight, int canvasWidth, int canvasHeight, int cameraOri, int mCameraId) {
        if (rect == null) {
            return null;
        }
        if (canvasWidth < canvasHeight) {
            int t = previewHeight;
            previewHeight = previewWidth;
            previewWidth = t;
        }

        float horizontalRatio;
        float verticalRatio;
        if (cameraOri == 0 || cameraOri == 180) {
            horizontalRatio = (float) canvasWidth / (float) previewWidth;
            verticalRatio = (float) canvasHeight / (float) previewHeight;
        } else {
            horizontalRatio = (float) canvasHeight / (float) previewHeight;
            verticalRatio = (float) canvasWidth / (float) previewWidth;
        }
        rect.left *= horizontalRatio;
        rect.right *= horizontalRatio;
        rect.top *= verticalRatio;
        rect.bottom *= verticalRatio;

        Rect newRect = new Rect();

        switch (cameraOri) {
            case 0:
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.left = canvasWidth - rect.right;
                    newRect.right = canvasWidth - rect.left;
                } else {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                }
                newRect.top = rect.top;
                newRect.bottom = rect.bottom;
                break;
            case 90:
                newRect.right = canvasWidth - rect.top;
                newRect.left = canvasWidth - rect.bottom;
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.top = canvasHeight - rect.right;
                    newRect.bottom = canvasHeight - rect.left;
                } else {
                    newRect.top = rect.left;
                    newRect.bottom = rect.right;
                }
                break;
            case 180:
                newRect.top = canvasHeight - rect.bottom;
                newRect.bottom = canvasHeight - rect.top;
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.left = rect.left;
                    newRect.right = rect.right;
                } else {
                    newRect.left = canvasWidth - rect.right;
                    newRect.right = canvasWidth - rect.left;
                }
                break;
            case 270:
                newRect.left = rect.top;
                newRect.right = rect.bottom;
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    newRect.top = rect.left;
                    newRect.bottom = rect.right;
                } else {
                    newRect.top = canvasHeight - rect.right;
                    newRect.bottom = canvasHeight - rect.left;
                }
                break;
            default:
                break;
        }
        return newRect;
    }

    static void drawFaceRect(Canvas canvas, Rect rect, int color, int faceRectThickness, int trackId, String name) {
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

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2);
        paint.setTextSize(rect.width() / 10);
        canvas.drawText(name != null ? name : String.valueOf(trackId), rect.left, rect.top - 10, paint);
    }

    static boolean isSameFace(float fSimilarity, Rect rect1, Rect rect2) {
        int left = 0, top = 0, right = 0, bottom = 0;

        left = Math.max(rect1.left, rect2.left);
        top = Math.max(rect1.top, rect2.top);
        right = Math.min(rect1.right, rect2.right);
        bottom = Math.min(rect1.bottom, rect2.bottom);

        return left < right
                && top < bottom
                && ((rect2.right - rect2.left) * (rect2.bottom - rect2.top)) * fSimilarity <= (right - left) * (bottom - top)
                && ((rect1.right - rect1.left) * (rect1.bottom - rect1.top)) * fSimilarity <= (right - left) * (bottom - top);

    }
}
