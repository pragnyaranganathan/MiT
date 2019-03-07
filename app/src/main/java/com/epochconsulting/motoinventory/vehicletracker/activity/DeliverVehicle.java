package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import android.os.Bundle;

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
import com.epochconsulting.motoinventory.vehicletracker.implementation.ServiceLocatorImpl;
import com.epochconsulting.motoinventory.vehicletracker.R;
import com.epochconsulting.motoinventory.vehicletracker.util.AlertDialogHandler;
import com.epochconsulting.motoinventory.vehicletracker.util.Constants;
import com.epochconsulting.motoinventory.vehicletracker.util.Url;
import com.epochconsulting.motoinventory.vehicletracker.util.Utility;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.SingletonTemp;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.VehicleDetails;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DeliverVehicle extends BasicActivity {
    private String vehicelNameFromScanner = null;
    ImageButton completeCycle;
    ImageButton barcodefooter;
    boolean compleCycleButtonClicked = false;
    boolean overrideStatus = false;
    private boolean activityTimedOut = false;
    boolean firstScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_deliver_vehicle);

        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);

        //Start: Added on 19th Feb 2018 to implement the new interface
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            firstScan = extras.getBoolean("FirstScan");
        }


        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //setting this variable to true so that submit stock doesnt call the sendmail method everytime
                compleCycleButtonClicked = true;


                submitStockEntry();


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
            delivervehicle();

        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //end added the lines of code on Jan 3rd 2018

    private void delivervehicle() {
        showProgress();
        String urlStr = Utility.getInstance().buildUrl(Url.API_RESOURCE, null, "Serial No", vehicelNameFromScanner);
        if (getApplicationContext() != null) {
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), urlStr, VehicleDetails.class, null, getHeaders(), new Response.Listener<VehicleDetails>() {

                @Override
                public void onResponse(VehicleDetails response) {

                    if (response != null) {


                        SingletonTemp.getInstance().setSerialNumberedVehicle(response);


                        String vehicleStatus = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getVehicle_status();
                        String deliveryDate = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getDelivery_required_on();
                        //Start: Added on 22nd Nov 2018
                        String vehicleBRN = SingletonTemp.getInstance().getSerialNumberedVehicle().getVehicleData().getBooking_reference_number();
                        //End: 22nd Nov 2018

                        if (deliveryDate == null) {
                            deliveryDate = getResources().getString(R.string.dummydate);//getTodaysDate(); //added onn 21st Dec 2017, to handle blank spaces as well on ERPNext
                        }
                        hideProgress();
                        //Start: 22nd Nov 2018 Added the extra parameter to allow of delivery of vehicles without a brn
                        checkVehicleStatusAndDeliveryDate(vehicleStatus, deliveryDate, vehicleBRN);
                        //End : 22nd Nov 2018


                    }


                }


            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    hideProgress();
                    showErrorDialogBox("Something went wrong while fetching vehicle details from ERPNext while trying to Deliver the vehicle. Make sure vehicle entry is present on ERPNext. Server error is  " + error.toString());

                }
            });
        }
    }

    private void checkVehicleStatusAndDeliveryDate(String vehicleStatus, String deliveryDate, String vehicleBRN) {
        if (vehicleStatus != null && deliveryDate != null) {
            if (vehicleStatus.equalsIgnoreCase("Allocated but not Delivered") && deliveryDate.equals(getTodaysDate()) && vehicleBRN!=null ) {

                playBeepSound("beeppositive.mp3");
                showDeliverVehicleAlert();
            } else {

                playBeepSound("beepnegative.mp3");
                String error = "";
                if (!vehicleStatus.equalsIgnoreCase("Allocated but not Delivered")) {
                    error = getResources().getString(R.string.donotdelivervehicle_statusnotABND_str);
                }
                if (!deliveryDate.equals(getTodaysDate())) {
                    error = error+ getResources().getString(R.string.donotdelivervehicle_str);
                }
                if(vehicleBRN == null)
                {
                    error = error+getResources().getString(R.string.brn_missing_for_vehicle_str);
                }

                showDoNotDeliverVehicleAlert(error+getResources().getString(R.string.do_you_want_to_deliver_anyway_str));
            }
        }
        if (deliveryDate == null || vehicleStatus == null) {
            showErrorDialogBox("Some fields are Null in the Serial No document. Please ensure they are not null on ERPNext and try again.");
        }

    }

    private void showDeliverVehicleAlert() {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(getResources().getString(R.string.deliver_vehicle_str));
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                createDeliverNote(vehicelNameFromScanner);


            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();


            }

        });



    }

    private void createDeliverNote(final String serialNo) {
        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        Map<String, String> map = new HashMap<>();
        map.put("serial_no", serialNo);
        //Start: Added on 22nd Nov 2018 to accommodate a dummy customer sales invoice for vehicles without a brn
        map.put("source_warehouse",getCurrent_warehouse());
        //End: Change on 22nd Nov 2018


        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.MAKE_SALES_INVOICE);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String serverResponse = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    serverResponse = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                hideProgress();
                if(serverResponse.contains(getResources().getString(R.string.success_string)))
                {
                    String usermsg = serverResponse+getResources().getString(R.string.vehicle_delivered_success_str);
                    toastMaker(usermsg);
                }
                else
                {
                    showErrorDialogBox(serverResponse+". Vehicle could not be delivered.");
                }



            }
        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox("Could not create a sales invoice on ERPNext for this vehicle. Vehicle could not be delivered. Server Error: " + error.toString());



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

    private void showDoNotDeliverVehicleAlert(String errorString) {

        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(errorString);
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                createDeliverNote(vehicelNameFromScanner);

            }

            @Override
            public void onNegativeButtonClicked() {

                //will be implemented
                dialogbox.dismiss();
            }
        });


    }

    private void submitStockEntry() {

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String, String> map = new HashMap<>();
        map.put("serial_no", vehicelNameFromScanner);

        String urlStr = Utility.getInstance().buildUrl(Url.API_METHOD, map, Url.SUBMIT_SALES_INVOICE);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String returnmsg = "";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg = jsonObject.getString("message");

                } catch (Exception e) {
                    //do nothing
                }
                String usermsg= "";
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    usermsg = ". Vehicle was delivered!";
                }
                else
                {
                    usermsg = ". Vehicle could not be delivered!";
                }

                toastMaker(returnmsg+usermsg);



            }
        }//end of sucess responseP
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {


                toastMaker("The sales invoice could not be submitted on ERPNext. Vehicle could not be delivered. Server Error: "+error.toString());


            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
        if (activityTimedOut) {
            activityTimedOut = false;
            logout();
            //resetting this value

        }
        if (compleCycleButtonClicked) {
            compleCycleButtonClicked = false; //resetting this once this is called
            callHomePage();
            finish();

        }

    }


    @Override
    public void autoLogout() {

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

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put("user_id", userId);
        headers.put("sid", sid);

        return headers;
    }

    private String getTodaysDate() {

        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String today = (formattedDate.format(c.getTime()));

        return today;
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

}
