package com.SimuSound.utils;

import android.util.Log;

import org.opencv.core.Mat;

public class PatchInf {
    public static void PatchFloatInf (Mat myMat, float patchVal) {
        int numVals = myMat.rows() * myMat.cols();
        float vals[] = new float[numVals];
        myMat.get(0, 0, vals);

        for (int i = 0; i < numVals; i++) {
            if (Float.isInfinite(vals[i])) {
                vals[i] = patchVal;
            }
        }


//        for(int row=0;row<myMat.rows();row++){
//            for(int col=0;col<myMat.cols();col++) {
//                if (Double.isInfinite(myMat.get(row, col)[0])) {
//                    myMat.put(row, col, patchVal);
//                }
//            }
//        }

        myMat.put(0, 0, vals);
    }
}
