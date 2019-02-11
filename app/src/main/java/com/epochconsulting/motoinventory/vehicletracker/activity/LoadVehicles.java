package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
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
import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.util.AlertDialogHandler;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;

import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;

import com.epochconsulting.motoinventory.vehicletracker.util.pojo.VehicleDetails;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class LoadVehicles extends BasicActivity {


    String vehicelNameFromScanner;
    String mcurrentWarehouse;

    boolean overrideStatus = false;

    boolean activityTimedOut = false;
    boolean compleCycleButtonClicked = false;


    ImageButton completeCycle;
    ImageButton barcodefooter;
    boolean fromUnloadVehiclesPage;
    boolean firstScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_vehicles);

        mcurrentWarehouse = getCurrent_warehouse();
        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);

        //Start: Added on 19th Feb 2018 to implement new interface
        /*vehicelNameFromScanner = null;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            fromUnloadVehiclesPage = extras.getBoolean("FromUnloadVehicles");
            if(!fromUnloadVehiclesPage) {
                vehicelNameFromScanner = extras.getString("VehicleScanned");
                if(vehicelNameFromScanner!=null)
                {
                    loadVehicle();
                }
                else
                {
                    showErrorDialogBox("Vehicle was not scanned properly. Please scan the vehicle again!");
                }
            }
            else {
                initializeScanner(); //go directly to the initialize scanner page
            }
        }*/
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            firstScan = extras.getBoolean("FirstScan");
        }

        //End : Added on 19th Feb 2018


        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //setting this variable to true so that submit stock doesnt call the sendmail method everytime
                compleCycleButtonClicked = true;
                submitStockEntry(); //first submit the stock entry


            }
        });

        barcodefooter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


               if(!firstScan)
                    submitStockEntry();
                initializeScanner();

            }
        });
        barcodefooter.performClick();

    }

    @Override
    public void onBackPressed() {
        //do nothing here
    }

    //start - added these lines of code on Jan 3rd 2018
    private void initializeScanner() {
        firstScan = false;
        IntentIntegrator mScanIntegrator = new IntentIntegrator(this);
        mScanIntegrator.initiateScan();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (scanningResult.getContents() != null) {
            //we have a result
            vehicelNameFromScanner = scanningResult.getContents();
            loadVehicle();

        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //end added the lines of code on Jan 3rd 2018


    private void loadVehicle() {


        mcurrentWarehouse = getCurrent_warehouse();
        if (vehicelNameFromScanner != null && mcurrentWarehouse != null) {
            fetchVehicleDetailsFromBackend();
        } else {
            //throw an error dialog box saying that the current warehouse is null or vehiclenamefromscanner is null
            playBeepSound("beepnegative.mp3");
            handleErrors();

        }


    }


    private void handleErrors() {
        if (mcurrentWarehouse == null) {
            showErrorDialogBox("The current warehouse is not specified. Please click on OK to dismiss the Alert. Finish the task to retry.");
        }
        if (vehicelNameFromScanner == null) {
            showErrorDialogBox("The scanned vehicle code is not specified. Please click on OK to dismiss this Alert. Scan another vehicle to continue.");
        }

    }

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

    private void doLoadTask(String requiredatWH, long daysDiff) {

        if (requiredatWH != null) {

            if (!mcurrentWarehouse.equalsIgnoreCase(requiredatWH) && daysDiff < Constants.NUMBER_OF_DAYS_TO_TRANSFER_VEHICLE && daysDiff >= 0) {

                playBeepSound("beeppositive.mp3");
                showLoadVehicleAlert();


            } else {


                String errormsg = "";
                if (mcurrentWarehouse.equalsIgnoreCase(requiredatWH)) {
                    errormsg = errormsg + getResources().getString(R.string.donotloadvehicle_str);

                } else {
                    errormsg = errormsg + getResources().getString(R.string.donotloadvehicletoday_str);

                }
                playBeepSound("beepnegative.mp3");
                showDoNotLoadVehicleAlert(errormsg);

            }
        } else {
            playBeepSound("beepnegative.mp3");
            showErrorDialogBox("The delivery_required_at field is not specified on ERPNext for vehicle with serial number " + vehicelNameFromScanner + ". Click on OK to dismiss this alert. Scan another vehicle to continue or enter details on ERPNext and retry.");
        }
    }

    private void fetchVehicleDetailsFromBackend() {

        showProgress();

        String urlStr = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, "Serial No", vehicelNameFromScanner);
        if (getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), urlStr, VehicleDetails.class, null, getHeaders(), new Response.Listener<VehicleDetails>() {

                @Override
                public void onResponse(VehicleDetails response) {

                    hideProgress();

                    if (response != null) {


                        SingletonTemp.getInstance().setSerialNumberedVehicle(response);

                        String requiredAtWH = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_at();
                        String requiredOnDate = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_on();
                        String vehicleStatus = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getVehicle_status();
                        long daysDiff;
                        if (vehicleStatus == null) {
                            playBeepSound("beepnegative.mp3");
                            showErrorDialogBox("The vehicle status field is not specified on ERPNext. Please specify the value on ERPNext and retry loading the vehicle");
                        } else {


                            if (requiredAtWH == null) {
                                requiredAtWH = getResources().getString(R.string.dummywarehouse);
                            }
                            if (requiredOnDate == null) {
                                daysDiff = Constants.DUMMY_NUMBER_OF_DAYS_DIFF;
                            } else {
                                daysDiff = findDifferenceInNumberOfDays(requiredOnDate);
                            }

                            if (!(vehicleStatus.equalsIgnoreCase(getResources().getString(R.string.delivery_status_of_vehicle)))) {
                                doLoadTask(requiredAtWH, daysDiff);
                            } else {

                                playBeepSound("beepnegative.mp3");
                                showErrorDialogBox("The vehicle with serial no " + vehicelNameFromScanner + "  is already Delivered . Click on OK to dismiss this alert. Scan another vehicle to continue or click on Complete Task to go back to Home page.");

                            }
                        }

                    }

                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox("Server Error: "+error.toString());
                }
            });
        }
    }

    private long findDifferenceInNumberOfDays(String requiredOnDate) {
        String pattern = "yyyy-MM-dd";
        long daysDiff = -1;
        Date date = null;
        try {
            date = new SimpleDateFormat(pattern).parse(requiredOnDate);
            Calendar testCalendar = Calendar.getInstance();
            testCalendar.setTime(date);

            long msDiff = testCalendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

            daysDiff = TimeUnit.MILLISECONDS.toDays(msDiff);
        } catch (ParseException e) {
            //log the error
        }


        return daysDiff;
    }


    private void showLoadVehicleAlert() {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(getResources().getString(R.string.loadvehicle_str));
        LoadVehicles.this.showAlertDialog(null,null , true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                createStockEntry(vehicelNameFromScanner, mcurrentWarehouse);


            }

            @Override
            public void onNegativeButtonClicked() {

                dialogbox.dismiss();


            }

        });


    }

    private void showDoNotLoadVehicleAlert(String errormsg) {

        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(errormsg);
        //End: Added this on 20th Feb 2018
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                //I should add this serial no in the stock entry table
                //setting the flag to vehiclefound to true again, since I'm overriding

                createStockEntry(vehicelNameFromScanner, mcurrentWarehouse);

            }

            @Override
            public void onNegativeButtonClicked() {

                //will be implemented
                dialogbox.dismiss();
            }
        });


    }

    private void createStockEntry(String serialNo, String currentWarehouse) {
        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put("serial_no", serialNo);
        map.put("source_warehouse", currentWarehouse);
        //added on 3 Nov 2017, to allow ny truck warehouse
        String truckWH = getTruckWH();
        map.put("target_warehouse", truckWH);

        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_MOVEMENT_STOCK_ENTRY);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                hideProgress();

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");

                } catch (Exception e) {
                    Log.d("ERROR", e.toString());
                }
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    toastMaker(returnmsg+". Vehicle is moved on truck!");
                }
                else
                {
                    showErrorDialogBox(returnmsg+". Vehicle could not be moved on truck.");
                }



            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgress();
                showErrorDialogBox("Vehicle could not be moved on truck. Server Error: "+error.toString());

            }
        })
                //end of error response and stringRequest params
        {
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

    @Override
    protected void autoLogout() {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //do nothing
            }
        });
        destroyTimer();
        activityTimedOut = true;
        submitStockEntry();

        if (dialogbox != null) {
            dialogbox.dismiss();
        }

    }

    private void submitStockEntry() {


        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String, String> map = new HashMap<>();
        map.put("serial_no", vehicelNameFromScanner);
        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.SUBMIT_STOCK_ENTRY);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");
                } catch (Exception e) {
                    Log.d("ERROR", e.toString());

                }


                toastMaker(returnmsg);


            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                toastMaker(getResources().getString(R.string.couldnotsubmitstockentry) + " " + vehicelNameFromScanner+". Vehicle could not be moved/loaded on truck!");


            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
        if (activityTimedOut) {
            activityTimedOut = false;
            logout();


        }
        if (compleCycleButtonClicked) {
            compleCycleButtonClicked = false; //resetting this once this is called
            callHomePage();
            finish();

        }


    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put("user_id", userId);
        headers.put("sid", sid);

        return headers;
    }


}
