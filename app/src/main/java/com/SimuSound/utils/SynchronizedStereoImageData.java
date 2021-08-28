package com.SimuSound.utils;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.graphics.Bitmap;
import android.graphics.Color;

public class SynchronizedStereoImageData {
    public ReentrantReadWriteLock mReadWriteLock;
    public Bitmap imageL;
    public Bitmap imageR;
    public long timestampL;
    public long timestampR;

    public SynchronizedStereoImageData(int width, int height) {
        mReadWriteLock = new ReentrantReadWriteLock();
        imageL = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        imageL.eraseColor(Color.BLUE);
        imageR = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        imageR.eraseColor(Color.LTGRAY);
        timestampL = 1;
        timestampR = 2;
    }

    // TODO: make function to do locking and reading/writing together
}
