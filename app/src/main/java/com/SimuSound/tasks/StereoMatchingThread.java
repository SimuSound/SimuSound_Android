package com.SimuSound.tasks;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.calib3d.StereoSGBM;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.StereoBM;
import org.opencv.ximgproc.DisparityWLSFilter;
import org.opencv.ximgproc.Ximgproc;

import com.SimuSound.R;

import com.SimuSound.utils.Constants;
import com.SimuSound.utils.PatchInf;
import com.SimuSound.utils.PrintMatSample;
import com.SimuSound.utils.ReadFloatFileIntoMat;
import com.SimuSound.utils.SynchronizedStereoImageData;
import com.SimuSound.utils.StereoImageData;

import java.util.concurrent.LinkedBlockingQueue;

public class StereoMatchingThread extends HandlerThread {
    private static final boolean DEBUG = true;
    private static final String TAG = "StereoMatchingThread";

    private static final int numDisparity = 192;
    private static final int blockSizeBM = 5;  // remaining params set in constructor
    private static final int blockSizeSGBM = 7;  // remaining params set in constructor

    private Activity mMainActivity;

    public SynchronizedStereoImageData mSyncData;
    public LinkedBlockingQueue<Bitmap> mStereoToObjectDetectionData;
    public LinkedBlockingQueue<StereoImageData> mStereoImageData;

    private long mTimestamp;

    private ImageView mUndistortedImageViewL;
    private ImageView mUndistortedImageViewR;
    private ImageView mDisparityMapView;
    private ImageView mDepthMapView;

    private Bitmap mImageL;
    private Bitmap mImageR;
    private Bitmap mImageObjDetect;
    private Bitmap mUndistortedImageL;
    private Bitmap mUndistortedImageR;
    private Bitmap mDisparityMapImage;
    private Bitmap mDepthMapImage;

    // calibration data
    private Mat mapL1Mat;
    private Mat mapL2Mat;
    private Mat mapR1Mat;
    private Mat mapR2Mat;
    private Mat QMat;

    private Mat mImageMatL;
    private Mat mImageMatR;
    private Mat mDisparityMatL;  // disparity of left image
    private Mat mDisparityFilteredMatL;
    private Mat mDepthMapMat;  // depth map of left image

    // these are declared in class for performance reasons
    private Mat mGrayL;
    private Mat mGrayR;
    private Mat mWLSZerosMat;
    private Mat mDisparityMatDispL;
//    private Mat mDepthMapMattemp3;
    private Mat mDepthMapMatDisp;

    private final boolean use_sgbm = true;
    private StereoBM mStereoBMMatcherL;
    private StereoSGBM mStereoSGBMMatcherL;
    private DisparityWLSFilter mWLSFilter;


    public StereoMatchingThread(Activity mainActivity, SynchronizedStereoImageData syncData, LinkedBlockingQueue<Bitmap> stereoToObjectDetectionData, LinkedBlockingQueue<StereoImageData> stereoImageData) {
        super(TAG);
        mMainActivity = mainActivity;
        mSyncData = syncData;
        mStereoToObjectDetectionData = stereoToObjectDetectionData;
        mStereoImageData = stereoImageData;

        mImageMatL = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC3);
        mImageMatR = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC3);
        mDisparityMatL = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_16SC1);
        mDisparityFilteredMatL = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_16SC1);
        mDepthMapMat = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_32FC1);

        mGrayL = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC1);
        mGrayR = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC1);
        mWLSZerosMat = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_16SC1);  // all zeros
        mDisparityMatDispL = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC1);  // all zeros
        mDepthMapMatDisp = new Mat(Constants.stereoHeight, Constants.stereoWidth, CvType.CV_8UC1);

        mStereoBMMatcherL = StereoBM.create(numDisparity, blockSizeBM);
        mStereoBMMatcherL.setMinDisparity(0);
        mStereoBMMatcherL.setTextureThreshold(7);
        mStereoBMMatcherL.setUniquenessRatio(0);
        mStereoBMMatcherL.setDisp12MaxDiff(0);
