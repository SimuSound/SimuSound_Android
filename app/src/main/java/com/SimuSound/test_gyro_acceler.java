package com.SimuSound;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.commons.math3.analysis.integration.*;
import org.apache.commons.math3.analysis.polynomials.*;

import androidx.appcompat.app.AppCompatActivity;

public class test_gyro_acceler extends AppCompatActivity {
    private static final String  TAG = "integrate_yolo_opencv";

    // integration functions
    TrapezoidIntegrator trapInt;

    // accelerometer, gyroscope
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    // for debugging
    private TextView gyroDirection;
    private TextView gyroX;
    private TextView gyroY;
    private TextView gyroZ;
    private TextView accX;
    private TextView accY;
    private TextView accZ;

    public test_gyro_acceler() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_gyro_acceler);
        setTitle("Gyro and Accelerometer");

        // initialize integrator
        trapInt = new TrapezoidIntegrator();

        // initialize gyro and acc sensors
        gyroDirection = findViewById(R.id.gyroDirection);
        gyroX = findViewById(R.id.gyroX);
        gyroY = findViewById(R.id.gyroY);
        gyroZ = findViewById(R.id.gyroZ);
        accX = findViewById(R.id.accX);
        accY = findViewById(R.id.accY);
        accZ = findViewById(R.id.accZ);

        // initialize sensor manager + accelerometer and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        SensorEventListener gyroscopeSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // values[0] = rate of rotation around X axis
                // values[1] = rate of rotation around Y axis
                // values[2] = rate of rotation around Z axis
                gyroX.setText(String.valueOf(sensorEvent.values[0]));
                gyroY.setText(String.valueOf(sensorEvent.values[1]));
                gyroZ.setText(String.valueOf(sensorEvent.values[2]));
                if(sensorEvent.values[2] > 0.5f) { // anticlockwise
                    gyroDirection.setText(R.string.ccw);
                } else if(sensorEvent.values[2] < -0.5f) { // clockwise
                    gyroDirection.setText(R.string.cw);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);

        SensorEventListener accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // TODO: compute integral of acceleration to find position
                // values[0] = acc force on X axis
                // values[1] = acc force on Y axis
                // values[2] = acc force on Z axis
                accX.setText(String.valueOf(sensorEvent.values[0]));
                accY.setText(String.valueOf(sensorEvent.values[1]));
                accZ.setText(String.valueOf(sensorEvent.values[2]));
                //trapInt.integrate()
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
    }

    public void onDestroy() {
        super.onDestroy();
    }
}
