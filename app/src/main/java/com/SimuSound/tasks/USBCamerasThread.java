package com.SimuSound.tasks;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.media.ImageReader;
import android.os.Looper;
import android.os.HandlerThread;
import android.os.Process;
import android.view.View;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.util.Log;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import com.SimuSound.R;
import com.SimuSound.utils.Constants;
import com.SimuSound.utils.ImageToEnvData;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usbcameracommon.UVCCameraHandler;

import com.SimuSound.utils.SynchronizedStereoImageData;

public class USBCamerasThread extends HandlerThread implements ImageReader.OnImageAvailableListener {
    private static final boolean DEBUG = true;
    private static final String TAG = "USBCamerasThread";
    private static final int width = 2048;
    private static final int height = 1536;
    private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

    private Activity mMainActivity;
    private Handler mMainActivityHandler;

    public SynchronizedStereoImageData mSyncData;

    // for accessing USB and USB camera
    public USBMonitor mUSBMonitor;

    private UVCCameraHandler mHandlerL;
    private ImageReader mImageReaderL;
    private ImageView mPreviewL;

    private UVCCameraHandler mHandlerR;
    private ImageReader mImageReaderR;
    private ImageView mPreviewR;

    private TextView mFrameDelayText;

    public USBCamerasThread(Activity mainActivity, SynchronizedStereoImageData syncLock) {
        super(TAG);
        mMainActivity = mainActivity;
        mMainActivityHandler = new Handler(Looper.getMainLooper());
        // mUSBMonitor set by creator of thread
        mSyncData = syncLock;
    }

    @Override
    public void run() {
        Log.d(TAG,TAG + " started");

        mMainActivity.runOnUiThread( new Runnable() {
            @SuppressLint("WrongConstant")
            @Override
            public void run() {
                mUSBMonitor.register();

                mHandlerL = UVCCameraHandler.createHandler(mMainActivity, null, width, height, BANDWIDTH_FACTORS[0]);
                mPreviewL = mMainActivity.findViewById(R.id.frame_image_L);
                mPreviewL.setOnClickListener(mOnClickListener);
                mImageReaderL = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
                mImageReaderL.setOnImageAvailableListener(USBCamerasThread.this, mMainActivityHandler);

                mHandlerR = UVCCameraHandler.createHandler(mMainActivity, null, width, height, BANDWIDTH_FACTORS[1]);
                mPreviewR = mMainActivity.findViewById(R.id.frame_image_R);
                mPreviewR.setOnClickListener(mOnClickListener);
                mImageReaderR = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
                mImageReaderR.setOnImageAvailableListener(USBCamerasThread.this, mMainActivityHandler);

                mFrameDelayText = mMainActivity.findViewById(R.id.frame_delay_text);

//                Process.setThreadPriority(Process.myTid(), -8);  // make highest priority so USB doesn't drop out
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void onImageAvailable(ImageReader reader) {
        // TODO: put Images in queue instead of bitmaps so don't have to decode every Image
        // that will mean no preview for USB images directly though
        long timestamp = System.currentTimeMillis();
        Image myImage = reader.acquireLatestImage();

        if (myImage != null) {
            Rect crop = myImage.getCropRect();


            final Image.Plane[] planes = myImage.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int offset = 0;
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

//            Log.e(TAG, "width=" + crop.width());
//            Log.e(TAG, "height=" + crop.height());
//            Log.e(TAG, "bottom=" + crop.bottom);
//            Log.e(TAG, "left=" + crop.left);
//            Log.e(TAG, "right=" + crop.right);
//            Log.e(TAG, "top=" + crop.top);

            Bitmap myBitmap = Bitmap.createBitmap(myImage.getWidth() + rowPadding / pixelStride, myImage.getHeight(), Bitmap.Config.ARGB_8888);
            myBitmap.copyPixelsFromBuffer(buffer);

            Bitmap croppedBitmap = Bitmap.createBitmap(myBitmap, crop.left, crop.top, crop.width(), crop.height());

            myImage.close();

//            mMainActivity.runOnUiThread( new Runnable() {
//                @Override
//                public void run() {
//                    mFrameDelayText.setText("croppedBitmap width=" + croppedBitmap.getWidth() + " height=" + croppedBitmap.getHeight());
//                }
//            });

            // TODO: idea to queue 2 images each for L and R and store time values so stereo matching can choose closest in time
            // have to display this so we know which is left and right
            if (reader == mImageReaderL) {
                mPreviewL.setImageBitmap(croppedBitmap);
            } else if (reader == mImageReaderR) {
                mPreviewR.setImageBitmap(croppedBitmap);
            } else {  // debug
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFrameDelayText.setText("imagereader didn't match");
                    }
                });
                return;
            }

            mSyncData.mReadWriteLock.writeLock().lock();
            if (reader == mImageReaderL) {
                mSyncData.imageL = croppedBitmap;
                mSyncData.timestampL = timestamp;
            } else if (reader == mImageReaderR) {
                mSyncData.imageR = croppedBitmap;
                mSyncData.timestampR = timestamp;
            }
            mSyncData.mReadWriteLock.writeLock().unlock();

            mMainActivity.runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    mFrameDelayText.setText("" + Math.abs(mSyncData.timestampL - mSyncData.timestampR) + " ms");
                }
            });
        }
    }


    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.frame_image_L:
                    if (mHandlerL != null) {
                        if (!mHandlerL.isOpened()) {
                            CameraDialog.showDialog(mMainActivity);
                        } else {
                            mHandlerL.close();
                        }
                    }
                    break;
                case R.id.frame_image_R:
                    if (mHandlerR != null) {
                        if (!mHandlerR.isOpened()) {
                            CameraDialog.showDialog(mMainActivity);
                        } else {
                            mHandlerR.close();
                        }
                    }
                    break;
            }
        }
    };

    public final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:" + device);
            Toast.makeText(mMainActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:" + device);
            if (!mHandlerL.isOpened()) {
                mHandlerL.open(ctrlBlock);
                mHandlerL.startPreview(mImageReaderL.getSurface());
            } else if (!mHandlerR.isOpened()) {
                mHandlerR.open(ctrlBlock);
                mHandlerR.startPreview(mImageReaderR.getSurface());
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
            if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
                mHandlerL.close();
            } else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
                mHandlerR.close();
            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDettach:" + device);
            Toast.makeText(mMainActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

}
