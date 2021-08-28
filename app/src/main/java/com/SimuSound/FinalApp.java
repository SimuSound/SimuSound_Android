package com.SimuSound;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.Process;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.SimuSound.tasks.GyroAccelThread;
import com.SimuSound.tasks.ImageToEnvThread;
import com.SimuSound.tasks.ObjectDetectionThread;
import com.SimuSound.tasks.StereoMatchingThread;
import com.SimuSound.tasks.USBCamerasThread;
import com.SimuSound.utils.ImageToEnvData;
import com.SimuSound.utils.StereoImageData;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import com.SimuSound.utils.SynchronizedStereoImageData;
import com.tencent.yolov5ncnn.YoloV5Ncnn;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class FinalApp extends AppCompatActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;
    private static final String TAG = "FinalApp";
    private static final int width = 2048;
    private static final int height = 1536;

    private USBMonitor mUSBMonitor;

    private USBCamerasThread mUSBCamerasThread;
    private ObjectDetectionThread mObjectDetectionThread;
    private StereoMatchingThread mStereoMatchingThread;
    private ImageToEnvThread mImageToEnvThread;
    private GyroAccelThread mGyroAccelThread;

    private EnvModel mEnvModel;

    public SynchronizedStereoImageData mSyncData;
    public LinkedBlockingQueue<Bitmap> mStereoToObjectDetectionData;
    // these are 1 to 1
    public LinkedBlockingQueue<StereoImageData> mStereoImageData;
    public LinkedBlockingQueue<YoloV5Ncnn.Obj[]> mObjectDetectionData;

    private YoloV5Ncnn mYolov5ncnn = new YoloV5Ncnn();

    private Button startStopSystemButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_app);

        startStopSystemButton = findViewById(R.id.start_system_button);
        startStopSystemButton.setOnClickListener(v -> startSystem());

        mSyncData = new SynchronizedStereoImageData(width, height);
        mStereoToObjectDetectionData = new LinkedBlockingQueue<Bitmap>();
        mStereoImageData = new LinkedBlockingQueue<StereoImageData>();
        mObjectDetectionData = new LinkedBlockingQueue<YoloV5Ncnn.Obj[]>();

        AssetManager assetManager = getAssets();
        try {
            mSyncData.imageR = BitmapFactory.decodeStream(assetManager.open("testcalimg.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.loadLibrary("opencv_java4");

//        if (!OpenCVLoader.initDebug()) {
//            Log.e(TAG, "Internal OpenCV library not found.");
////                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
////                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }

//        mYolov5ncnn = new YoloV5Ncnn();
        boolean ret_init = mYolov5ncnn.Init(getAssets());  // easiest to do this in activity
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov5ncnn Init failed");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    protected void startSystem() {
        startStopSystemButton.setOnClickListener(v -> stopSystem());
        startThreads();
    }

    protected void stopSystem() {
        Log.e(TAG, "TODO: stopSystem()"); // TODO
    }

    protected void startThreads() {
        mUSBCamerasThread = new USBCamerasThread(this, mSyncData);
        mUSBMonitor = new USBMonitor(this, mUSBCamerasThread.mOnDeviceConnectListener);
        mUSBCamerasThread.mUSBMonitor = mUSBMonitor;  // have to do it like this because threads are not meant to initialize USB (?)
        mUSBCamerasThread.start();

        mStereoMatchingThread = new StereoMatchingThread(this, mSyncData, mStereoToObjectDetectionData, mStereoImageData);
        mStereoMatchingThread.start();

        mObjectDetectionThread = new ObjectDetectionThread(this, mYolov5ncnn, mStereoToObjectDetectionData, mObjectDetectionData);
        mObjectDetectionThread.start();

        mEnvModel = new EnvModel();
        mEnvModel.startEnvModelThread();

        mImageToEnvThread = new ImageToEnvThread(mStereoImageData, mObjectDetectionData, mEnvModel);
        mImageToEnvThread.start();

        mGyroAccelThread = new GyroAccelThread(this, mEnvModel);
        mGyroAccelThread.start();
    }


    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            Toast.makeText(FinalApp.this, "cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
//                case LoaderCallbackInterface.INIT_FAILED:
//                    Log.i(TAG,"Init Failed");
//                    break;
//                case LoaderCallbackInterface.INSTALL_CANCELED:
//                    Log.i(TAG,"Install Cancelled");
//                    break;
//                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
//                    Log.i(TAG,"Incompatible Version");
//                    break;
//                case LoaderCallbackInterface.MARKET_ERROR:
//                    Log.i(TAG,"Market Error");
//                    break;
                default:
                    Log.i(TAG,"OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
