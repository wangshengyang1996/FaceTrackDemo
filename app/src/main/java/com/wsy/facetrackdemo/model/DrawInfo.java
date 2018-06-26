package com.wsy.facetrackdemo.model;

import android.graphics.Rect;

/**
 * 绘制所需的信息：人脸框，人脸框颜色，画笔宽度，trackId，姓名
 * @author wsy9057
 */
public class DrawInfo {
    private Rect rect;
    private int color;
    private int faceRectThickness;
    private int trackId;
    private String name;

    public DrawInfo(Rect rect, int color, int faceRectThickness, int trackId, String name) {
        this.rect = rect;
        this.color = color;
        this.faceRectThickness = faceRectThickness;
        this.trackId = trackId;
        this.name = name;
    }

    public Rect getRect() {
        return rect;
    }

    public int getColor() {
        return color;
    }

    public int getFaceRectThickness() {
        return faceRectThickness;
    }

    public int getTrackId() {
        return trackId;
    }

    public String getName() {
        return name;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setFaceRectThickness(int faceRectThickness) {
        this.faceRectThickness = faceRectThickness;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public void setName(String name) {
        this.name = name;
    }

}
