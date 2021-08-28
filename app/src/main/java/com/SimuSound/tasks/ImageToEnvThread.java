package com.SimuSound.tasks;

import android.util.Log;

import com.SimuSound.EnvModel;
import com.SimuSound.utils.ImageToEnvData;
import com.SimuSound.utils.StereoImageData;
import com.tencent.yolov5ncnn.YoloV5Ncnn;

import java.util.concurrent.LinkedBlockingQueue;

public class ImageToEnvThread extends Thread {
    public LinkedBlockingQueue<StereoImageData> mStereoImageData;
    public LinkedBlockingQueue<YoloV5Ncnn.Obj[]> mObjectDetectionData;
    public EnvModel mEnvModel;

    public ImageToEnvThread (LinkedBlockingQueue<StereoImageData> stereoImageData, LinkedBlockingQueue<YoloV5Ncnn.Obj[]> objectDetectionData, EnvModel envModel) {
        mStereoImageData = stereoImageData;
        mObjectDetectionData = objectDetectionData;
        mEnvModel = envModel;
        Log.e("ImageToEnvThread", "started");
    }

    @Override
    public void run() {
        while (true) {
            try {
                StereoImageData tempStereoImageData = mStereoImageData.take();
                YoloV5Ncnn.Obj[] tempYolodata = mObjectDetectionData.take();
                long timeDiff = System.currentTimeMillis() - tempStereoImageData.mTimestamp;
                Log.e("ImageToEnvThread", "took data, timediff=" + timeDiff);
                ImageToEnvData mImageToEnvData = new ImageToEnvData(tempStereoImageData, tempYolodata, timeDiff);

                mEnvModel.passDetectedInfo(mImageToEnvData);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
