package com.epochconsulting.motoinventory.vehicletracker.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.activity.Home;
import com.epochconsulting.motoinventory.vehicletracker.activity.Login;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.PersistentCookieStoreManager;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BaseFragment;

import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;


public class LoginFragment extends BaseFragment {


    Button loginButton;
    private Login activity;

    EditText usernameentry;
    EditText passwordentry;
    TextView forgotpwd;
    JSONObject jsonObject;
    boolean isaNewLogin;
    String IS_NEW_LOGIN;


    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View loginFragment = inflater.inflate(R.layout.fragment_login, container, false);
        this.activity = (Login) this.getActivity();
        this.context = activity.getApplicationContext();
        forgotpwd = (TextView) loginFragment.findViewById(R.id.forgot_password);
        loginButton = (Button) loginFragment.findViewById(R.id.login_button);
        usernameentry = (EditText) loginFragment.findViewById(R.id.username_entry);
        passwordentry = (EditText) loginFragment.findViewById(R.id.password_entry);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showProgress();

                requestLogin();


            }
        });
        forgotpwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResetPasswordFragment resetPasswordFragment = new ResetPasswordFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.login_content, resetPasswordFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();


            }
        });
        CookieManager cookieManager = new CookieManager(new PersistentCookieStoreManager(context), CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        // Inflate the layout for this fragment
        return loginFragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.activity = (Login) this.getActivity();
        this.context = activity.getApplicationContext();
    }



    private void requestLogin() {

        RequestQueue requestQueue = Volley.newRequestQueue(LoginFragment.super.context);

        String myUrl = Utility.getInstance().buildUrl(Url.API_METHOD, null, Url.LOGIN_URL);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, myUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideProgress();
                String loginResponse = null;

                try {
                    if (response != null) {
                        jsonObject = new JSONObject(response);
                    }
                    loginResponse = jsonObject.get("message").toString();
                    if (loginResponse != null && loginResponse.equalsIgnoreCase(Constants.LOGIN_RESPONSE)) {
                        String loggedUser = jsonObject.get("full" +
                                "_name").toString();
                        String successmsg = Constants.LOGIN_SUCCESS + " " + loggedUser;
                        Toast.makeText(LoginFragment.super.context, successmsg, Toast.LENGTH_LONG).show();
                        isaNewLogin = true;
                        SharedPreferences.Editor editor = activity.getSharedPreferences(IS_NEW_LOGIN, MODE_PRIVATE).edit();
                        editor.putBoolean("IsNewUser", isaNewLogin);

                        editor.apply();
                        editor.commit();

                        //added on 25th Oct 2017 to tie user down to a warehouse
                        callHomePage();

                    } else {
                        Toast.makeText(LoginFragment.super.context, R.string.login_failed, Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    Log.e("ERROR", e.toString());
                }


            }


        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgress();
                Toast.makeText(LoginFragment.super.context, "Login Failed, Server Error: " + error.toString(), Toast.LENGTH_LONG).show();


            }
        })
                //end of error response and stringRequest params
        {
            //This is for providing body for Post request@Override
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> mapObject = new HashMap<>();
                mapObject.put(Constants.KEY_NAME, usernameentry.getText().toString());
                mapObject.put(Constants.KEY_PASSWORD, passwordentry.getText().toString());
                return mapObject;

            }
        };


        stringRequest.setRetryPolicy(new   DefaultRetryPolicy(30*1000,0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);



    }

    private void callHomePage() {
        Intent intent = new Intent(LoginFragment.super.getContext(), Home.class);

        startActivity(intent);
    }


}

