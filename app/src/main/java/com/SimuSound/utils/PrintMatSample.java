package com.SimuSound.utils;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class PrintMatSample {
    public static void PrintSample(Mat sampleMat, String name) {
        Mat downscaledMat = new Mat(6, 8, sampleMat.type());  // TODO ;remove constant
        Imgproc.resize(sampleMat, downscaledMat, downscaledMat.size());
        Log.e("PrintMatSample", name + " sample vals:\n");
        for(int row=0;row<6;row++){
            String vals = "";
            for(int col=0;col<8;col++) {
                vals += downscaledMat.get(row, col)[0] + ", ";
            }
            Log.e("PrintMatSample", "" + vals);
        }
    }
}
