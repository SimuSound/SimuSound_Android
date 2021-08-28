package com.SimuSound.utils;

import com.tencent.yolov5ncnn.YoloV5Ncnn;

public class ImageToEnvData {
    public StereoImageData mStereoData;
    public YoloV5Ncnn.Obj[] mYoloData;
    public long mTimeDiff;

    public ImageToEnvData(StereoImageData stereoData, YoloV5Ncnn.Obj[] yoloData, long timeDiff) {
        mStereoData = stereoData;
        mYoloData = yoloData;
        mTimeDiff = timeDiff;
    }
}
