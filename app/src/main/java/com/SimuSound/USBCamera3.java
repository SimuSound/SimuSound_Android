///*
// *  UVCCamera
// *  library and sample to access to UVC web camera on non-rooted Android device
// *
// * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *   You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// *
// *  All files in the folder are under this Apache License, Version 2.0.
// *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
// *  may have a different license, see the respective files.
// */
//
//package com.SimuSound;
//
//import android.graphics.SurfaceTexture;
//import android.hardware.usb.UsbDevice;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.Surface;
//import android.view.TextureView;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.serenegiant.common.BaseActivity;
//import com.serenegiant.usb.CameraDialog;
//import com.serenegiant.usb.IButtonCallback;
//import com.serenegiant.usb.IStatusCallback;
//import com.serenegiant.usb.USBMonitor;
//import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
//import com.serenegiant.usb.USBMonitor.UsbControlBlock;
//import com.serenegiant.usb.UVCCamera;
//import com.serenegiant.usb.IFrameCallback;
//import com.serenegiant.usbcameracommon.UVCCameraHandler;
//import com.serenegiant.widget.CameraViewInterface;
//import com.serenegiant.widget.UVCCameraTextureView;
//
//import java.nio.ByteBuffer;
//
///**
// * Show side by side view from two camera.
// * You cane record video images from both camera, but secondarily started recording can not record
// * audio because of limitation of Android AudioRecord(only one instance of AudioRecord is available
// * on the device) now.
// */
//public final class USBCamera extends BaseActivity implements CameraDialog.CameraDialogParent {
//    private static final boolean DEBUG = false;
//    private static final String TAG = "USBCamera";
//
//    private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };
//
//    // for accessing USB and USB camera
//    private USBMonitor mUSBMonitor;
//
//    private UVCCamera mCameraR;
//    private UVCCameraTextureView mUVCCameraViewR;
//    private Surface mRightPreviewSurface;
//    private ImageView mLeftOpenCVImage;
//    private TextView mLeftText;
//
//    private UVCCamera mCameraL;
//    private UVCCameraTextureView mUVCCameraViewL;
//    private Surface mLeftPreviewSurface;
//    private ImageView mRightOpenCVImage;
//    private TextView mRightText;
//
//
//    @Override
//    protected void onCreate(final Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_usb_cameras);
//
//        mUVCCameraViewL = (UVCCameraTextureView)findViewById(R.id.camera_view_L);
//        mLeftOpenCVImage = (ImageView)findViewById(R.id.opencv_L);
//        mLeftText = (TextView) findViewById(R.id.text_L);
//
//        mUVCCameraViewR = (UVCCameraTextureView)findViewById(R.id.camera_view_R);
//        mLeftOpenCVImage = (ImageView)findViewById(R.id.opencv_R);
//        mLeftText = (TextView) findViewById(R.id.text_R);
//
////		findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
//
//        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//        ((UVCCameraTextureView)mUVCCameraViewL).setOnClickListener(mOnClickListener);
//
//        ((UVCCameraTextureView)mUVCCameraViewR).setOnClickListener(mOnClickListener);
//        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);
//
//        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        mUSBMonitor.register();
//        if (mUVCCameraViewR != null)
//            mUVCCameraViewR.onResume();
//        if (mUVCCameraViewL != null)
//            mUVCCameraViewL.onResume();
//    }
//
//    @Override
//    protected void onStop() {
//        if (mUVCCameraViewR != null)
//            mUVCCameraViewR.stopPreview();
//        if (mUVCCameraViewL != null)
//            mUVCCameraViewL.stopPreview();
//        mUSBMonitor.unregister();
//        super.onStop();
//    }
//
//    @Override
//    protected void onDestroy() {
//        if (mUSBMonitor != null) {
//            mUSBMonitor.destroy();
//            mUSBMonitor = null;
//        }
//        mUVCCameraViewR = null;
//        mUVCCameraViewL = null;
//        super.onDestroy();
//    }
//
//    private final OnClickListener mOnClickListener = new OnClickListener() {
//        @Override
//        public void onClick(final View view) {
//            switch (view.getId()) {
//                case R.id.camera_view_L:
//                    if (mCameraL != null) {
//                        CameraDialog.showDialog(USBCamera.this);
//                    }
//                    break;
//                case R.id.camera_view_R:
//                    if (mCameraR != null) {
//                        CameraDialog.showDialog(USBCamera.this);
//                    }
//                    break;
//            }
//        }
//    };
//
//    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
//        @Override
//        public void onAttach(final UsbDevice device) {
//            if (DEBUG) Log.v(TAG, "onAttach:" + device);
//            Toast.makeText(USBCamera.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
//        }
//
//        @Override
//        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
//            releaseCamera();
//            queueEvent(new Runnable() {
//                @Override
//                public void run() {
//                    final UVCCamera camera = new UVCCamera();
//                    camera.open(ctrlBlock);
//                    if (mPreviewSurface != null) {
//                        mPreviewSurface.release();
//                        mPreviewSurface = null;
//                    }
//                    try {
//                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
//                    } catch (final IllegalArgumentException e) {
//                        // fallback to YUV mode
//                        try {
//                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
//                        } catch (final IllegalArgumentException e1) {
//                            camera.destroy();
//                            return;
//                        }
//                    }
//                    final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
//                    if (st != null) {
//                        mPreviewSurface = new Surface(st);
//                        camera.setPreviewDisplay(mPreviewSurface);
////						camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
//                        camera.startPreview();
//                        camera.setFrameCallback(MainActivity.this, UVCCamera.PIXEL_FORMAT_NV21);
//                    }
//                    synchronized (mSync) {
//                        mUVCCamera = camera;
//                    }
//                }
//            }, 0);
//        }
//
//        @Override
//        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
//            if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
//            if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
//                queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        mHandlerL.close();
//                        if (mLeftPreviewSurface != null) {
//                            mLeftPreviewSurface.release();
//                            mLeftPreviewSurface = null;
//                        }
//                    }
//                }, 0);
//            } else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
//                queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        mHandlerR.close();
//                        if (mRightPreviewSurface != null) {
//                            mRightPreviewSurface.release();
//                            mRightPreviewSurface = null;
//                        }
//                    }
//                }, 0);
//            }
//        }
//
//        @Override
//        public void onDettach(final UsbDevice device) {
//            if (DEBUG) Log.v(TAG, "onDettach:" + device);
//            Toast.makeText(USBCamera.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
//        }
//
//        @Override
//        public void onCancel(final UsbDevice device) {
//            if (DEBUG) Log.v(TAG, "onCancel:");
//        }
//    };
//
//    /**
//     * to access from CameraDialog
//     * @return
//     */
//    @Override
//    public USBMonitor getUSBMonitor() {
//        return mUSBMonitor;
//    }
//
//    @Override
//    public void onDialogResult(boolean canceled) {
//        if (canceled) {
//            Toast.makeText(USBCamera.this, "cancelled", Toast.LENGTH_SHORT).show();
//        }
//    }
//}
