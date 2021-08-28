package com.SimuSound;

import android.util.Log;

import com.SimuSound.utils.EnvDetectedImage;
import com.SimuSound.utils.EnvDetectedInfo;
import com.SimuSound.utils.ImageToEnvData;

public class EnvModel {
    public native void startEnvModelThread();
    public native void passDetectedInfoNative(EnvDetectedImage envDetectedImage, EnvDetectedInfo[] envDetectedInfos);
    public native void updateIMUAccelNative(float accelX, float accelY, float accelZ);
    public native void updateIMUAGyroNative(float gyroX, float gyroY, float gyroZ);

    public void passDetectedInfo(ImageToEnvData imageToEnvData) {
        Log.e("EnvModel", "got called with timeDiff val of: " + imageToEnvData.mTimeDiff);
        EnvDetectedInfo[] envDetectedInfos = new EnvDetectedInfo[imageToEnvData.mYoloData.length];
        for(int i = 0; i < envDetectedInfos.length; i++) {
            EnvDetectedInfo info = new EnvDetectedInfo();
            info.bottomLeftX = imageToEnvData.mYoloData[i].x;
            info.bottomLeftY = imageToEnvData.mYoloData[i].y;
            info.topRightX = imageToEnvData.mYoloData[i].x + imageToEnvData.mYoloData[i].w;
            info.topRightY = imageToEnvData.mYoloData[i].y + imageToEnvData.mYoloData[i].h;
            info.prob = imageToEnvData.mYoloData[i].prob;
            info.classType = imageToEnvData.mYoloData[i].label;
            info.timeDiff = imageToEnvData.mTimeDiff;
            envDetectedInfos[i] = info;
        }
        EnvDetectedImage envDetectedImage = new EnvDetectedImage();
        envDetectedImage.colorPixelsDataPtr = imageToEnvData.mStereoData.mColorImageL.getNativeObjAddr();
        envDetectedImage.colorImgWidth = imageToEnvData.mStereoData.mColorImageL.width();
        envDetectedImage.colorImgHeight =  imageToEnvData.mStereoData.mColorImageL.height();
        envDetectedImage.depthPixelsDataPtr = imageToEnvData.mStereoData.mDepthMapL.getNativeObjAddr();
        envDetectedImage.depthImgWidth = imageToEnvData.mStereoData.mDepthMapL.width();
        envDetectedImage.depthImgHeight =  imageToEnvData.mStereoData.mDepthMapL.height();
        passDetectedInfoNative(envDetectedImage, envDetectedInfos);
    }

    public void UpdateIMUAccel(float accelX, float accelY, float accelZ) {
        updateIMUAccelNative(accelX, accelY, accelZ); // Comment out to disable IMU correction
    }
    public void UpdateIMUGyro(float gyroX, float gyroY, float gyroZ) {
        updateIMUAGyroNative(gyroX, gyroY, gyroZ); // Comment out to disable IMU correction
    }

    static {
        System.loadLibrary("env-model-jni-lib");
    }
}
