package com.SimuSound.utils;

import org.opencv.core.Mat;

public class StereoImageData {
    public Mat mColorImageL;
    public Mat mDepthMapL;
    public long mTimestamp;

    public StereoImageData(Mat colorImageL, Mat depthMapL, long timestamp) {
        mColorImageL = colorImageL;
        mDepthMapL = depthMapL;
        mTimestamp = timestamp;
    }
}
