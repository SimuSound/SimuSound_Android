/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.SimuSound;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.ImageReader;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;

/**
 * Show side by side view from two camera.
 * You cane record video images from both camera, but secondarily started recording can not record
 * audio because of limitation of Android AudioRecord(only one instance of AudioRecord is available
 * on the device) now.
 */
public final class USBCamera extends BaseActivity implements CameraDialog.CameraDialogParent, ImageReader.OnImageAvailableListener {
	private static final boolean DEBUG = false;
	private static final String TAG = "USBCamera";
	private int width = 2048;
	private int height = 1536;

	private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

	private UVCCameraHandler mHandlerR;
	private UVCCameraTextureView mUVCCameraViewR;
	private Surface mRightPreviewSurface;
	private ImageView mLeftOpenCVImage;
	private TextView mRightText;

	private UVCCameraHandler mHandlerL;
	private ImageView mFrameImageL;
	private Surface mLeftPreviewSurface;
	private ImageView mRightOpenCVImage;
	private TextView mLeftText;

//	private WorkerThread myThread;

	private ImageReader mImageReader;

//	class WorkerThread extends Thread {
//		private int count = 0;
//		public volatile boolean running = true;
//		private Handler image_handler = new Handler(Looper.myLooper());
//		public Bitmap myBitmap;
//		public double fps = 0;
//		public double lastTime = 1;
//		public double currTime = 1;

//
//		public void run() {
//			try {
//				sleep(3000);
//			} catch (InterruptedException consumed) {
//				return;
//			}
//
//			while(true) {
//				if (!running) {
//					return;
//				}
//
//				if (mUVCCameraViewL != null) {
//					myBitmap = mUVCCameraViewL.captureStillImage();
//				} else {
//					return;
//				}
//				USBCamera.this.runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
//						currTime = System.currentTimeMillis() / 1000.0;
//						mLeftOpenCVImage.setImageBitmap(myBitmap);
//						mLeftText.setText("got frame " + count);
//						fps = 1 / (currTime - lastTime);
//						mRightText.setText("fps = " + fps);
//						count += 1;
//						lastTime = currTime;
//					}
//				});
//
//				try {
//					sleep(1);
//				} catch (InterruptedException consumed) {
//					return;
//				}
//			}
//		}
//	}


	@SuppressLint("WrongConstant")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_usb_cameras);

		mFrameImageL = (ImageView)findViewById(R.id.frame_image_L);
		mLeftOpenCVImage = (ImageView)findViewById(R.id.opencv_L);
		mLeftText = (TextView) findViewById(R.id.text_L);

		mUVCCameraViewR = (UVCCameraTextureView)findViewById(R.id.camera_view_R);
		mRightOpenCVImage = (ImageView)findViewById(R.id.opencv_R);
		mRightText = (TextView) findViewById(R.id.text_R);

//		findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);

		mHandlerL = UVCCameraHandler.createHandler(this, null, width, height, BANDWIDTH_FACTORS[0]);
		((ImageView)mFrameImageL).setOnClickListener(mOnClickListener);

		mUVCCameraViewR.setAspectRatio(width / (float)height);
		((UVCCameraTextureView)mUVCCameraViewR).setOnClickListener(mOnClickListener);
		mHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR, width, height, BANDWIDTH_FACTORS[1]);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

		mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
		mImageReader.setOnImageAvailableListener(USBCamera.this, null);
	}

	public void onImageAvailable(ImageReader reader) {
		Image myImage = reader.acquireLatestImage();

		if (myImage != null) {
			final Image.Plane[] planes = myImage.getPlanes();
			final ByteBuffer buffer = planes[0].getBuffer();
			int offset = 0;
			int pixelStride = planes[0].getPixelStride();
			int rowStride = planes[0].getRowStride();
			int rowPadding = rowStride - pixelStride * width;

			Bitmap myBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
			myBitmap.copyPixelsFromBuffer(buffer);
			myImage.close();

			mLeftOpenCVImage.setImageBitmap(myBitmap);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mUSBMonitor.register();
		if (mUVCCameraViewR != null)
			mUVCCameraViewR.onResume();
//		if (mUVCCameraViewL != null)
//			mUVCCameraViewL.onResume();
	}

	@Override
	protected void onStop() {
		mHandlerR.close();
		if (mUVCCameraViewR != null)
			mUVCCameraViewR.onPause();
		mHandlerL.close();
//		if (mUVCCameraViewL != null)
//			mUVCCameraViewL.onPause();
		mUSBMonitor.unregister();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mHandlerR != null) {
			mHandlerR = null;
  		}
		if (mHandlerL != null) {
			mHandlerL = null;
  		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraViewR = null;
//		mUVCCameraViewL = null;
		super.onDestroy();
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.frame_image_L:
				if (mHandlerL != null) {
					if (!mHandlerL.isOpened()) {
						CameraDialog.showDialog(USBCamera.this);
					} else {
						mHandlerL.close();
					}
				}
				break;
			case R.id.camera_view_R:
				if (mHandlerR != null) {
					if (!mHandlerR.isOpened()) {
						CameraDialog.showDialog(USBCamera.this);
					} else {
						mHandlerR.close();
					}
				}
				break;
			}
		}
	};

	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onAttach:" + device);
			Toast.makeText(USBCamera.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.v(TAG, "onConnect:" + device);
			if (!mHandlerL.isOpened()) {
				mHandlerL.open(ctrlBlock);
//				final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
//				mHandlerL.startPreview(new Surface(st));
				mHandlerL.startPreview(mImageReader.getSurface());

//				myThread = new WorkerThread();
//				myThread.start();
			} else if (!mHandlerR.isOpened()) {
				mHandlerR.open(ctrlBlock);
				final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
				mHandlerR.startPreview(new Surface(st));
			}
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
			if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
//						myThread.running = false;
//						myThread.interrupt();

						mHandlerL.close();
						if (mLeftPreviewSurface != null) {
							mLeftPreviewSurface.release();
							mLeftPreviewSurface = null;
						}
					}
				}, 0);
			} else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mHandlerR.close();
						if (mRightPreviewSurface != null) {
							mRightPreviewSurface.release();
							mRightPreviewSurface = null;
						}
					}
				}, 0);
			}
		}

		@Override
		public void onDettach(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onDettach:" + device);
			Toast.makeText(USBCamera.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel(final UsbDevice device) {
			if (DEBUG) Log.v(TAG, "onCancel:");
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (canceled) {
			Toast.makeText(USBCamera.this, "cancelled", Toast.LENGTH_SHORT).show();
		}
	}
}
