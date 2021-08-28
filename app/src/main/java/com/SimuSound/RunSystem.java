package com.SimuSound;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.SimuSound.utils.ImageToEnvData;
import com.SimuSound.utils.StereoImageData;
import com.tencent.yolov5ncnn.YoloV5Ncnn;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

public class RunSystem extends AppCompatActivity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final int PERMISSION_REQUEST_CODE = 200;

    private static final String  TAG = "RunSystem";
    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    private EnvModel envModel;

    private CameraBridgeViewBase mOpenCvCameraView;

    public native String stringFromJNI();
    public native void testFloatMat(long inputMat);

    static {
        System.loadLibrary("hello-jni");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
                mOpenCvCameraView.setOnTouchListener((View.OnTouchListener) RunSystem.this);
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public RunSystem() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_runsystem);

        Log.i("myTag", stringFromJNI());

        // File Cache Path
//        Context context = getApplicationContext();
//        File cacheDir = context.getCacheDir();
//        String cachePath = cacheDir.getAbsolutePath();
        Log.i("myTag", "STARTING Sound thing");
        envModel = new EnvModel();
        envModel.startEnvModelThread();
        Log.i("myTag", "FINISHED Sound thing");

//        envModelObjectTest();

        //ActionBar actionbar = getSupportActionBar();
        //assert actionbar != null;
        //actionbar.setBackgroundDrawable(new ColorDrawable(Color.RED));
        setTitle("Run System");

        if (checkPermission()) {
            //OpenCVLoader.initDebug();
            //main logic or main code
            // . write your main code to execute, It will execute if the permission is already given.
        } else {
            requestPermission();
        }

        mOpenCvCameraView = findViewById(R.id.stereo_camera_view2);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);

        // MUST CALL THIS AT START OF CODE TO BE ABLE TO USE OPENCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        Mat testMat = new Mat(1, 4, CvType.CV_32FC1);
        testMat.put(0, 0, 1.0f);
        testMat.put(0, 1, 2.0f);
        testMat.put(0, 2, 3.0f);
        testMat.put(0, 3, 4.0f);
        testFloatMat(testMat.getNativeObjAddr());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    private void envModelObjectTest() {
        YoloV5Ncnn yoloInst = new YoloV5Ncnn();
        YoloV5Ncnn.Obj yoloObj = yoloInst.new Obj();
        yoloObj.x = 0;
        yoloObj.y = 0;
        yoloObj.w = 0;
        yoloObj.h = 0;
        yoloObj.label = "BLAH BLAH";
        yoloObj.prob = 1.0f;
        YoloV5Ncnn.Obj[] yoloList = new YoloV5Ncnn.Obj[2];
        yoloList[0] = yoloObj;
        yoloObj.label = "NOPE";
        yoloList[1] = yoloObj;
        ImageToEnvData imageToEnvData = new ImageToEnvData(
                new StereoImageData(new MatOfInt(), new MatOfFloat(), 0),
                yoloList,
                0
        );
        envModel.passDetectedInfo(imageToEnvData);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {

        int cols = mRgba.cols();
        int rows = mRgba.rows();
        // YOLO added code
        /*Bitmap bmp = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bmp);
        YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(bmp, false);
        showObjects(objects); */
        // YOLO added code

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    //@Override
    //public void onPointerCaptureChanged(boolean hasCapture) {}

    private boolean checkPermission() {
        // Permission is not granted
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // from: https://stackoverflow.com/questions/42275906/how-to-ask-runtime-permissions-for-camera-in-android-runtime-storage-permissio
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    // from: https://stackoverflow.com/questions/42275906/how-to-ask-runtime-permissions-for-camera-in-android-runtime-storage-permissio
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                mOpenCvCameraView.setCameraPermissionGranted();
                // main logic
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    showMessageOKCancel(
                            (dialog, which) -> requestPermission());
                }
            }
        }
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(RunSystem.this)
                .setMessage("You need to allow access permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
