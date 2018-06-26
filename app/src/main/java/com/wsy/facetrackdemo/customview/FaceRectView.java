package com.wsy.facetrackdemo.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.wsy.facetrackdemo.model.DrawInfo;
import com.wsy.facetrackdemo.previewutil.TrackUtil;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author wsy9057
 */
public class FaceRectView extends View {
    private CopyOnWriteArrayList<DrawInfo> drawInfoList = new CopyOnWriteArrayList<>();
    public FaceRectView(Context context) {
        this(context,null);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawInfoList !=null&& drawInfoList.size()>0) {
            for (DrawInfo drawInfo : drawInfoList) {
                TrackUtil.drawFaceRect(canvas, drawInfo.getRect(), drawInfo.getColor(), drawInfo.getFaceRectThickness(),
                        drawInfo.getTrackId(), drawInfo.getName());
            }
        }
    }

    public void clearFaceInfo(){
        drawInfoList.clear();
        invalidate();
    }

    public void addDrawInfo(DrawInfo drawInfo) {
        drawInfoList.add(drawInfo);
        invalidate();
    }
}