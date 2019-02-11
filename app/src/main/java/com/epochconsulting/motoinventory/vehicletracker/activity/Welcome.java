package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.epochconsulting.motoinventory.vehicletracker.R;

public class Welcome extends AppCompatActivity {
    String MY_PREFS_NAME;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //start added on 21st Dec 2017 to solve a known android bug..funny behaviour of apk install and install thru studio
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }
        //end change
        setContentView(R.layout.activity_welcome);
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        boolean isServeraddressPresent = prefs.getBoolean("IsServeraddressFound", false);
        if (isServeraddressPresent) {
            intent = new Intent(Welcome.this, Login.class);
            startActivity(intent);

        } else {
            intent = new Intent(Welcome.this, Configuration.class);
            startActivity(intent);

        }

    }
}
