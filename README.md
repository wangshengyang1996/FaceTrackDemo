
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

    faceCameraHelper = new FaceCameraHelper.Builder()
                    .activity(this)
                    .specificCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                    .isMirror(true) //是否镜像显示，只有TextureView支持此选项
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

//根据屏幕方向和CameraID调整绘制的人脸框
       
       /**
          * @param ftRect          FT人脸框
          * @param previewWidth  相机预览的宽度
          * @param previewHeight 相机预览高度
          * @param canvasWidth   画布的宽度
          * @param canvasHeight  画布的高度
          * @param cameraOri     相机预览方向
          * @param mCameraId     相机ID
          * @return
          */
         public static Rect adjustRect(Rect ftRect, int previewWidth, int previewHeight, int canvasWidth, int canvasHeight, int cameraOri, int mCameraId,boolean isMirror) {
             if (ftRect == null) {
                 return null;
             }
             Rect rect = new Rect(ftRect);
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
                     newRect.right = canvasWidth -  rect.top;
                     newRect.left =  canvasWidth -  rect.bottom;
                     if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                         newRect.top =  canvasHeight - rect.right;
                         newRect.bottom = canvasHeight - rect.left;
                     } else {
                         newRect.top =  rect.left;
                         newRect.bottom =  rect.right;
                     }
                     break;
                 case 180:
                     newRect.top = canvasHeight - rect.bottom;
                     newRect.bottom = canvasHeight -  rect.top;
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
             if (isMirror){
                 int left = newRect.left;
                 int right = newRect.right;
                 newRect.left = canvasWidth - right;
                 newRect.right = canvasWidth - left;
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
