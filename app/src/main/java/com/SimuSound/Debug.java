package com.SimuSound;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Debug extends AppCompatActivity {
    private Button start_stop_button;
    private boolean system_stopped;
    private UsbManager mUSBmgr;
    private ImageView img_in_left; // input image 1 (left camera)
    private ImageView img_in_right; // input image 2 (right camera)
    private ImageView img_processed; // for debugging; should show image w/ identified objects


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // initialize all supporting objects and functions
        system_stopped = false;
        start_stop_button = findViewById(R.id.start_stop_system_button);
        start_stop_button.setOnClickListener(v -> start_stop_system_handler());

        img_in_left = findViewById(R.id.image_in_left);
        img_in_right = findViewById(R.id.image_in_right);
        img_processed = findViewById(R.id.image_out);

        UsbManager mUSBmgr = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUSBmgr.getAccessoryList();

        // start system
        start_system();
    }

    void start_stop_system_handler() {
        if(system_stopped) { // system is stopped, need to start it
            start_system();
            start_stop_button.setText(R.string.stop_button);
            system_stopped = false;
        }
        else { // system is running, need to stop it
            stop_system();
            start_stop_button.setText(R.string.start_button);
            system_stopped = true;
        }
    }

    void stop_system() { // stops system when requested

    }

    void start_system() { // run on activity start-up and when manually started after a stop

    }

}