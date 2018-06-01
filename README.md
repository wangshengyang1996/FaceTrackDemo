
# FaceTrackDemo
使用虹软的FreeSDK做了个打开相机跟踪人脸画框的demo，适配屏幕方向和前后置相机。
FaceCameraHelper提供了相关回调和一些配置属性。

//FaceTrackListener回调接口

    public interface FaceTrackListener {

        /**
         * 回传相机预览数据和人脸框位置
         *
         * @param nv21       相机预览数据
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

        /**
         * 请求人脸特征后的回调
         *
         * @param frFace  人脸特征数据
         * @param requestId  请求码
         */
        void onFaceFeatureInfoGet(@Nullable AFR_FSDKFace frFace, Integer requestId);
    }

        
//功能属性设置

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
    
    /**
     * 设置FR引擎
     *
     * @param frEngine
     */
    public void setFrEngine(AFR_FSDKEngine frEngine) {
        this.frEngine = frEngine;
    }
    
//根据屏幕方向和CameraID调整绘制的人脸框

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

//FR数据获取
    /**
     * 请求获取人脸特征数据，需要传入FR的参数，以下参数同 AFR_FSDKEngine.AFR_FSDK_ExtractFRFeature
     *
     * @param nv21     NV21格式的图像数据
     * @param faceRect 人脸框
     * @param width    图像宽度
     * @param height   图像高度
     * @param format   图像格式
     * @param ori      人脸在图像中的朝向
     * @param requestId      请求人脸特征的请求码
     */
    public void requestFaceFeature(byte[] nv21, Rect faceRect, int width, int height, int format, int ori, Integer requestId) {
        if (faceTrackListener != null) {
            if (frEngine != null && nv21Data == null) {
                nv21Data = new byte[nv21.length];
                System.arraycopy(nv21, 0, nv21Data, 0, nv21.length);
                executor.execute(new FaceRecognizeRunnable(faceRect, width, height, format, ori, requestId));
            }
            //下面这个回调根据需求选择是否需要添加
            else if (frEngine!=null){
                faceTrackListener.onFaceFeatureInfoGet(null,requestId);
            }
        }
    }


//demo截图
![张学友](https://github.com/wangshengyang1996/FaceTrackDemo/blob/master/%E5%BC%A0%E5%AD%A6%E5%8F%8B.jpg)
