package com.SimuSound.tasks;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.HandlerThread;
import android.util.Log;

import com.SimuSound.EnvModel;
import com.tencent.yolov5ncnn.YoloV5Ncnn;

public class GyroAccelThread extends HandlerThread {
    private static final boolean DEBUG = true;
    private static final String TAG = "Gyroscope and Acceleration Sensor Thread";

    private Activity mMainActivity;

    private EnvModel mEnvModel;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;

    public GyroAccelThread(Activity mainActivity, EnvModel envModel) {
        super(TAG);
        Log.i(TAG, "Instantiated new " + this.getClass());
        mMainActivity = mainActivity;

        mEnvModel = envModel;

        sensorManager = (SensorManager) mMainActivity.getSystemService(mMainActivity.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); // type accelerometer
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void run() {
        Log.d(TAG, TAG + " started");

        // initialize sensor manager + accelerometer and gyroscope sensors
        // move to constructor

        SensorEventListener gyroscopeSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // values[0] = rate of rotation around X axis
                // values[1] = rate of rotation around Y axis
                // values[2] = rate of rotation around Z axis
//                mEnvModel.UpdateIMUGyro(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sensorManager.registerListener(gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);

        SensorEventListener accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // values[0] = acc force on X axis
                // values[1] = acc force on Y axis
                // values[2] = acc force on Z axis
//                mEnvModel.UpdateIMUAccel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