//        mStereoAlgo.setPreFilterCap();
//        mStereoAlgo.setPreFilterSize();
//        mStereoAlgo.setPreFilterType();
        mStereoBMMatcherL.setSpeckleRange(0);
        mStereoBMMatcherL.setSpeckleWindowSize(0);

        int window_size = 9;
        mStereoSGBMMatcherL = StereoSGBM.create(0,
                numDisparity,
                blockSizeSGBM,
                8*3*window_size*window_size,
                32*3*window_size*window_size,
                0,
                0,
                0,
                0,
                0,
                StereoSGBM.MODE_SGBM_3WAY
        );

        if (use_sgbm) {
            mWLSFilter = Ximgproc.createDisparityWLSFilter(mStereoSGBMMatcherL);
        } else {
            mWLSFilter = Ximgproc.createDisparityWLSFilter(mStereoBMMatcherL);
        }
        mWLSFilter.setSigmaColor(1.5);
        mWLSFilter.setLambda(8000);
        double ddr = 0.33;
        int depthDiscontinuityRadius = (int)Math.ceil(ddr * (use_sgbm ? window_size  : blockSizeBM));
        mWLSFilter.setDepthDiscontinuityRadius(depthDiscontinuityRadius);

        // unused right now
//        mStereoMatcherR = (StereoBM) Ximgproc.createRightMatcher(mStereoBMMatcherL);
    }

    @Override
    public void run() {
        Log.d(TAG, TAG + " started");
        Core.setNumThreads(1);

        mMainActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mUndistortedImageViewL = mMainActivity.findViewById(R.id.undistorted_image_L);
                mUndistortedImageViewR = mMainActivity.findViewById(R.id.undistorted_image_R);
                mDisparityMapView = mMainActivity.findViewById(R.id.disparity_map_image);
                mDepthMapView = mMainActivity.findViewById(R.id.depth_map_image);
            }
        });

        // TODO: remove spaghet
        while (mDisparityMapView == null) {
            try { sleep(100); } catch (InterruptedException e) {}
        }
        try { sleep(100); } catch (InterruptedException e) {}

        getCalibrationData(); // TODO: make faster

        // TODO: thread exit condition
        while (true) {
            // can do this because stereo matching is expected take long time vs getting USB images
            // otherwise need some sort of condition variable
            mSyncData.mReadWriteLock.readLock().lock();
            // don't need copy because USB thread creates copy already
            mImageL = Bitmap.createScaledBitmap(mSyncData.imageL, Constants.stereoWidth, Constants.stereoHeight, true);
            mImageR = Bitmap.createScaledBitmap(mSyncData.imageR, Constants.stereoWidth, Constants.stereoHeight, true);
            mTimestamp = Math.min(mSyncData.timestampL, mSyncData.timestampR);  // disparity is only as good as earliest image
            mSyncData.mReadWriteLock.readLock().unlock();
            // load stereo bitmaps into mats
            Utils.bitmapToMat(mImageL, mImageMatL);
            Utils.bitmapToMat(mImageR, mImageMatR);

            undistortImages();

            // give undistorted image to yolo
            mImageObjDetect = Bitmap.createBitmap(Constants.stereoWidth, Constants.stereoHeight, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImageMatL, mImageObjDetect);
            try {
                // block until taken because we want stereo and yolo to be 1 to 1
                mStereoToObjectDetectionData.put(mImageObjDetect);  // TODO: is this thread safe
                Log.d(TAG, TAG + " put bitmap to yolo queue");
            } catch (InterruptedException e) {
                Log.e(TAG, TAG + " interrupted");
            }

            computeDisparityMap();
            if (Constants.enableImagePreviews) {
                showDisparityMap();
            }

            computeDepthMap();
            StereoImageData stereoImageData = new StereoImageData(mImageMatL.clone(), mDepthMapMat.clone(), mTimestamp);
            mStereoImageData.offer(stereoImageData);
            if (Constants.enableImagePreviews) {
                showDepthMap();
            }

            try { sleep(400); } catch (InterruptedException e) { Log.w(TAG, TAG + " interrupted"); }  // stop phone from catching fire

            // wait here if object detection is behind by more than 1
            synchronized(mStereoToObjectDetectionData) {
                while (!mStereoToObjectDetectionData.isEmpty()) {
                    try {
                        mStereoToObjectDetectionData.wait(); //wait for the queue to become empty
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // TODO: make faster
    protected void getCalibrationData() {
        // file is packed F32BE, extension is .wav to avoid compression
        mapL1Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapL1_BE.wav", mMainActivity);
        Log.e(TAG, "mapL1Mat[0][0]=" + mapL1Mat.get(0, 0)[0]);
        Log.e(TAG, "mapL1Mat[0][1]=" + mapL1Mat.get(0, 1)[0]);
        Log.e(TAG, "mapL1Mat[1][0]=" + mapL1Mat.get(1, 0)[0]);

        mapL2Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapL2_BE.wav", mMainActivity);
        Log.e(TAG, "mapL2Mat[0][0]=" + mapL2Mat.get(0, 0)[0]);
        Log.e(TAG, "mapL2Mat[0][1]=" + mapL2Mat.get(0, 1)[0]);
        Log.e(TAG, "mapL2Mat[1][0]=" + mapL2Mat.get(1, 0)[0]);

        mapR1Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapR1_BE.wav", mMainActivity);
        Log.e(TAG, "mapR1Mat[0][0]=" + mapR1Mat.get(0, 0)[0]);
        Log.e(TAG, "mapR1Mat[0][1]=" + mapR1Mat.get(0, 1)[0]);
        Log.e(TAG, "mapR1Mat[1][0]=" + mapR1Mat.get(1, 0)[0]);

        mapR2Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapR2_BE.wav", mMainActivity);
        Log.e(TAG, "mapR2Mat[0][0]=" + mapR2Mat.get(0, 0)[0]);
        Log.e(TAG, "mapR2Mat[0][1]=" + mapR2Mat.get(0, 1)[0]);
        Log.e(TAG, "mapR2Mat[1][0]=" + mapR2Mat.get(1, 0)[0]);

        QMat = new ReadFloatFileIntoMat().MatFromFileFaster(4, 4, CvType.CV_32FC1, "/sdcard/Download/Q_BE.wav", mMainActivity);
        Log.e(TAG, "QMat[0][0]=" + QMat.get(0, 0)[0]);
        Log.e(TAG, "QMat[0][1]=" + QMat.get(0, 1)[0]);
        Log.e(TAG, "QMat[1][0]=" + QMat.get(1, 0)[0]);

    }

    private void undistortImages() {
        // undistort
        Imgproc.remap(mImageMatL, mImageMatL, mapL1Mat, mapL2Mat, Imgproc.INTER_LINEAR);  // TODO: try other sampling algos
        Imgproc.remap(mImageMatR, mImageMatR, mapR1Mat, mapR2Mat, Imgproc.INTER_LINEAR);

        Imgproc.cvtColor(mImageMatL, mGrayL, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mImageMatR, mGrayR, Imgproc.COLOR_BGR2GRAY);

        // this assumes histogram of L and R are similar
//        Imgproc.equalizeHist(mGrayL, mGrayL);
//        Imgproc.equalizeHist(mGrayR, mGrayR);

        if (Constants.enableImagePreviews) {
            mUndistortedImageL = Bitmap.createBitmap(Constants.stereoWidth, Constants.stereoHeight, Bitmap.Config.ARGB_8888);
            mUndistortedImageR = Bitmap.createBitmap(Constants.stereoWidth, Constants.stereoHeight, Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(mGrayL, mUndistortedImageL);  // for debug only
            Utils.matToBitmap(mGrayR, mUndistortedImageR);

            mMainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUndistortedImageViewL.setImageBitmap(mUndistortedImageL.copy(mUndistortedImageL.getConfig(), false));
                    mUndistortedImageViewR.setImageBitmap(mUndistortedImageR.copy(mUndistortedImageR.getConfig(), false));
                }
            });
        }
    }

    private void imageAdjustment(Mat MatL, Mat MatR) {
        // TODO
    }

    private Mat computeDisparityMap() {

        if (use_sgbm) {
            mStereoSGBMMatcherL.compute(mGrayL, mGrayR, mDisparityMatL);
        } else {
            mStereoBMMatcherL.compute(mGrayL, mGrayR, mDisparityMatL);
        }

//        PrintMatSample.PrintSample(mDisparityMatL, "mDisparityMatL 1");

        // MAke sure to set the right block size above
//        mWLSFilter.filter(mDisparityMatL, mGrayL, mDisparityFilteredMatL, mWLSZerosMat); // TODO: try giving full color image

//        PrintMatSample.PrintSample(mDisparityFilteredMatL, "mDisparityFilteredMatL 1");

        // values from compute and filter are from -1 to maxdisparity-1, have to cut out negative val
//        Imgproc.threshold(mDisparityFilteredMatL, mDisparityMatL, 0, numDisparity * 16, Imgproc.THRESH_TOZERO);
        Imgproc.threshold(mDisparityMatL, mDisparityMatL, 0, numDisparity * 16, Imgproc.THRESH_TOZERO);  // Filter out -1 values
//        TODO: optimize by combining with depth calculation
//        Core.divide(mDisparityMatL, new org.opencv.core.Scalar(16), mDisparityMatL);  // moved to showDisparityMap to preserve fractional values
        mDisparityMatL.convertTo(mDisparityMatL, CvType.CV_16SC1);

//        PrintMatSample.PrintSample(mDisparityMatL, "mDisparityMatL 2");

        return mDisparityMatL;
    }

    private void showDisparityMap() {
        Core.divide(mDisparityMatL, new org.opencv.core.Scalar(16), mDisparityMatDispL);  // moved to showDisparityMap to preserve fractional values
        mDisparityMatDispL.convertTo(mDisparityMatDispL, CvType.CV_8UC1);
        // disparity is shown normalized to 8-bit value (range will likely be compressed)
        Core.normalize(mDisparityMatDispL, mDisparityMatDispL, 0, 255, Core.NORM_MINMAX);
        mDisparityMapImage = Bitmap.createBitmap(Constants.stereoWidth, Constants.stereoHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mDisparityMatDispL, mDisparityMapImage);  // for debug only

        mMainActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mDisparityMapView.setImageBitmap(mDisparityMapImage.copy(mDisparityMapImage.getConfig(), false));
            }
        });
    }

    private Mat computeDepthMap() {
        // convert to depth map
        mDisparityMatL.convertTo(mDepthMapMat, CvType.CV_32FC1);

        Core.divide((2.15 * 144 * 0.7 * 16), mDepthMapMat, mDepthMapMat);  // divide by 16 is here
        PatchInf.PatchFloatInf(mDepthMapMat, 0.0f); // TODO: this is extremely slow
        Imgproc.threshold(mDepthMapMat, mDepthMapMat, 12.25, 0.0f, Imgproc.THRESH_TOZERO_INV); // filter out noise, anything past 12.25 we ignore

        PrintMatSample.PrintSample(mDepthMapMat, "mDepthMapMat 1");

        return mDepthMapMat;
    }

    private void showDepthMap() {
//        Core.normalize(mDepthMapMat, mDepthMapMatDisp, 0, 255, Core.NORM_MINMAX);
        // depth map is truncated from previous step, 12.25m is pure white, anything higher is just 0
        Core.multiply(mDepthMapMat, new Scalar(20), mDepthMapMatDisp);
        mDepthMapMatDisp.convertTo(mDepthMapMatDisp, CvType.CV_8UC1);
        mDepthMapImage = Bitmap.createBitmap(Constants.stereoWidth, Constants.stereoHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mDepthMapMatDisp, mDepthMapImage);  // for debug only

        mMainActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mDepthMapView.setImageBitmap(mDepthMapImage.copy(mDepthMapImage.getConfig(), false));
            }
        });
    }
}
