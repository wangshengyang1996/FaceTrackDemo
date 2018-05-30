# FaceTrackDemo
使用虹软的FreeSDK做了个打开相机跟踪人脸画框的demo，适配屏幕方向和前后置相机。
FaceCameraHelper提供了相关回调和一些配置属性。

//FaceTrackListener回调接口
 
        /**
         * 回传相机预览数据和人脸框位置
         *
         * @param nv21 相机预览数据
         * @param ftFaceList 处理后的人脸列表
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
         * @param ftFaceList 待处理的人脸列表
         */
        void adjustFaceRectList(List<AFT_FSDKFace> ftFaceList); 
     
        
        
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
//demo截图
![张学友](https://github.com/wangshengyang1996/FaceTrackDemo/blob/master/%E5%BC%A0%E5%AD%A6%E5%8F%8B.jpg)
