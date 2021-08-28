package com.SimuSound;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.SimuSound.utils.Constants;
import com.SimuSound.utils.ReadFloatFileIntoMat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.StereoSGBM;
import org.opencv.android.Utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class Stereo extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int SELECT_PICTURE_LEFT = 1;
    private static final int SELECT_PICTURE_RIGHT = 2;

    private static final String  TAG = "Stereo";

    private Button selectLeftImgButton;
    private Button selectRightImgButton;
    private Button computeStereoButton;

    private Bitmap left_img;
    private Bitmap right_img;
    private Bitmap left_img_undistorted;
    private Bitmap right_img_undistorted;
    private Bitmap stereo_map_img;

    private ImageView left_img_view;
    private ImageView right_img_view;
    private ImageView left_img_view_undistorted;
    private ImageView right_img_view_undistorted;
    private ImageView stereo_map_view;

    private Mat left_img_mat;
    private Mat right_img_mat;
    private Mat stereo_map_img_mat;

    private Mat mapL1Mat;
    private Mat mapL2Mat;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public Stereo() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_img_button: {
//                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setType("image/*");
//                startActivityForResult(Intent.createChooser(intent, "Select Left Picture"), SELECT_PICTURE_LEFT);

                getCalibrationData();

                AssetManager assetManager = getAssets();
                InputStream istr;
                Bitmap bitmap = null;
                try {
                    istr = assetManager.open("testcalimg.png");
                    bitmap = BitmapFactory.decodeStream(istr);
                } catch (IOException e) {
                    // handle exception
                }
                left_img_view.setImageBitmap(bitmap);

                Mat imageMat = new Mat(2048, 1536, CvType.CV_8UC3);
                Utils.bitmapToMat(bitmap, imageMat);

                left_img_mat = new Mat(2048, 1536, CvType.CV_8UC3);
                Imgproc.remap(imageMat, left_img_mat, mapL1Mat, mapL2Mat, Imgproc.INTER_LINEAR);

                left_img = Bitmap.createBitmap(2048, 1536, Bitmap.Config.ARGB_8888);

                Mat rgb = new Mat();
                Imgproc.cvtColor(left_img_mat, rgb, Imgproc.COLOR_BGR2RGBA);
                left_img_mat = rgb;

                Utils.matToBitmap(left_img_mat, left_img);
                left_img_view_undistorted.setImageBitmap(left_img);

                break;
            }
            case R.id.right_img_button: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Right Picture"), SELECT_PICTURE_RIGHT);
                break;
            }
            case R.id.stereo_match_button: {
                this.undistortImages();
                this.computeStereo();
                break;
            }
        }
    }

    protected void getCalibrationData() {
        // file is packed F32BE, extension is .wav to avoid compression
        mapL1Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapL1_BE.wav", this);
        Log.e(TAG, "mapL1Mat[0][0]=" + mapL1Mat.get(0, 0)[0]);
        Log.e(TAG, "mapL1Mat[0][1]=" + mapL1Mat.get(0, 1)[0]);
        Log.e(TAG, "mapL1Mat[1][0]=" + mapL1Mat.get(1, 0)[0]);

        mapL2Mat = new ReadFloatFileIntoMat().MatFromFileFaster(Constants.stereoWidth, Constants.stereoHeight, CvType.CV_32FC1, "/sdcard/Download/mapL2_BE.wav", this);
        Log.e(TAG, "mapL2Mat[0][0]=" + mapL2Mat.get(0, 0)[0]);
        Log.e(TAG, "mapL2Mat[0][1]=" + mapL2Mat.get(0, 1)[0]);
        Log.e(TAG, "mapL2Mat[1][0]=" + mapL2Mat.get(1, 0)[0]);

    }

    protected void undistortImages() {

    }

    protected void computeStereo() {
        left_img_mat = new Mat(left_img.getWidth(), left_img.getHeight(), CvType.CV_8UC3);
        right_img_mat = new Mat(left_img.getWidth(), left_img.getHeight(), CvType.CV_8UC3);

        Utils.bitmapToMat(left_img, left_img_mat);
        Utils.bitmapToMat(right_img, right_img_mat);

        createDisparityMap(left_img_mat, right_img_mat);

        stereo_map_img = Bitmap.createBitmap(stereo_map_img_mat.cols(), stereo_map_img_mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(stereo_map_img_mat, stereo_map_img);
        stereo_map_view.setImageBitmap(stereo_map_img);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (reqCode == SELECT_PICTURE_LEFT) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    left_img = BitmapFactory.decodeStream(imageStream);
                    left_img_view.setImageBitmap(left_img);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(Stereo.this, "Something went wrong1", Toast.LENGTH_LONG).show();
                }
            }
            else if (reqCode == SELECT_PICTURE_RIGHT) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    right_img = BitmapFactory.decodeStream(imageStream);
                    right_img_view.setImageBitmap(right_img);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(Stereo.this, "Something went wrong2", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(Stereo.this, "You didn't pick an image", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_stereo);

        //ActionBar actionbar = getSupportActionBar();
        //assert actionbar != null;
        //actionbar.setBackgroundDrawable(new ColorDrawable(Color.RED));
        setTitle("Stereo");

        if (checkPermission()) {
//            OpenCVLoader.initDebug();
            //main logic or main code
            // . write your main code to execute, It will execute if the permission is already given.
        } else {
            requestPermission();
        }

        // MUST CALL THIS AT START OF CODE TO BE ABLE TO USE OPENCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


        selectLeftImgButton = findViewById(R.id.left_img_button);
        selectRightImgButton = findViewById(R.id.right_img_button);
        computeStereoButton = findViewById(R.id.stereo_match_button);
        left_img_view = findViewById(R.id.left_img);
        right_img_view = findViewById(R.id.right_img);
        left_img_view_undistorted = findViewById(R.id.left_img_undistorted);
        right_img_view_undistorted = findViewById(R.id.right_img_undistorted);
        stereo_map_view = findViewById(R.id.stereo_match_filtered);

        selectLeftImgButton.setOnClickListener(this);
        selectRightImgButton.setOnClickListener(this);
        computeStereoButton.setOnClickListener(this);
    }

    private Mat createDisparityMap(Mat rectLeft, Mat rectRight){

        // Converts the images to a proper type for stereoMatching
        Mat left = new Mat(rectLeft.size(), CvType.CV_8UC1);
        Mat right = new Mat(rectLeft.size(), CvType.CV_8UC1);

        Imgproc.cvtColor(rectLeft, left, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(rectRight, right, Imgproc.COLOR_BGR2GRAY);

        // Create a new image using the size and type of the left image
        stereo_map_img_mat = new Mat(left.size(), CvType.CV_8UC1);

        int numDisparity = (int)(left.size().width/8);

        StereoSGBM stereoAlgo = StereoSGBM.create(
                0,    // min DIsparities
                numDisparity, // numDisparities
                11,   // SADWindowSize
                2*11*11,   // 8*number_of_image_channels*SADWindowSize*SADWindowSize   // p1
                5*11*11,  // 8*number_of_image_channels*SADWindowSize*SADWindowSize  // p2

                -1,   // disp12MaxDiff
                63,   // prefilterCap
                10,   // uniqueness ratio
                0, // sreckleWindowSize
                32, // spreckle Range
                0); // full DP
        // create the DisparityMap - SLOW: O(Width*height*numDisparity)
        stereoAlgo.compute(left, right, stereo_map_img_mat);

        Core.normalize(stereo_map_img_mat, stereo_map_img_mat, 0, 256, Core.NORM_MINMAX);

        stereo_map_img_mat.convertTo(stereo_map_img_mat, CvType.CV_8UC1);

        return stereo_map_img_mat;
    }

    @Override
    public void onPause()
    {
        super.onPause();
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
    }

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
        new AlertDialog.Builder(Stereo.this)
                .setMessage("You need to allow access permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
