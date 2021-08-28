package com.SimuSound.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

// TODO: make faster
public class ReadFloatFileIntoMat {
    public Mat MatFromFile(int width, int height, int cvType, String fileName, Context c) {
        Mat matFromFile = new Mat(height, width, cvType);
        try {
            AssetManager assetManager = c.getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd(fileName);
            FileInputStream fis = fileDescriptor.createInputStream();
            DataInputStream ds = new DataInputStream(fis);

            float floatVals[] = new float[width * height];

            int num_vals = width * height;
            for (int i = 0; i < num_vals; i++) {
                floatVals[i] = ds.readFloat();
            }

            if (ds.available() > 0) {
                throw new IOException("bytes left over from reading " + fileName);
            }

            matFromFile.put(0, 0, floatVals);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matFromFile;
    }

    public Mat MatFromFileFaster(int width, int height, int cvType, String pathName, Context c) {
        Mat matFromFile = new Mat(height, width, cvType);


        float[] readback=new float[width*height];
        try{

            RandomAccessFile rFile = new RandomAccessFile(pathName, "r");
            FileChannel inChannel = rFile.getChannel();
            ByteBuffer buf_in = ByteBuffer.allocate(width*height*4);
            buf_in.clear();

            inChannel.read(buf_in);

            buf_in.rewind();
            buf_in.asFloatBuffer().get(readback);

            inChannel.close();

        }
        catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        matFromFile.put(0, 0, readback);

        return matFromFile;
    }
}
