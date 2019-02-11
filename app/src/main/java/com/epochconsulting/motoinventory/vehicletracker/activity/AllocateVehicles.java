package com.epochconsulting.motoinventory.vehicletracker.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.AllocateVehicleControlData;
import com.epochconsulting.motoinventory.vehicletracker.util.pojo.Message;
import com.epochconsulting.motoinventory.vehicletracker.view.common.BasicActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AllocateVehicles extends BasicActivity {

    private String vehicelNameFromScanner = null;
    ImageButton completeCycle;
    ImageButton barcodefooter;
    EditText bookingReferenceNo;
    Button findBRN;
    String brn;
    boolean overrideStatus = false;
    static final String USERID = "user_id";
    static final String SERIALNO = "serial_no";
    boolean firstScan ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocate_vehicles);

        bookingReferenceNo = (EditText) findViewById(R.id.brn);
        findBRN = (Button) findViewById(R.id.getbrninfo) ;
        completeCycle = (ImageButton) findViewById(R.id.completecycle_footer);
        barcodefooter = (ImageButton) findViewById(R.id.barcode_footer);

        //Start : Added on 19th Feb 2018 to implement new interface
        firstScan = false;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
        {
            firstScan= extras.getBoolean("FirstScan");
        }




        completeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               callHomePage();
                finish();

            }
        });

        barcodefooter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //restarting the scanner


                enableTheAllocateVehicleButton();
                initializeScanner();

            }
        });

        findBRN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                brn = bookingReferenceNo.getText().toString();
                canallocateVehicle();


            }
        });
        barcodefooter.performClick();


    }
    @Override
    public void onBackPressed(){
        //do nothing here
    }
    //start - added these lines of code on Jan 3rd 2018
    private void initializeScanner() {
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

        }
        else{
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    //end added the lines of code on Jan 3rd 2018

    private void canallocateVehicle() {
        showProgress();

        Map<String,String> map = new HashMap<>();
        map.put(SERIALNO,vehicelNameFromScanner);
        map.put("brn",brn);



        //Start:Added on 17 Dec 2018
        //String url = Utility.getInstance().buildUrl(Url.API_METHOD,map,Url.ALLOCATE_VEHICLE_NEW);
        String url = Utility.getInstance().buildUrl(Url.API_METHOD,null,Url.ALLOCATE_VEHICLE_NEW);
        if(this.getApplicationContext()!=null){
            showProgress();
            ServiceLocatorImpl.getInstance().executeGetVolleyRequest(getApplicationContext(), url, AllocateVehicleControlData.class, map, getHeaders(), new Response.Listener<AllocateVehicleControlData>() {
                @Override
                public void onResponse(AllocateVehicleControlData response) {
                    hideProgress();
                    if(response!=null)
                    {
                        Message serverMsg = response.getMessage();
                        if(serverMsg!=null){

                            switch (serverMsg.getLevel()){
                                case 2: //hight ontrol
                                    changeStatusControlHigh(serverMsg.getRetval());
                                    break;
                                case 1 : //medium control
                                    changeStatusControlMedium(serverMsg.getRetval());
                                    break;
                                case 0: //low control
                                    changeStatusControlLow(serverMsg.getRetval());
                                    break;
                                default:
                                    showErrorDialogBox("Server Error: "+getResources().getString(R.string.serverresponse_err));
                                    break;
                            }



                        }
                        else{
                            showErrorDialogBox("Server Error: "+getResources().getString(R.string.serverresponse_err));
                        }

                    }
                    else
                    {
                        showErrorDialogBox("Server Error: "+getResources().getString(R.string.serverresponse_err));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgress();
                    showErrorDialogBox("Server error: "+error.toString());
                }
            });

        }



    }

    private void changeStatusControlLow(Integer retval) {
        switch (retval){
            case 3:
                playBeepSound("beeppositive.mp3");
                showAlertDialogForAllocatingVehicleLowMedium(getResources().getString(R.string.vehicle_tobe_allocated_low_control));
                disableTheAllocateVehicleButton();
                break;
            case -7:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This vehicle with the serial number "+ vehicelNameFromScanner+ " has already been delivered. Cannot allocate this vehicle! Please scan another vehicle or click on finish task button to go back to Home.");
                disableTheAllocateVehicleButton();
                break;
            case -8:
                //vehicle status is IBNR
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleLowMedium("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Invoiced but not Received on ERPNext and the control level is low. Not allocating this vehicle to the dummy customer . Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -9:
                //vehicle status is ABND
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleLowMedium("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Allocated but Not Delivered on ERPNext but does not have a BRN. The control level is low. Not allocating this vehicle to the dummy customer. Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case 0:
            default:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("Something went wrong while fetching the allocation details for this vehicle from ERPNext. Try scanning again.");
                disableTheAllocateVehicleButton();
                break;

        }
    }



    private void changeStatusControlMedium(Integer retVal) {
        //todo : add code to handle medium control level
        switch (retVal){
            case 1:
                playBeepSound("beeppositive.mp3");
                showAlertDialogForAllocatingVehicleHigh();
                disableTheAllocateVehicleButton();
                break;
            case 2:
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("A vehicle has already been allocated to the booking reference number "+brn+". Do you want to allocate vehicle "+vehicelNameFromScanner+" to "+ brn+ " ? Click Yes to allocate and No to try with another booking reference number.");
                bookingReferenceNo.setText("");
                break;
            case 3:
                playBeepSound("beeppositive.mp3");
                showAlertDialogForAllocatingVehicleLowMedium(getString(R.string.allocate_vehicle_medium_control));
                bookingReferenceNo.setText("");
                break;
            case -1:
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleLowMedium("A valid Sales Order with Booking Reference Number "+brn+" does not exist on ERPNext. Since the control level is set to medium, do you want to allocate this to a Dummy Customer?");
                bookingReferenceNo.setText("");
                break;
            case -2:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This vehicle with the serial number "+ vehicelNameFromScanner+ " has already been delivered. Cannot allocate this vehicle! Please scan another vehicle or click on finish task button to go back to Home.");
                disableTheAllocateVehicleButton();
                break;
            case -3:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("Item code of vehicle with Serial No "+vehicelNameFromScanner+" does not match the item code associated with booking reference number "+brn+". Not allocating this vehicle to the booking reference number "+brn+".");

                disableTheAllocateVehicleButton();
                break;
            case -4:
                //vehicle status is IBNR
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Invoiced but not Received on ERPNext. Not allocating this vehicle to the booking reference number  "+brn+". Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -5:
                //vehicle status is ABND
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Allocated but Not Delivered on ERPNext but does not have a BRN. Not allocating this vehicle to the booking reference number  "+brn+". Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -6:
                //vehicle status is Delivered
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This BRN has been used for a vehicle that is already delivered. Cannot use "+brn+" for this vehicle!. Please retry with another booking reference number.");
                bookingReferenceNo.setText("");
                break;
            case -7:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This vehicle is already delivered. Cannot allocate this vehicle to a dummy customer again!");
                bookingReferenceNo.setText("");
                break;
            case -8:
                //vehicle status is IBNR
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleLowMedium("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Invoiced but not Received on ERPNext. The control level is medium. Not allocating this vehicle to the dummy customer. Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -9:
                //vehicle status is ABND
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleLowMedium("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Allocated but Not Delivered on ERPNext but does not have a BRN. The control level is medium. Not allocating this vehicle to the booking reference number  "+brn+". Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case 0:
            default:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("Something went wrong while fetching the allocation details for this vehicle from ERPNext. Try scanning again.");
                disableTheAllocateVehicleButton();
                break;

        }
    }


    private void changeStatusControlHigh(Integer retVal) {
        switch (retVal){
            case 1:
                playBeepSound("beeppositive.mp3");
                showAlertDialogForAllocatingVehicleHigh();
                disableTheAllocateVehicleButton();
                break;
            case 2:
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("A vehicle has already been allocated to the booking reference number "+brn+". Do you want to allocate vehicle "+vehicelNameFromScanner+" to "+ brn+ " ? Click Yes to allocate and No to try with another booking reference number.");
                bookingReferenceNo.setText("");
                break;
            case -1:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("A valid Sales Order with Booking Reference Number "+brn+" does not exist on ERPNext. Please re-enter the correct number and try again.");
                bookingReferenceNo.setText("");
                break;
            case -2:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This vehicle with the serial number "+ vehicelNameFromScanner+ " has already been delivered. Cannot allocate this vehicle! Please scan another vehicle or click on finish task button to go back to Home.");
                disableTheAllocateVehicleButton();
                break;
            case -3:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("Item code of vehicle with Serial No "+vehicelNameFromScanner+" does not match the item code associated with booking reference number "+brn+". Not allocating this vehicle to the booking reference number "+brn+".");

                disableTheAllocateVehicleButton();
                break;
            case -4:
                //vehicle status is IBNR
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Invoiced but not Received on ERPNext. Not allocating this vehicle to the booking reference number  "+brn+". Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -5:
                //vehicle status is ABND
                playBeepSound("beepnegative.mp3");
                showAlertDialogForNotAllocatingVehicleHigh("Status of vehicle with Serial No "+vehicelNameFromScanner+" is Allocated but Not Delivered on ERPNext but does not have a BRN. Not allocating this vehicle to the booking reference number  "+brn+". Do you want to allocate anyway? Click Yes to allocate and No to dismiss.");
                disableTheAllocateVehicleButton();
                break;
            case -6:
                //vehicle status is Delivered
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("This BRN has been used for a vehicle that is already delivered. Cannot use "+brn+" for this vehicle!. Please retry with another booking reference number.");
                bookingReferenceNo.setText("");
                break;
            case 0:
            default:
                playBeepSound("beepnegative.mp3");
                showErrorDialogBox("Something went wrong while fetching the allocation details for this vehicle from ERPNext. Try scanning again.");
                disableTheAllocateVehicleButton();
                break;
        }
    }
    //End: added on 18th Dec 2018

    //Start: Added on 17th Dec 2018
    public Map<String, String> getHeaders()  {
        Map<String, String> headers = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.USER_ID, null);
        String sid = prefs.getString(Constants.SESSION_ID, null);
        headers.put(USERID, userId);
        headers.put("sid", sid);
        return headers;
    }
    //End: Added on 17th Dec 2018



    private void disableTheAllocateVehicleButton() {

        bookingReferenceNo.setText("");
        bookingReferenceNo.setEnabled(false);
        findBRN.setEnabled(false);
        findBRN.setTextColor(getResources().getColor(R.color.cardview_dark_background));
        findBRN.setBackgroundColor(getResources().getColor(R.color.cardview_shadow_start_color));
    }
    private void enableTheAllocateVehicleButton(){
        bookingReferenceNo.setText("");
        bookingReferenceNo.setEnabled(true);
        findBRN.setEnabled(true);
        findBRN.setTextColor(getResources().getColor(R.color.cardview_light_background));
        findBRN.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));



    }

    private void showAlertDialogForAllocatingVehicleLowMedium(String s) {
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(s +" "+brn+". Click Yes to allocate and  No to cancel.");
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView,new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                allocateVehicleLowMedium();


            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();


            }

        });
    }
    private void showAlertDialogForNotAllocatingVehicleLowMedium(String s) {
        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        final View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(s);
        //End: Added this on 20th Feb 2018
        showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                //make method call to change the status and allocate the vehicle
                allocateVehicleLowMedium();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();
            }
        });
    }

    private void allocateVehicleLowMedium() {
        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());


        Map<String,String> map = new HashMap<>();

        String currentWarehouse = getCurrent_warehouse();
        map.put("current_warehouse",currentWarehouse);
        map.put("serial_no",vehicelNameFromScanner);
        String url = Utility.getInstance().buildUrl(Url.API_METHOD,map,Url.ALLOCATE_VEHICLE_LOWMEDIUM);
        System.out.println("The url for change status in low medium is "+url);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideProgress();
                String returnmsg = "";

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg  = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    String usermsg = returnmsg+getResources().getString(R.string.vehicle_allocated_success_str);
                    toastMaker(usermsg);
                }
                else
                {
                    showErrorDialogBox(returnmsg);
                }

                disableTheAllocateVehicleButton();

            }


        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox("Could not allocate vehicle to booking reference number: "+brn+". Server Error: "+error.toString());

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put(USERID, userId);
                headers.put("sid", sid);
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);

    }


    private void showAlertDialogForAllocatingVehicleHigh() {

        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        View dialogView = dialogViewinflator.inflate(R.layout.dialog_success_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_success_title) ;
        title.setText(getResources().getString(R.string.success_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_success_message) ;
        message.setText(getResources().getString(R.string.vehicle_tobe_allocated)+" "+brn+". Click Yes to allocate and  No to cancel.");
        this.showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView,new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {


                allocateVehicle();


            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();


            }

        });


    }

    private void showAlertDialogForNotAllocatingVehicleHigh(String s) {

        //Start: Added this on 20th Feb 2018 to implement new interface
        LayoutInflater dialogViewinflator = this.getLayoutInflater();
        final View dialogView = dialogViewinflator.inflate(R.layout.dialog_error_content,null);
        TextView title = (TextView) dialogView.findViewById(R.id.dialog_error_title) ;
        title.setText(getResources().getString(R.string.error_string));
        TextView message = (TextView) dialogView.findViewById(R.id.dialog_error_message) ;
        message.setText(s);
        //End: Added this on 20th Feb 2018
        showAlertDialog(null, null, true, getResources().getString(R.string.yes_str), getResources().getString(R.string.no_str), dialogView, new AlertDialogHandler() {
            @Override
            public void onPositiveButtonClicked() {

                //make method call to change the status and allocate the vehicle
                allocateVehicle();

            }

            @Override
            public void onNegativeButtonClicked() {
                //do nothing
                dialogbox.dismiss();
            }
        });

    }

    private void allocateVehicle() {

        showProgress();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());


        Map<String,String> map = new HashMap<>();
        map.put(SERIALNO,vehicelNameFromScanner);
        map.put("brn",brn);
        String url = Utility.getInstance().buildUrl(Url.API_METHOD,map,Url.ALLOCATE_VEHICLE);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                hideProgress();
                String returnmsg = "";

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    returnmsg  = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(returnmsg.contains(getResources().getString(R.string.success_string)))
                {
                    String usermsg = returnmsg+getResources().getString(R.string.vehicle_allocated_success_str);
                    toastMaker(usermsg);
                }
                else
                {
                    showErrorDialogBox(returnmsg);
                }

                disableTheAllocateVehicleButton();

            }


        }//end of sucess response
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                hideProgress();
                showErrorDialogBox("Could not allocate vehicle to booking reference number: "+brn+". Server Error: "+error.toString());




            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
                String userId = prefs.getString(Constants.USER_ID, null);
                String sid = prefs.getString(Constants.SESSION_ID, null);
                headers.put(USERID, userId);
                headers.put("sid", sid);
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        requestQueue.add(stringRequest);
    }



    @Override
    protected void autoLogout(){

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //I am not doing anything..just killing time
            }
        });
        destroyTimer();
        logout();
        // hideprogress method to be called here
        if (dialogbox != null) {
            dialogbox.dismiss();
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




}