package com.SimuSound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // map button vars to xml buttons
        Button run_system_button = findViewById(R.id.start_button);
        run_system_button.setOnClickListener(v -> openRunSystem());
        //run_system_button.setOnClickListener(v -> openDebug());

        Button yolo_button = findViewById(R.id.test_yolo_button);
        yolo_button.setOnClickListener(v -> openYolo());

        Button int_button = findViewById(R.id.gyro_acc);
        int_button.setOnClickListener(v-> openIntegrate());

        Button stereo_button = findViewById(R.id.stereo_button);
        stereo_button.setOnClickListener(v-> openStereo());

        Button usb_camera_button = findViewById(R.id.usb_camera_button);
        usb_camera_button.setOnClickListener(v-> openUSBCameras());

        Button final_app_button = findViewById(R.id.final_app_button);
        final_app_button.setOnClickListener(v-> openFinalApp());



        androidx.core.app.ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MANAGE_EXTERNAL_STORAGE },
                101
        );

    }

    public void openRunSystem() {
        Intent intent = new Intent(this, RunSystem.class);
        startActivity(intent);
    }

    public void openDebug() {
        Intent intent = new Intent(this, Debug.class);
        startActivity(intent);
    }

    public void openYolo() {
        Intent intent = new Intent(this, com.tencent.yolov5ncnn.MainActivity.class);
        startActivity(intent);
    }

    public void openIntegrate() {
        Intent intent = new Intent(this, test_gyro_acceler.class);
        startActivity(intent);
    }

    public void openStereo() {
        Intent intent = new Intent(this, Stereo.class);
        startActivity(intent);
    }

    public void openUSBCameras() {
        Intent intent = new Intent(this, USBCamera.class);
        startActivity(intent);
    }

    public void openFinalApp() {
        Intent intent = new Intent(this, FinalApp.class);
        startActivity(intent);
    }

}