package com.epochconsulting.motoinventory.vehicletracker.activity;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;


import com.epochconsulting.motoinventory.vehicletracker.fragments.LoginFragment;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;

public class Login extends AppCompatActivity {


    String MY_PREFS_NAME;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String serverAddress = prefs.getString("serverAddress", null);



        if(serverAddress!=null){
            Url.setServerAddress(serverAddress);
        }

        callloginFragment();

    }


    private void callloginFragment() {
        LoginFragment loginFragment = new LoginFragment();
        addFragment(loginFragment);

    }


    public void addFragment(Fragment fragment1) {
        FragmentTransaction fragmentTransaction;
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.login_content, fragment1);
        fragmentTransaction.commitAllowingStateLoss();

    }


}
