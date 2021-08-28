package com.SimuSound.tasks;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;

import com.SimuSound.R;
import com.SimuSound.utils.Constants;
import com.tencent.yolov5ncnn.YoloV5Ncnn;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class ObjectDetectionThread extends HandlerThread {
    private static final boolean DEBUG = true;
    private static final String TAG = "ObjectDetectionThread";
    private static final int width = 2048;
    private static final int height = 1536;

    private Activity mMainActivity;

    public LinkedBlockingQueue<Bitmap> mStereoToObjectDetectionData;
    public LinkedBlockingQueue<YoloV5Ncnn.Obj[]> mObjectDetectionData;

    private Bitmap mSrcImage;
    private ImageView mObjectDetectionView;
    YoloV5Ncnn.Obj[] mObjects;

    private YoloV5Ncnn mYolov5ncnn;

    public ObjectDetectionThread(Activity mainActivity, YoloV5Ncnn yolov5ncnn, LinkedBlockingQueue<Bitmap> stereoToObjectDetectionData, LinkedBlockingQueue<YoloV5Ncnn.Obj[]> objectDetectionData) {
        super(TAG);
        mMainActivity = mainActivity;
        mYolov5ncnn = yolov5ncnn;
        mStereoToObjectDetectionData = stereoToObjectDetectionData;
        mObjectDetectionData = objectDetectionData;
    }

    @Override
    public void run() {
        Log.d(TAG, TAG + " started");

        mMainActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mObjectDetectionView = mMainActivity.findViewById(R.id.yolo_image);
            }
        });

        // TODO: remove spaghet
        while (mObjectDetectionView == null) {
            try { sleep(100); } catch (InterruptedException e) {}
        }
        try { sleep(100); } catch (InterruptedException e) {}

        // TODO: thread exit condition
        while (true) {
            try {
                mSrcImage = mStereoToObjectDetectionData.take();
                Log.e(TAG, TAG + " get bitmap from yolo queue");
            } catch (InterruptedException e) {
                Log.e(TAG, TAG + " interrupted");
            }

            Bitmap myBitmap = mSrcImage.copy(mSrcImage.getConfig(), true);

            mObjects = mYolov5ncnn.Detect(myBitmap, true);

            // display highest probability detected objects first
            Arrays.sort(mObjects, Collections.reverseOrder());

            // debug
            for (int i = 0; i < mObjects.length; i++) {
                Log.e(TAG, "mObjects " + i + ": " + mObjects[i].toString());
            }

            mObjectDetectionData.offer(mObjects); // TODO: check data is thread safe like this
            Log.e(TAG, "mObjectDetectionData size=" + mObjectDetectionData.size());



//            List<YoloV5Ncnn.Obj> objectsList = Arrays.asList(mObjects);
//            Collections.sort(objectsList, new Comparator<YoloV5Ncnn.Obj>() {
//                @Override
//                public int compare(YoloV5Ncnn.Obj o1, YoloV5Ncnn.Obj o2) {
//                    return Float.compare(o1.prob, o2.prob);
//                }
//            });

            if (Constants.enableImagePreviews) {
                Bitmap rgba = AddObjectsToImage(mObjects, mSrcImage);

                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mObjectDetectionView.setImageBitmap(rgba);
                    }
                });
            }

            // TODO: output Objs
        }
    }

    private Bitmap AddObjectsToImage(YoloV5Ncnn.Obj[] objects, Bitmap image) {

        if (objects == null)
        {
            mObjectDetectionView.setImageBitmap(image);
            return image;
        }

        // draw objects on bitmap
        Bitmap rgba = image.copy(Bitmap.Config.ARGB_8888, true);

        final int[] colors = new int[] {
                Color.rgb( 54,  67, 244),
                Color.rgb( 99,  30, 233),
                Color.rgb(176,  39, 156),
                Color.rgb(183,  58, 103),
                Color.rgb(181,  81,  63),
                Color.rgb(243, 150,  33),
                Color.rgb(244, 169,   3),
                Color.rgb(212, 188,   0),
                Color.rgb(136, 150,   0),
                Color.rgb( 80, 175,  76),
                Color.rgb( 74, 195, 139),
                Color.rgb( 57, 220, 205),
                Color.rgb( 59, 235, 255),
                Color.rgb(  7, 193, 255),
                Color.rgb(  0, 152, 255),
                Color.rgb( 34,  87, 255),
                Color.rgb( 72,  85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125,  96)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(60);
        textpaint.setTextAlign(Paint.Align.LEFT);

        // display at most 4 objects, highest probability first
        for (int i = 0; i < Math.min(objects.length, 4); i++)
        {
            // don't draw if the probability is too low
            if (objects[i].prob < 0.5f) {
                break;
            }
            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // draw filled text inside image
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        return rgba;
    }
}
