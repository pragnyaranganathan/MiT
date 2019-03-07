package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.epochconsulting.motoinventory.vehicletracker.R;

import com.epochconsulting.motoinventory.vehicletracker.fragments.SelectModeFragment;
import com.epochconsulting.motoinventory.vehicletracker.fragments.WarehouseLocationFragment;
import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.util.AlertDialogHandler;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.UserData;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.UserDetails;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;
import com.google.gson.JsonArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home extends BasicActivity
        implements NavigationView.OnNavigationItemSelectedListener, SelectModeFragment.OnTaskSelectedListener {

    private FragmentTransaction fragmentTransaction;
    private String loggedInUser;
    private String loggedInUserBaseWHLoc;
    TextView toolbarUserName;
    public TextView toolbarUserWHLoc;
    public TextView toolbarTruckWH;
    String IS_NEW_LOGIN;
    int taskSelected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //added on 25th Oct 2017, to tie user down to WH...get user id from cookies stored


        //added on 25th Oct 2017, to display user name and WH on the top right corner of the screen in toolbar

        toolbarUserName = (TextView) navigationView.getHeaderView(0).findViewById((R.id.nav_header_username));
        toolbarUserWHLoc = (TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_header_warehouse);
        toolbarTruckWH = (TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_header_truckwarehouse);


        SharedPreferences preferences = getSharedPreferences(IS_NEW_LOGIN, MODE_PRIVATE);
        boolean isThisANewLoginSession = preferences.getBoolean("IsNewUser",false);
        if(isThisANewLoginSession) {


            getLoggedUser();
        }
        else { //not a new login session
            toolbarUserName.setText(getCurrentLoggedUserId());
            loadSelectModeFragment();

        }



    }
    private void loadSelectModeFragment(){
        SelectModeFragment selectModeFragment = new SelectModeFragment();
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.home_content, selectModeFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

    }



    private void loadFindWarehousefragment() {
        WarehouseLocationFragment fragment = new WarehouseLocationFragment();
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.home_content, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            //logout of the system in the backend and flush the stack etc and call the login page again

            Toast.makeText(Home.this,"Please wait...logging you out! ", Toast.LENGTH_LONG).show();
            logoutprocess();


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    public void getLoggedUser()
    {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

       String myUrl = Utility.getInstance().buildUrl(Url.API_METHOD, null,Url.GET_LOGGED_USER);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, myUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    loggedInUser = object.getString("message");
                    setCurrentLoggedUserId(loggedInUser);
                    toolbarUserName.setText(loggedInUser);
                    findUserBaseWarehouseLocation(loggedInUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }



            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Home.this, error.toString(), Toast.LENGTH_LONG).show();


            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put("user_id", userId);
                headers.put("sid", sid);
                return headers;
            }

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);


    }

    public void logoutprocess()
    {
        //logout in the back end
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

       String myUrl = Utility.getInstance().buildUrl(Url.API_METHOD, null,Url.LOGOUT_URL);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, myUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        callLoginIntent();


                    }


                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Home.this, error.toString(), Toast.LENGTH_LONG).show();

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put("user_id", userId);
                headers.put("sid", sid);
                return headers;
            }

        };
    // Add the request to the RequestQueue.
        queue.add(stringRequest);


    }

 public void callLoginIntent(){
     Intent intent = new Intent(Home.this, Login.class);
     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); //flush the back stack
     startActivity(intent);
     finish();
 }
 @Override
 protected void autoLogout(){
     new Handler(Looper.getMainLooper()).post(new Runnable() {
         @Override
         public void run() {
             //do nothing
         }
     });
     destroyTimer();
     logout();

     if (dialogbox != null) {
         dialogbox.dismiss();
     }
 }

    private void findUserBaseWarehouseLocation(final String loggedUser) {


        //build th url appropriately
       // Map<String,String> param =buildParameters(loggedUser);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String , String > param = new HashMap<>();
        param.put("loggedUser",loggedUser);
        //build the url
        //Made this change in Url to reflect the Base WH Location docytype change in ERPNext
       // String url = Utility.getInstance().buildUrl(Url.API_RESOURCE,null,Url.USER_TABLE);
        String url = Utility.getInstance().buildUrl(Url.API_METHOD,param,Url.USER_WH_LOCATION);
        System.out.println("The built url is " + url);


       /* if (this.getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(this.getApplicationContext(), url, UserDetails.class, param, getHeaders(), new Response.Listener<UserDetails>() {

                @Override
                public void onResponse(UserDetails response) {

                    if (response != null) {

                        SingletonTemp.getInstance().setLoggedInUser(response);


                        UserDetails loggedUserDetails = SingletonTemp.getInstance().getLoggedInUser();
                        UserData loggedUserData = loggedUserDetails.getUserData();
                        String loggedInUserBaseWH = loggedUserData.getBase_warehouse_location(); //the current user is always at index 0 in the returned list
                        if(loggedInUserBaseWH!=null) {

                            setLoggedInUserBaseWHLoc(loggedInUserBaseWH);

                            loadFindWarehousefragment();
                        }
                        else
                        {
                           //do nothing
                        }




                    }


                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {


                    Toast.makeText(Home.this, "Server denied request with error" + error.toString(), Toast.LENGTH_LONG).show();

                }
            });
        }*/
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    String loggedInUserBaseWH  = object.getString("message");
                    if(loggedInUserBaseWH!=null){
                        if(loggedInUserBaseWH.contains(getResources().getString(R.string.error_string)))
                        {
                            showErrorDialogBox(loggedInUserBaseWH);
                        }
                        else {
                            setLoggedInUserBaseWHLoc(loggedInUserBaseWH);

                            loadFindWarehousefragment();
                        }

                    }
                    else{
                        showErrorDialogBox(getResources().getString(R.string.basewh_notfound_error));

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }



            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(Home.this, error.toString(), Toast.LENGTH_LONG).show();


            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put("user_id", userId);
                headers.put("sid", sid);
                return headers;
            }

        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);

    }
    private Map<String,String> buildParameters(String loggedUser) {


        String field1 ="name";
        String field2 ="full_name";
        String field3 = "base_warehouse_location";
        Map<String,String> params = new HashMap<>();

        JsonArray fieldsArray = new JsonArray();

        fieldsArray.add(field1);
        fieldsArray.add(field2);
        fieldsArray.add(field3);

        String tableName = "User";
        String fieldName = "name";
        String operand = "=";


        JsonArray oneFilter = new JsonArray();
        JsonArray filtersParam = new JsonArray();



        //add the conditions
        oneFilter.add(tableName);
        oneFilter.add(fieldName);
        oneFilter.add(operand);
        oneFilter.add(loggedUser);

        //parameter is actually a JsonArray of an array

        filtersParam.add(oneFilter);

        params.put("filters",filtersParam.toString());
        params.put("fields",fieldsArray.toString());

        //now print these parameters
        return params;



    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = this.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put("user_id", userId);
        headers.put("sid", sid);

        return headers;
    }


    public String getLoggedInUserBaseWHLoc() {
        return loggedInUserBaseWHLoc;
    }

    public void setLoggedInUserBaseWHLoc(String loggedInUserBaseWHLoc) {
        this.loggedInUserBaseWHLoc = loggedInUserBaseWHLoc;
    }

    //Start: Added on 19th Feb  2018 to implemt new interface
   public void onTaskSelected(View buttonClicked){

       taskSelected = buttonClicked.getId();
       startScanner();

    }

    private void startScanner() {
        IntentIntegrator mScanIntegrator = new IntentIntegrator(this);
        mScanIntegrator.initiateScan();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult.getContents() != null) {
            //we have a result
            String vehicelNameFromScanner = scanningResult.getContents();
            switch (taskSelected){
                case R.id.nav_receivefrom_re: //start the receive vehicle activity, send the scanned vehicle code
                    Intent startReceiveFromRE = new Intent(this, ReceiveVehicle.class);
                    //startReceiveFromRE.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startActivity(startReceiveFromRE);
                    break;
                case R.id.nav_allocate:
                    Intent startAllocateVehicle = new Intent(this, AllocateVehicles.class);
                    startAllocateVehicle.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startActivity(startAllocateVehicle);
                    break;
                case R.id.nav_load:
                    Intent startLoadVehicle = new Intent(this, LoadVehicles.class);
                    startLoadVehicle.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startLoadVehicle.putExtra("FromUnloadVehicles",false);
                    startActivity(startLoadVehicle);
                    break;
                case R.id.nav_receivefrom_wh:
                    Intent startUnLoadVehicle = new Intent(this, UnloadVehicle.class);
                    startUnLoadVehicle.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startActivity(startUnLoadVehicle);
                    break;
                case R.id.nav_deliver:
                    Intent startDeliverVehicle = new Intent(this, DeliverVehicle.class);
                    startDeliverVehicle.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startActivity(startDeliverVehicle);
                    break;
                case R.id.nav_getvehicleinfo:
                    Intent startVehicleDetails = new Intent(this, GetVehicleStatus.class);
                    startVehicleDetails.putExtra("VehicleScanned",vehicelNameFromScanner);
                    startActivity(startVehicleDetails);
                    break;

            }


        }
        else{
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //End: added on 19th Feb 2018

    private void showErrorDialogBox(String str) {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(str);
        this.showAlertDialog(null, null, false, getResources().getString(R.string.ok_str), null, dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                dialogbox.dismiss();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing


            }

        });

    }

}
